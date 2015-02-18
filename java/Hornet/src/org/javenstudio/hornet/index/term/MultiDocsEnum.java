package org.javenstudio.hornet.index.term;

import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.index.term.DocsEnum;

/**
 * Exposes flex API, merged from flex API of sub-segments.
 *
 */
public final class MultiDocsEnum extends DocsEnum {
	
	private final MultiTermsEnum mParent;
	private final DocsEnum[] mSubDocsEnum;
	
	private EnumWithSlice[] mSubs;
	private DocsEnum mCurrent;
	
	private int mNumSubs;
	private int mUpto;
	private int mCurrentBase;
	private int mDoc = -1;

	public MultiDocsEnum(MultiTermsEnum parent, int subReaderCount) {
		mParent = parent;
		mSubDocsEnum = new DocsEnum[subReaderCount];
	}

	MultiDocsEnum reset(final EnumWithSlice[] subs, final int numSubs) throws IOException {
		mNumSubs = numSubs;
		mUpto = -1;
		mCurrent = null;
		
		mSubs = new EnumWithSlice[subs.length];
		for (int i=0; i < mSubs.length; i++) {
			mSubs[i] = new EnumWithSlice(subs[i].mDocsEnum, subs[i].mSlice);
		}
		
		return this;
	}

	public boolean canReuse(MultiTermsEnum parent) {
		return mParent == parent;
	}

	public int getNumSubs() {
		return mNumSubs;
	}

	//public DocsEnum[] getSubDocsEnum() { 
	//	return mSubDocsEnum;
	//}
	
	public int getSubDocsEnumLength() { 
		return mSubDocsEnum != null ? mSubDocsEnum.length : 0;
	}
	
	public DocsEnum getSubDocsEnumAt(int index) { 
		return mSubDocsEnum[index];
	}
	
	public void setSubDocsEnumAt(int index, DocsEnum docsEnum) { 
		mSubDocsEnum[index] = docsEnum;
	}
	
	public EnumWithSlice[] getSubs() {
		return mSubs;
	}

	public int getSubsLength() { 
		return mSubs != null ? mSubs.length : 0;
	}
	
	public EnumWithSlice getSubsAt(int index) { 
		return mSubs[index];
	}
	
	public void setSubsAt(int index, EnumWithSlice data) { 
		mSubs[index] = data;
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
	public int advance(int target) throws IOException {
		while (true) {
			if (mCurrent != null) {
				final int doc = mCurrent.advance(target-mCurrentBase);
				if (doc == NO_MORE_DOCS) 
					mCurrent = null;
				else 
					return mDoc = doc + mCurrentBase;
				
			} else if (mUpto == mNumSubs-1) {
				return mDoc = NO_MORE_DOCS;
				
			} else {
				mUpto ++;
				mCurrent = mSubs[mUpto].mDocsEnum;
				mCurrentBase = mSubs[mUpto].mSlice.getStart();
			}
		}
	}

	@Override
	public int nextDoc() throws IOException {
		while (true) {
			if (mCurrent == null) {
				if (mUpto == mNumSubs-1) {
					return mDoc = NO_MORE_DOCS;
				} else {
					mUpto ++;
					mCurrent = mSubs[mUpto].mDocsEnum;
					mCurrentBase = mSubs[mUpto].mSlice.getStart();
				}
			}

			final int doc = mCurrent.nextDoc();
			if (doc != NO_MORE_DOCS) 
				return mDoc = mCurrentBase + doc;
			
			mCurrent = null;
		}
	}

	// TODO: implement bulk read more efficiently than super
	public final static class EnumWithSlice {
		private DocsEnum mDocsEnum;
		private ReaderSlice mSlice;
    
		public EnumWithSlice(DocsEnum docsEnum, ReaderSlice slice) { 
			set(docsEnum, slice);
		}
		
		public void set(DocsEnum docsEnum, ReaderSlice slice) { 
			mDocsEnum = docsEnum;
			mSlice = slice;
		}
		
		public DocsEnum getDocsEnum() { return mDocsEnum; }
		public ReaderSlice getSlice() { return mSlice; }
		
		@Override
		public String toString() {
			return mSlice.toString() + ":" + mDocsEnum;
		}
	}

	@Override
	public String toString() {
		return "MultiDocsEnum(" + Arrays.toString(mSubs) + ")";
	}
	
}

