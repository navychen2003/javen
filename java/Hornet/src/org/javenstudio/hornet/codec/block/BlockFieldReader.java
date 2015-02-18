package org.javenstudio.hornet.codec.block;

import java.io.IOException;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.index.term.Terms;
import org.javenstudio.common.indexdb.store.ByteArrayDataInput;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.store.fst.ByteSequenceOutputs;
import org.javenstudio.hornet.store.fst.FST;

final class BlockFieldReader extends Terms {
	
	private final BlockTreeTermsReader mReader;
	
	private final IFieldInfo mFieldInfo;
	private final BytesRef mRootCode;
	private final FST<BytesRef> mIndex;
    
	private final long mNumTerms;
	private final long mSumTotalTermFreq;
	private final long mSumDocFreq;
	private final int mDocCount;
	private final long mIndexStartFP;
	private final long mRootBlockFP;
    
    public BlockFieldReader(BlockTreeTermsReader reader, 
    		IFieldInfo fieldInfo, long numTerms, BytesRef rootCode, long sumTotalTermFreq, 
    		long sumDocFreq, int docCount, long indexStartFP, IIndexInput indexIn) 
    		throws IOException {
    	assert numTerms > 0;
    	
    	mReader = reader;
    	mFieldInfo = fieldInfo;
    	mNumTerms = numTerms;
    	mSumTotalTermFreq = sumTotalTermFreq; 
    	mSumDocFreq = sumDocFreq; 
    	mDocCount = docCount;
    	mIndexStartFP = indexStartFP;
    	mRootCode = rootCode;

    	mRootBlockFP = (new ByteArrayDataInput(
    			rootCode.mBytes, rootCode.mOffset, rootCode.mLength)).readVLong() >>> 
    			BlockTreeTermsWriter.OUTPUT_FLAGS_NUM_BITS;

    	if (indexIn != null) {
    		final IndexInput clone = (IndexInput)indexIn.clone();
    		clone.seek(indexStartFP);
    		mIndex = new FST<BytesRef>(clone, ByteSequenceOutputs.getSingleton());
        
    	} else {
    		mIndex = null;
    	}
	}

    final FST<BytesRef> getIndex() { return mIndex; }
    final IFieldInfo getFieldInfo() { return mFieldInfo; }
    final BytesRef getRootCode() { return mRootCode; }
    
    final long getIndexStartFP() { return mIndexStartFP; }
    final long getRootBlockFP() { return mRootBlockFP; }
    
	/** For debugging -- used by CheckIndex too*/
	// TODO: maybe push this into Terms?
	public BlockStats computeStats() throws IOException {
		return new SegmentTermsEnum(mReader, this).computeBlockStats();
    }

    @Override
    public Comparator<BytesRef> getComparator() {
    	return BytesRef.getUTF8SortedAsUnicodeComparator();
    }

    @Override
    public ITermsEnum iterator(ITermsEnum reuse) throws IOException {
    	return new SegmentTermsEnum(mReader, this);
    }

    @Override
    public long size() {
    	return mNumTerms;
    }

    @Override
    public long getSumTotalTermFreq() {
    	return mSumTotalTermFreq;
    }

    @Override
    public long getSumDocFreq() {
    	return mSumDocFreq;
    }

    @Override
    public int getDocCount() throws IOException {
    	return mDocCount;
    }
    
    @Override
    public String toString() { 
    	return getClass().getSimpleName() + "{reader=" + mReader 
    			+ ",fieldInfo=" + mFieldInfo + ",numTerms=" + mNumTerms 
    			+ ",docCount=" + mDocCount + "}";
    }
    
}
