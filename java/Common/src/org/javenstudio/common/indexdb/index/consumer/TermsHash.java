package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.codec.ISegmentWriteState;
import org.javenstudio.common.indexdb.index.DocumentWriter;

/** 
 * This class implements {@link InvertedDocConsumer}, which
 *  is passed each token produced by the analyzer on each
 *  field.  It stores these tokens in a hash table, and
 *  allocates separate byte streams per token.  Consumers of
 *  this class, eg {@link FreqProxTermsWriter} and {@link
 *  TermVectorsConsumer}, write their own byte streams
 *  under each term.
 */
public class TermsHash extends DocBeginConsumer {

	private final TermsHashConsumer mConsumer;
	private final TermsHash mNextTermsHash;
	
	private final IntBlockPool mIntPool;
	private final ByteBlockPool mBytePool;
	private ByteBlockPool mPrimaryBytePool;

	// Used when comparing postings via termRefComp, in TermsHashPerField
	//private final BytesRef mTermRef1 = new BytesRef();
	//private final BytesRef mTermRef2 = new BytesRef();

	// Used by perField to obtain terms from the analysis chain
	//private final BytesRef mTermBytesRef = new BytesRef(10);
	
	private final boolean mTrackAllocations;
	
	public TermsHash(DocumentWriter writer, TermsHashConsumer consumer, 
			boolean trackAllocations, TermsHash next) { 
		super(writer);
		
		mConsumer = consumer;
		mNextTermsHash = next;
		mIntPool = new IntBlockPool(writer.getIntAllocator());
		mBytePool = new ByteBlockPool(writer.getByteAllocator());
		mTrackAllocations = trackAllocations;
		
		if (next != null) { 
			// We are primary
			mPrimaryBytePool = mBytePool;
			next.mPrimaryBytePool = mBytePool;
		}
	}
	
	final TermsHashConsumer getConsumer() { return mConsumer; }
	final IntBlockPool getIntPool() { return mIntPool; }
	final ByteBlockPool getBytePool() { return mBytePool; }
	final ByteBlockPool getPrimaryBytePool() { return mPrimaryBytePool; }
	final boolean isTrackAllocations() { return mTrackAllocations; }
	
	@Override
	public DocBeginConsumerPerField addField(
			DocFieldConsumerPerField consumer, IFieldInfo fieldInfo) { 
		return new TermsHashPerField(
				(DocumentInverterPerField)consumer, fieldInfo, this, mNextTermsHash);
	}
	
	@Override
	public void startDocument() throws IOException {
	    mConsumer.startDocument();
	    if (mNextTermsHash != null) 
	    	mNextTermsHash.getConsumer().startDocument();
	}
	
	@Override
	public void finishDocument() throws IOException {
	    mConsumer.finishDocument(this);
	    if (mNextTermsHash != null) 
	    	mNextTermsHash.getConsumer().finishDocument(mNextTermsHash);
	}

	@Override
	public void abort() {
	    reset();
	    try {
	    	mConsumer.abort();
	    } finally {
	    	if (mNextTermsHash != null) 
	    		mNextTermsHash.abort();
	    }
	}
	
	@Override
	public void flush(DocBeginConsumerPerField.List fields, ISegmentWriteState state) 
			throws IOException { 
		final TermsHashConsumerPerField.List childFields = new TermsHashConsumerPerField.List();
		final DocBeginConsumerPerField.List nextFields = (mNextTermsHash != null) ? 
				new DocBeginConsumerPerField.List() : null;
		
		for (DocBeginConsumerPerField field : fields.values()) { 
			TermsHashPerField termsField = (TermsHashPerField)field;
			childFields.put(termsField.getFieldInfo().getName(), termsField.getConsumerPerField());
			if (nextFields != null) 
				nextFields.put(termsField.getFieldInfo().getName(), termsField.getNextPerField());
		}
		
		mConsumer.flush(childFields, state);
		if (mNextTermsHash != null) 
			mNextTermsHash.flush(nextFields, state);
	}
	
	@Override
	public boolean freeRAM() {
	    return false;
	}
	
	// Clear all state
	final void reset() {
	    mIntPool.reset();
	    mBytePool.reset();

	    //if (mIsPrimary) 
	    //	mBytePool.reset();
	}
	
}
