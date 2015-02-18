package org.javenstudio.hornet.index.term;

import java.io.IOException;

import org.javenstudio.common.indexdb.index.DocMap;
import org.javenstudio.common.indexdb.index.MergeState;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Exposes flex API, merged from flex API of sub-segments,
 * remapping docIDs (this is used for segment merging).
 *
 */
public final class MappingMultiDocsAndPositionsEnum extends DocsAndPositionsEnum {
	
	private MultiDocsAndPositionsEnum.EnumWithSlice[] mSubs;
	private MergeState mMergeState;
	
	private DocMap mCurrentMap;
	private DocsAndPositionsEnum mCurrent;
	
	private int mNumSubs;
	private int mUpto;
	private int mCurrentBase;
	private int mDoc = -1;

	public MappingMultiDocsAndPositionsEnum reset(MultiDocsAndPositionsEnum postingsEnum) {
		mNumSubs = postingsEnum.getNumSubs();
		mSubs = postingsEnum.getSubs();
		mUpto = -1;
		mCurrent = null;
		return this;
	}

	public void setMergeState(MergeState mergeState) {
		mMergeState = mergeState;
	}
  
	public int getNumSubs() {
		return mNumSubs;
	}

	public MultiDocsAndPositionsEnum.EnumWithSlice[] getSubs() {
		return mSubs;
	}

	@Override
	public int getFreq() throws IOException {
		return mCurrent.getFreq();
	}

	@Override
	public int getDocID() {
		return mDoc;
	}

	@Override
	public int advance(int target) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int nextDoc() throws IOException {
		while (true) {
			if (mCurrent == null) {
				if (mUpto == mNumSubs-1) {
					return mDoc = NO_MORE_DOCS;
				} else {
					mUpto ++;
					
					final int reader = mSubs[mUpto].getSlice().getReaderIndex();
					mCurrent = mSubs[mUpto].getDocsAndPositionsEnum();
					mCurrentBase = mMergeState.getDocBaseAt(reader);
					mCurrentMap = mMergeState.getDocMapAt(reader);
				}
			}

			int doc = mCurrent.nextDoc();
			if (doc != NO_MORE_DOCS) {
				// compact deletions
				doc = mCurrentMap.get(doc);
				if (doc == -1) 
					continue;
				
				return mDoc = mCurrentBase + doc;
			} else {
				mCurrent = null;
			}
		}
	}

	@Override
	public int nextPosition() throws IOException {
		return mCurrent.nextPosition();
	}

	@Override
	public int startOffset() throws IOException {
		return mCurrent.startOffset();
	}
  
	@Override
	public int endOffset() throws IOException {
		return mCurrent.endOffset();
	}
  
	@Override
	public BytesRef getPayload() throws IOException {
		BytesRef payload = mCurrent.getPayload();
		if (mMergeState.getCurrentPayloadProcessorAt(mUpto) != null) 
			mMergeState.getCurrentPayloadProcessorAt(mUpto).processPayload(payload);
		
		return payload;
	}

}

