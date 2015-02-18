package org.javenstudio.common.indexdb.index.consumer;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.IField;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.index.DocState;
import org.javenstudio.common.indexdb.store.ByteSliceReader;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.BytesRefHash;
import org.javenstudio.common.indexdb.util.Counter;
import org.javenstudio.common.indexdb.util.IntPool;

final class TermsHashPerField extends DocBeginConsumerPerField {
	private static final int HASH_INIT_SIZE = 4;

	private final DocumentInverterPerField mInverter;
	private final TermsHash mTermsHash;
	private final TermsHashPerField mNextPerField;
	private final TermsHashConsumerPerField mConsumerPerField;
	
	private final Counter mBytesUsed;
	private final int mStreamCount;
	private final int mNumPostingInt;
	
	private final BytesRefHash mBytesHash;
	private ParallelPostingsArray mPostingsArray = null;
	
	private int[] mIntUptos = null;
	private int mIntUptoStart = 0;
	
	private boolean mDoCall = false;
	private boolean mDoNextCall = false;
	
	public TermsHashPerField(DocumentInverterPerField inverterPerField, IFieldInfo fieldInfo, 
			TermsHash termsHash, TermsHash nextTermsHash) { 
		super(inverterPerField, termsHash, fieldInfo);
		
		mTermsHash = termsHash;
		mInverter = inverterPerField;
		mConsumerPerField = termsHash.getConsumer().addField(this, fieldInfo);
		
		mBytesUsed = termsHash.isTrackAllocations() ? 
				termsHash.getDocumentWriter().getBytesUsedCounter() : Counter.newCounter();
	    mBytesHash = new BytesRefHash(termsHash.getPrimaryBytePool(), HASH_INIT_SIZE, 
	    		new PostingsBytesStartArray());
	    mStreamCount = mConsumerPerField.getStreamCount();
	    mNumPostingInt = 2*mStreamCount;
	    
	    if (nextTermsHash != null) 
	    	mNextPerField = (TermsHashPerField)nextTermsHash.addField(inverterPerField, fieldInfo);
	    else 
	    	mNextPerField = null;
	}
	
	final TermsHash getTermsHash() { return mTermsHash; }
	final TermsHashConsumerPerField getConsumerPerField() { return mConsumerPerField; }
	final TermsHashPerField getNextPerField() { return mNextPerField; }
	
	final DocState getDocState() { return mInverter.getDocumentWriter().getDocState(); }
	final FieldState getFieldState() { return mInverter.getFieldState(); }
	
	final BytesRefHash getBytesHash() { return mBytesHash; }
	final ParallelPostingsArray getPostingsArray() { return mPostingsArray; }
	
	final IntBlockPool getIntPool() { return mTermsHash.getIntPool(); }
	final ByteBlockPool getBytePool() { return mTermsHash.getBytePool(); }
	final ByteBlockPool getPrimaryBytePool() { return mTermsHash.getPrimaryBytePool(); }
	
	final void initReader(ByteSliceReader reader, int termID, int stream) {
	    assert stream < mStreamCount;
	    final int intStart = mPostingsArray.getIntStartAt(termID);
	    final int[] ints = getIntPool().getBufferAtStart(intStart);
	    final int upto = intStart & IntPool.INT_BLOCK_MASK;
	    reader.init(getBytePool(),
	    		mPostingsArray.getByteStartAt(termID) + stream*ByteBlockPool.FIRST_LEVEL_SIZE,
	    		ints[upto + stream]);
	}
	
	final void shrinkHash(int targetSize) {
		// Fully free the bytesHash on each flush but keep the pool untouched
		// bytesHash.clear will clear the ByteStartArray and in turn the ParallelPostingsArray too
		mBytesHash.clear(false);
	}

	public void reset() {
		mBytesHash.clear(false);
		if (mNextPerField != null)
			mNextPerField.reset();
	}

