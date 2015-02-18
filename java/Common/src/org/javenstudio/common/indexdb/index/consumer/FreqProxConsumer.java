package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.IFieldsConsumer;
import org.javenstudio.common.indexdb.codec.IPostingsFormat;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;
import org.javenstudio.common.indexdb.util.CollectionUtil;
import org.javenstudio.common.indexdb.util.IOUtils;

public final class FreqProxConsumer extends TermsHashConsumer {

	private final IPostingsFormat mPostingsFormat;
	
	public FreqProxConsumer(DocumentWriter writer) { 
		super(writer);
		
		mPostingsFormat = getDocumentWriter().getSegmentWriter().getIndexWriter()
				.getIndexFormat().getPostingsFormat();
	}
	
	protected final IPostingsFormat getPostingsFormat() { 
		return mPostingsFormat;
	}
	
	@Override
	public TermsHashConsumerPerField addField(TermsHashPerField termsHashPerField, IFieldInfo fieldInfo) { 
		return new FreqProxConsumerPerField(termsHashPerField, this, fieldInfo);
	}
	
	// TODO: would be nice to factor out more of this, eg the
	// FreqProxFieldMergeState, and code to visit all Fields
	// under the same FieldInfo together, up into TermsHash*.
	// Other writers would presumably share alot of this...
	
	@Override
	public void flush(TermsHashConsumerPerField.List fields, ISegmentWriteState state) 
			throws IOException { 
	    // Gather all FieldData's that have postings, across all
	    // ThreadStates
	    List<FreqProxConsumerPerField> allFields = new ArrayList<FreqProxConsumerPerField>();

	    for (TermsHashConsumerPerField field : fields.values()) {
	        final FreqProxConsumerPerField perField = (FreqProxConsumerPerField) field;
	        if (perField.getTermsHashPerField().getBytesHash().size() > 0) 
	        	allFields.add(perField);
	    }

	    final int numAllFields = allFields.size();

	    // Sort by field name
	    CollectionUtil.quickSort(allFields);
	    
	    final IFieldsConsumer consumer = getPostingsFormat().getFieldsConsumer(
	    		getDocumentWriter().getDirectory(), state);
	    
	    boolean success = false;
	    try {
	    	TermsHash termsHash = null;
	      
	    	/*
			    Current writer chain:
			      FieldsConsumer
			        -> IMPL: FormatPostingsTermsDictWriter
			          -> TermsConsumer
			            -> IMPL: FormatPostingsTermsDictWriter.TermsWriter
			              -> DocsConsumer
			                -> IMPL: FormatPostingsDocsWriter
			                  -> PositionsConsumer
			                    -> IMPL: FormatPostingsPositionsWriter
	    	 */
	      
	    	for (int fieldNumber = 0; fieldNumber < numAllFields; fieldNumber++) {
	    		final IFieldInfo fieldInfo = allFields.get(fieldNumber).getFieldInfo();
	    		final FreqProxConsumerPerField fieldWriter = allFields.get(fieldNumber);

	    		// If this field has postings then add them to the
	    		// segment
	    		fieldWriter.flush(fieldInfo.getName(), consumer, state);
	        
	    		TermsHashPerField perField = fieldWriter.getTermsHashPerField();
	    		assert termsHash == null || termsHash == perField.getTermsHash();
	    		termsHash = perField.getTermsHash();
	    		int numPostings = perField.getBytesHash().size();
	    		perField.reset();
	    		perField.shrinkHash(numPostings);
	    		fieldWriter.reset();
	    	}
	      
	    	if (termsHash != null) 
	    		termsHash.reset();
	    		
	    	success = true;
	    } finally {
	    	if (success) 
	    		IOUtils.close(consumer);
	    	else 
	    		IOUtils.closeWhileHandlingException(consumer);
	    }
	}
	
}
