package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.IndexOptions;
import org.javenstudio.common.indexdb.util.IOUtils;

/**
 * Holds state for inverting all occurrences of a single
 * field in the document.  This class doesn't do anything
 * itself; instead, it forwards the tokens produced by
 * analysis to its own consumer
 * (InvertedDocConsumerPerField).  It also interacts with an
 * endConsumer (DocEndConsumerPerField).
 */
final class DocumentInverterPerField extends DocFieldConsumerPerField {

	private final FieldState mFieldState;
	
	public DocumentInverterPerField(DocumentInverter parent, IFieldInfo fieldInfo) { 
		super(parent, fieldInfo);
		mFieldState = new FieldState(fieldInfo.getName());
	}
	
	final FieldState getFieldState() { return mFieldState; }
	
	@Override
	public void processFields(IField[] fields, int count) throws IOException { 
		mFieldState.reset();
		
		final boolean doInvert = getBeginConsumer().start(fields, count);
		final IAnalyzer analyzer = getDocState().getAnalyzer();
		
	    for (int i=0; i < count; i++) {
	        final IField field = fields[i];
	        final IField.Type fieldType = field.getFieldType();
		
	        // TODO FI: this should be "genericized" to querying
	        // consumer if it wants to see this particular field tokenized.
	        if (fieldType.isIndexed() && doInvert) 
	        	invertField(field, fieldType, analyzer, (i == 0));
	        
	        // LUCENE-2387: don't hang onto the field, so GC can reclaim
	        fields[i] = null;
	    }
	    
	    getBeginConsumer().finish();
	    getEndConsumer().finish();
	}
	
	private void invertField(final IField field, final IField.Type fieldType, 
			final IAnalyzer analyzer, boolean isFirst) throws IOException { 
		// if the field omits norms, the boost cannot be indexed.
        if (fieldType.isOmitNorms() && field.getBoost() != 1.0f) {
        	throw new UnsupportedOperationException("You cannot set an index-time boost:" + 
        			" norms are omitted for field '" + field.getName() + "'");
        }
        
        // only bother checking offsets if something will consume them.
        // TODO: after we fix analyzers, also check if termVectorOffsets will be indexed.
        final boolean checkOffsets = (fieldType.getIndexOptions() == 
        		IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        int lastStartOffset = 0;
    	
        if (!isFirst) {
        	mFieldState.increasePosition(analyzer == null ? 0 : 
        		analyzer.getPositionIncrementGap(getFieldInfo().getName()));
        }
        
        final ITokenStream stream = field.tokenStream(analyzer);
        // reset the TokenStream to the first token
        stream.reset();

        boolean success2 = false;
        try {
        	getBeginConsumer().start(field);
        	
        	for (;;) {
                // If we hit an exception in stream.next below
                // (which is fairly common, eg if analyzer
                // chokes on a given document), then it's
                // non-aborting and (above) this one document
                // will be marked as deleted, but still
                // consume a docID
        		IToken token = stream.nextToken();
        		if (token == null) 
        			break;
        		
        		final int posIncr = token.getPositionIncrement();
        		if (posIncr < 0) {
                    throw new IllegalArgumentException("position increment must be >=0 (got " 
                    		+ posIncr + ")");
        		}
        		
        		if (mFieldState.getPosition() == 0 && posIncr == 0) 
                    throw new IllegalArgumentException("first position increment must be > 0 (got 0)");
                
        		int position = mFieldState.getPosition() + posIncr;
        		if (position > 0) {
                    // NOTE: confusing: this "mirrors" the
                    // position++ we do below
                    position --;
        		} else if (position < 0) {
                    throw new IllegalArgumentException("position overflow for field '" 
                    		+ field.getName() + "'");
        		}
        		
                // position is legal, we can safely place it in fieldState now.
                // not sure if anything will use fieldState after non-aborting exc...
                mFieldState.setPosition(position);

                if (posIncr == 0)
                	mFieldState.increaseNumOverlap(1);
        		
                if (checkOffsets) {
                    int startOffset = mFieldState.getOffset() + token.getStartOffset();
                    int endOffset = mFieldState.getOffset() + token.getEndOffset();
                    if (startOffset < 0 || endOffset < startOffset) {
                    	throw new IllegalArgumentException(
                    			"startOffset must be non-negative, and endOffset must be >= startOffset, "
                    			+ "startOffset=" + startOffset + ",endOffset=" + endOffset);
                    }
                    if (startOffset < lastStartOffset) {
                    	throw new IllegalArgumentException("offsets must not go backwards startOffset=" 
                    			+ startOffset + " is < lastStartOffset=" + lastStartOffset);
                    }
                    lastStartOffset = startOffset;
                }
                
                boolean success = false;
                try {
                	// If we hit an exception in here, we abort
                	// all buffered documents since the last
                	// flush, on the likelihood that the
                	// internal state of the consumer is now
                	// corrupt and should not be flushed to a
                	// new segment:
                	getBeginConsumer().add(token);
                	success = true;
                } finally {
                	if (!success) 
                		getSegmentWriter().setAborting();
                }
                
                mFieldState.increaseLength(1);
                mFieldState.increasePosition(1);
        	}
        	
            // trigger streams to perform end-of-stream operations
            int finalOffset = stream.end();

            mFieldState.increaseOffset(finalOffset);
            success2 = true;
        	
        } finally { 
            if (!success2) 
                IOUtils.closeWhileHandlingException(stream);
            else 
                stream.close();
        }
        
        mFieldState.increaseOffset(analyzer == null ? 0 : analyzer.getOffsetGap(field.getName()));
        mFieldState.setBoost(mFieldState.getBoost() * field.getBoost());
	}
	
}