	@Override
	public void abort() {
		reset();
		if (mNextPerField != null)
			mNextPerField.abort();
	}
	
	/** Collapse the hash table & sort in-place. */
	public int[] sortPostings(Comparator<BytesRef> termComp) {
		return mBytesHash.sort(termComp);
	}
  
	@Override
	public boolean start(IField[] fields, int count) throws IOException {
		mDoCall = mConsumerPerField.start(fields, count);
		mBytesHash.reinit();
		if (mNextPerField != null) 
			mDoNextCall = mNextPerField.start(fields, count);
		
		return mDoCall || mDoNextCall;
	}
	
	@Override
  	public void start(IField field) {
		mConsumerPerField.start(field);
		if (mNextPerField != null) 
			mNextPerField.start(field);
	}
	
	// Secondary entry point (for 2nd & subsequent TermsHash),
	// because token text has already been "interned" into
	// textStart, so we hash by textStart
	private void add(IToken token, int textStart) throws IOException {
		final ByteBlockPool bytePool = getBytePool();
		final IntBlockPool intPool = getIntPool();
		
		int termID = mBytesHash.addByPoolOffset(textStart);
		
		if (termID >= 0) { // New posting
			// First time we are seeing this token since we last flushed the hash.
			// Init stream slices
			intPool.nextBuffer(mNumPostingInt);
			bytePool.nextBuffer(mNumPostingInt);
			
			mIntUptos = intPool.getBuffer();
			mIntUptoStart = intPool.getIntUpto();
			intPool.increaseIntUpto(mStreamCount);

			for (int i=0; i < mStreamCount; i++) {
				final int upto = bytePool.newSlice();
				mIntUptos[mIntUptoStart+i] = upto + bytePool.getByteOffset();
			}
			
			mPostingsArray.setIntStartAt(termID, mIntUptoStart + intPool.getIntOffset());
			mPostingsArray.setByteStartAt(termID, mIntUptos[mIntUptoStart]);
			
			mConsumerPerField.newTerm(termID, token);

		} else {
			termID = (-termID)-1; // see BytesRefHash.add()
			final int intStart = mPostingsArray.getIntStartAt(termID);
			
			mIntUptos = intPool.getBufferAtStart(intStart);
			mIntUptoStart = intStart & IntPool.INT_BLOCK_MASK;
			
			mConsumerPerField.addTerm(termID, token);
		}
	}
	
	// Primary entry point (for first TermsHash)
	@Override
	public void add(IToken token) throws IOException {
		final ByteBlockPool bytePool = getBytePool();
		final IntBlockPool intPool = getIntPool();
		
		final BytesRef termBytesRef = token.getBytesRef();
		
		// We are first in the chain so we must "intern" the
		// term text into textStart address
		// Get the text & hash of this term.
		int termID;
		try {
			termID = mBytesHash.add(termBytesRef, token.fillBytesRef());
		} catch (BytesRefHash.MaxBytesLengthExceededException e) {
			// Not enough room in current block
			// Just skip this term, to remain as robust as
			// possible during indexing.  A TokenFilter
			// can be inserted into the analyzer chain if
			// other behavior is wanted (pruning the term
			// to a prefix, throwing an exception, etc).
			if (getDocState().getMaxTermPrefix() == null) {
				final int saved = termBytesRef.getLength();
				try {
					termBytesRef.setLength(Math.min(30, Constants.MAX_TERM_LENGTH_UTF8));
					getDocState().setMaxTermPrefix(termBytesRef.toString());
				} finally {
					termBytesRef.setLength(saved);
				}
			}
			mConsumerPerField.skippingLongTerm(token);
			return;
		}
		
		if (termID >= 0) {	// New posting
			mBytesHash.byteStart(termID);
			
			// Init stream slices
			intPool.nextBuffer(mNumPostingInt);
			bytePool.nextBuffer(mNumPostingInt);
			
			mIntUptos = intPool.getBuffer();
			mIntUptoStart = intPool.getIntUpto();
			intPool.increaseIntUpto(mStreamCount);

			for (int i=0; i < mStreamCount; i++) {
				final int upto = bytePool.newSlice();
				mIntUptos[mIntUptoStart+i] = upto + bytePool.getByteOffset();
			}
			
			mPostingsArray.setIntStartAt(termID, mIntUptoStart + intPool.getIntOffset());
			mPostingsArray.setByteStartAt(termID, mIntUptos[mIntUptoStart]);
			
			mConsumerPerField.newTerm(termID, token);

		} else {
			termID = (-termID)-1; // see BytesRefHash.add()
			final int intStart = mPostingsArray.getIntStartAt(termID);
			
			mIntUptos = intPool.getBufferAtStart(intStart);
			mIntUptoStart = intStart & IntPool.INT_BLOCK_MASK;
			
			mConsumerPerField.addTerm(termID, token);
		}

		if (mDoNextCall)
			mNextPerField.add(token, mPostingsArray.getTextStartAt(termID));
	}
	
