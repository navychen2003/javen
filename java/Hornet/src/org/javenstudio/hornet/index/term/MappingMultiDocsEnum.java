package org.javenstudio.hornet.index.term;

import java.io.IOException;

import org.javenstudio.common.indexdb.index.DocMap;
import org.javenstudio.common.indexdb.index.MergeState;
import org.javenstudio.common.indexdb.index.term.DocsEnum;

/**
 * Exposes flex API, merged from flex API of sub-segments,
 * remapping docIDs (this is used for segment merging).
 *
 */
public final class MappingMultiDocsEnum extends DocsEnum {
	
	private MultiDocsEnum.EnumWithSlice[] mSubs;
	private MergeState mMergeState;
	private DocMap mCurrentMap;
	private DocsEnum mCurrent;
	
	private int mNumSubs;
	private int mUpto;
	private int mCurrentBase;
	private int mDoc = -1;

	public MappingMultiDocsEnum reset(MultiDocsEnum docsEnum) {
		mNumSubs = docsEnum.getNumSubs();
		mSubs = docsEnum.getSubs();
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

	public MultiDocsEnum.EnumWithSlice[] getSubs() {
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
					mCurrent = mSubs[mUpto].getDocsEnum();
					mCurrentBase = mMergeState.getDocBaseAt(reader);
					mCurrentMap = mMergeState.getDocMapAt(reader);
					
					assert mCurrentMap.getMaxDoc() == mSubs[mUpto].getSlice().getLength(): 
						"readerIndex=" + reader + " subs.len=" + mSubs.length + " len1=" 
						+ mCurrentMap.getMaxDoc() + " vs " + mSubs[mUpto].getSlice().getLength();
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
	
}

