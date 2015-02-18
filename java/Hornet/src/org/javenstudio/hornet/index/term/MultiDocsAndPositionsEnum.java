package org.javenstudio.hornet.index.term;

import java.io.IOException;

import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Exposes flex API, merged from flex API of sub-segments.
 *
 */
public final class MultiDocsAndPositionsEnum extends DocsAndPositionsEnum {
	
	private final MultiTermsEnum mParent;
	private final DocsAndPositionsEnum[] mSubDocsAndPositionsEnum;
	
	private EnumWithSlice[] mSubs;
	private DocsAndPositionsEnum mCurrent;
	
	private int mNumSubs;
	private int mUpto;
	private int mCurrentBase;
	private int mDoc = -1;

	public MultiDocsAndPositionsEnum(MultiTermsEnum parent, int subReaderCount) {
		mParent = parent;
		mSubDocsAndPositionsEnum = new DocsAndPositionsEnum[subReaderCount];
	}

	public MultiDocsAndPositionsEnum reset(
			final EnumWithSlice[] subs, final int numSubs) throws IOException {
		mNumSubs = numSubs;
		mUpto = -1;
		mCurrent = null;
		
		mSubs = new EnumWithSlice[subs.length];
		for (int i=0; i < subs.length; i++) {
			mSubs[i] = new EnumWithSlice(subs[i].mDocsAndPositionsEnum, subs[i].mSlice);
		}
		
		return this;
	}

	public boolean canReuse(MultiTermsEnum parent) {
		return mParent == parent;
	}
	
	public int getNumSubs() {
		return mNumSubs;
	}

	//public DocsAndPositionsEnum[] getSubDocsAndPositionsEnum() { 
	//	return mSubDocsAndPositionsEnum;
	//}
	
	public int getSubDocsAndPositionsEnumLength() { 
		return mSubDocsAndPositionsEnum != null ? mSubDocsAndPositionsEnum.length : 0;
	}
	
	public DocsAndPositionsEnum getSubDocsAndPositionsEnumAt(int index) { 
		return mSubDocsAndPositionsEnum[index];
	}
	
	public void setSubDocsAndPositionsEnumAt(int index, DocsAndPositionsEnum data) { 
		mSubDocsAndPositionsEnum[index] = data;
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
				mCurrent = mSubs[mUpto].mDocsAndPositionsEnum;
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
					mCurrent = mSubs[mUpto].mDocsAndPositionsEnum;
					mCurrentBase = mSubs[mUpto].mSlice.getStart();
				}
			}

			final int doc = mCurrent.nextDoc();
			if (doc != NO_MORE_DOCS) 
				return mDoc = mCurrentBase + doc;
			
			mCurrent = null;
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

	//@Override
	//public boolean hasPayload() {
	//	return mCurrent.hasPayload();
	//}

	@Override
	public BytesRef getPayload() throws IOException {
		return mCurrent.getPayload();
	}

	// TODO: implement bulk read more efficiently than super
	public final static class EnumWithSlice {
		private DocsAndPositionsEnum mDocsAndPositionsEnum;
		private ReaderSlice mSlice;
		
		public EnumWithSlice(DocsAndPositionsEnum docsAndPositionsEnum, ReaderSlice slice) { 
			set(docsAndPositionsEnum, slice);
		}
		
		public void set(DocsAndPositionsEnum docsAndPositionsEnum, ReaderSlice slice) { 
			mDocsAndPositionsEnum = docsAndPositionsEnum;
			mSlice = slice;
		}
		
		public DocsAndPositionsEnum getDocsAndPositionsEnum() { return mDocsAndPositionsEnum; }
		public ReaderSlice getSlice() { return mSlice; }
	}
	
}