	private void writeByte(int stream, byte b) {
		final ByteBlockPool bytePool = getBytePool();
		
	    int upto = mIntUptos[mIntUptoStart+stream];
	    byte[] bytes = bytePool.getBufferAtStart(upto);
	    assert bytes != null;
	    int offset = upto & ByteBlockPool.BYTE_BLOCK_MASK;
	    if (bytes[offset] != 0) {
	    	// End of slice; allocate a new one
	    	offset = bytePool.allocSlice(bytes, offset);
	    	bytes = bytePool.getBuffer();
	    	mIntUptos[mIntUptoStart+stream] = offset + bytePool.getByteOffset();
	    }
	    bytes[offset] = b;
	    (mIntUptos[mIntUptoStart+stream])++;
	}

	public void writeBytes(int stream, byte[] b, int offset, int len) {
		// TODO: optimize
		final int end = offset + len;
		for (int i=offset; i < end; i++) {
			writeByte(stream, b[i]);
		}
	}

	public void writeVInt(int stream, int i) {
		assert stream < mStreamCount;
		while ((i & ~0x7F) != 0) {
			writeByte(stream, (byte)((i & 0x7f) | 0x80));
			i >>>= 7;
		}
		writeByte(stream, (byte) i);
	}

	@Override
	public void finish() throws IOException {
		mConsumerPerField.finish();
		if (mNextPerField != null)
			mNextPerField.finish();
	}
	
	private class PostingsBytesStartArray implements BytesRefHash.BytesStartArray {
	    public PostingsBytesStartArray() {}

	    @Override
	    public int[] init() {
	    	ParallelPostingsArray postingsArray = mPostingsArray;
	    	if (postingsArray == null) {
	    		postingsArray = mPostingsArray = getConsumerPerField().createPostingsArray(2);
	    		mBytesUsed.addAndGet(postingsArray.getSize() * postingsArray.getBytesPerPosting());
	    	}
	    	return postingsArray.getTextStarts();
	    }

	    @Override
	    public int[] grow() {
	    	ParallelPostingsArray postingsArray = mPostingsArray;
	    	final int oldSize = postingsArray.getSize();
	    	postingsArray = mPostingsArray = postingsArray.grow();
	    	mBytesUsed.addAndGet((postingsArray.getBytesPerPosting() * (postingsArray.getSize() - oldSize)));
	    	return postingsArray.getTextStarts();
	    }

	    @Override
	    public int[] clear() {
	    	if (mPostingsArray != null) {
	    		mBytesUsed.addAndGet(-(mPostingsArray.getSize() * mPostingsArray.getBytesPerPosting()));
	    		mPostingsArray = null;
	    	}
	    	return null;
	    }

	    @Override
	    public Counter bytesUsed() {
	    	return mBytesUsed;
	    }
	}
	
}
