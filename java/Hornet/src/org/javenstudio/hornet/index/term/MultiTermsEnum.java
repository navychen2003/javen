package org.javenstudio.hornet.index.term;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.index.segment.BitsSlice;
import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.index.term.DocsAndPositionsEnum;
import org.javenstudio.common.indexdb.index.term.DocsEnum;
import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.common.util.Logger;

/**
 * Exposes flex API, merged from flex API of sub-segments.
 * This does a merge sort, by term text, of the sub-readers.
 *
 */
public final class MultiTermsEnum extends TermsEnum {
	private static final Logger LOG = Logger.getLogger(MultiTermsEnum.class);
    
	private final TermMergeQueue mQueue;
	private final TermsEnumWithSlice[] mSubs;        // all of our subs (one per sub-reader)
	private final TermsEnumWithSlice[] mCurrentSubs; // current subs that have at least one term for this field
	private final TermsEnumWithSlice[] mTop;
	private final MultiDocsEnum.EnumWithSlice[] mSubDocs;
	private final MultiDocsAndPositionsEnum.EnumWithSlice[] mSubDocsAndPositions;

	private BytesRef mLastSeek;
	private boolean mLastSeekExact;
	private final BytesRef mLastSeekScratch = new BytesRef();

	private int mNumTop;
	private int mNumSubs;
	private BytesRef mCurrent;
	private Comparator<BytesRef> mTermComp;

	static class TermsEnumIndex {
		//public final static TermsEnumIndex[] EMPTY_ARRAY = new TermsEnumIndex[0];
		
		private final int mSubIndex;
		private final TermsEnum mTermsEnum;

		public TermsEnumIndex(TermsEnum termsEnum, int subIndex) {
			mTermsEnum = termsEnum;
			mSubIndex = subIndex;
		}
		
		@Override
		public String toString() { 
			return "TermsEnumIndex{termsEnum=" + mTermsEnum + ",subIndex=" + mSubIndex + "}";
		}
	}

	public MultiTermsEnum(ReaderSlice[] slices) {
		mQueue = new TermMergeQueue(slices.length);
		mTop = new TermsEnumWithSlice[slices.length];
		mSubs = new TermsEnumWithSlice[slices.length];
		mSubDocs = new MultiDocsEnum.EnumWithSlice[slices.length];
		mSubDocsAndPositions = new MultiDocsAndPositionsEnum.EnumWithSlice[slices.length];
		
		for (int i=0; i < slices.length; i++) {
			mSubs[i] = new TermsEnumWithSlice(i, slices[i]);
			mSubDocs[i] = new MultiDocsEnum.EnumWithSlice(null, slices[i]);
			mSubDocsAndPositions[i] = new MultiDocsAndPositionsEnum.EnumWithSlice(null, slices[i]);
		}
		
		mCurrentSubs = new TermsEnumWithSlice[slices.length];
	}

	public int getMatchCount() {
		return mNumTop;
	}

	public TermsEnumWithSlice[] getMatchArray() {
		return mTop;
	}
	
	@Override
	public BytesRef getTerm() {
		return mCurrent;
	}

	@Override
	public Comparator<BytesRef> getComparator() {
		return mTermComp;
	}

	/** 
	 * The terms array must be newly created TermsEnum, ie
	 *  {@link TermsEnum#next} has not yet been called. 
	 */
	public TermsEnum reset(TermsEnumIndex[] termsEnumsIndex) throws IOException {
		assert termsEnumsIndex.length <= mTop.length;
		
		mNumSubs = 0;
		mNumTop = 0;
		mTermComp = null;
		mQueue.clear();
		
		for (int i=0; i < termsEnumsIndex.length; i++) {
			final TermsEnumIndex termsEnumIndex = termsEnumsIndex[i];
			assert termsEnumIndex != null;

			// init our term comp
			if (mTermComp == null) {
				mQueue.mTermComp = mTermComp = termsEnumIndex.mTermsEnum.getComparator();
				
			} else {
				// We cannot merge sub-readers that have
				// different TermComps
				final Comparator<BytesRef> subTermComp = termsEnumIndex.mTermsEnum.getComparator();
				if (subTermComp != null && !subTermComp.equals(mTermComp)) {
					throw new IllegalStateException("sub-readers have different BytesRef.Comparators: " 
							+ subTermComp + " vs " + mTermComp + "; cannot merge");
				}
			}

			final BytesRef term; 
			try {
				//if (LOG.isDebugEnabled())
				//	LOG.debug("reset: " + termsEnumIndex + " next");
				
				term = termsEnumIndex.mTermsEnum.next();
				
				//if (LOG.isDebugEnabled()) {
				//	LOG.debug("reset: " + termsEnumIndex + " next term=" 
				//			+ (term != null ? ""+term.getLength()+" bytes" : "null"));
				//}
			} catch (RuntimeException e) { 
				if (LOG.isWarnEnabled())
					LOG.warn("reset: " + termsEnumIndex + " next error: " + e, e);
				
				throw e;
			}
			
			if (term != null) {
				final TermsEnumWithSlice entry = mSubs[termsEnumIndex.mSubIndex];
				entry.reset(termsEnumIndex.mTermsEnum, term);
				mQueue.add(entry);
				mCurrentSubs[mNumSubs++] = entry;
			} else {
				// field has no terms
			}
		}

		if (mQueue.size() == 0) 
			return TermsEnum.EMPTY;
		
		return this;
	}

	@Override
	public boolean seekExact(BytesRef term, boolean useCache) throws IOException {
		mQueue.clear();
		mNumTop = 0;

		boolean seekOpt = false;
		if (mLastSeek != null && mTermComp.compare(mLastSeek, term) <= 0) 
			seekOpt = true;

		mLastSeek = null;
		mLastSeekExact = true;

		for (int i=0; i < mNumSubs; i++) {
			final boolean status;
			
			// LUCENE-2130: if we had just seek'd already, prior
			// to this seek, and the new seek term is after the
			// previous one, don't try to re-seek this sub if its
			// current term is already beyond this new seek term.
			// Doing so is a waste because this sub will simply
			// seek to the same spot.
			if (seekOpt) {
				final BytesRef curTerm = mCurrentSubs[i].mCurrent;
				if (curTerm != null) {
					final int cmp = mTermComp.compare(term, curTerm);
					if (cmp == 0) 
						status = true;
					else if (cmp < 0) 
						status = false;
					else 
						status = mCurrentSubs[i].mTerms.seekExact(term, useCache);
				} else {
					status = false;
				}
			} else {
				status = mCurrentSubs[i].mTerms.seekExact(term, useCache);
			}

			if (status) {
				mTop[mNumTop++] = mCurrentSubs[i];
				mCurrent = mCurrentSubs[i].mCurrent = mCurrentSubs[i].mTerms.getTerm();
				assert term.equals(mCurrentSubs[i].mCurrent);
			}
		}

		// if at least one sub had exact match to the requested
		// term then we found match
		return mNumTop > 0;
	}

	@Override
	public SeekStatus seekCeil(BytesRef term, boolean useCache) throws IOException {
		mQueue.clear();
		mNumTop = 0;
		mLastSeekExact = false;

		boolean seekOpt = false;
		if (mLastSeek != null && mTermComp.compare(mLastSeek, term) <= 0) 
			seekOpt = true;

		mLastSeekScratch.copyBytes(term);
		mLastSeek = mLastSeekScratch;

		for (int i=0; i < mNumSubs; i++) {
			final SeekStatus status;
			
			// LUCENE-2130: if we had just seek'd already, prior
			// to this seek, and the new seek term is after the
			// previous one, don't try to re-seek this sub if its
			// current term is already beyond this new seek term.
			// Doing so is a waste because this sub will simply
			// seek to the same spot.
			if (seekOpt) {
				final BytesRef curTerm = mCurrentSubs[i].mCurrent;
				if (curTerm != null) {
					final int cmp = mTermComp.compare(term, curTerm);
					if (cmp == 0) 
						status = SeekStatus.FOUND;
					else if (cmp < 0) 
						status = SeekStatus.NOT_FOUND;
					else 
						status = mCurrentSubs[i].mTerms.seekCeil(term, useCache);
				} else {
					status = SeekStatus.END;
				}
			} else {
				status = mCurrentSubs[i].mTerms.seekCeil(term, useCache);
			}

			if (status == SeekStatus.FOUND) {
				mTop[mNumTop++] = mCurrentSubs[i];
				mCurrent = mCurrentSubs[i].mCurrent = mCurrentSubs[i].mTerms.getTerm();
				
			} else {
				if (status == SeekStatus.NOT_FOUND) {
					mCurrentSubs[i].mCurrent = mCurrentSubs[i].mTerms.getTerm();
					assert mCurrentSubs[i].mCurrent != null;
					mQueue.add(mCurrentSubs[i]);
					
				} else {
					// enum exhausted
					mCurrentSubs[i].mCurrent = null;
				}
			}
		}

		if (mNumTop > 0) {
			// at least one sub had exact match to the requested term
			return SeekStatus.FOUND;
			
		} else if (mQueue.size() > 0) {
			// no sub had exact match, but at least one sub found
			// a term after the requested term -- advance to that
			// next term:
			pullTop();
			return SeekStatus.NOT_FOUND;
			
		} else {
			return SeekStatus.END;
		}
	}

	@Override
	public void seekExact(long ord) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getOrd() throws IOException {
		throw new UnsupportedOperationException();
	}

	private void pullTop() {
		// extract all subs from the queue that have the same
		// top term
		assert mNumTop == 0;
		while (true) {
			mTop[mNumTop++] = mQueue.pop();
			if (mQueue.size() == 0 || !(mQueue.top()).mCurrent.bytesEquals(mTop[0].mCurrent)) 
				break;
		}
		mCurrent = mTop[0].mCurrent;
	}

	private void pushTop() throws IOException {
		// call next() on each top, and put back into queue
		for (int i=0; i < mNumTop; i++) {
			mTop[i].mCurrent = mTop[i].mTerms.next();
			if (mTop[i].mCurrent != null) {
				mQueue.add(mTop[i]);
			} else {
				// no more fields in this reader
			}
		}
		mNumTop = 0;
	}

	@Override
	public BytesRef next() throws IOException {
		if (mLastSeekExact) {
			// Must seekCeil at this point, so those subs that
			// didn't have the term can find the following term.
			// NOTE: we could save some CPU by only seekCeil the
			// subs that didn't match the last exact seek... but
			// most impls short-circuit if you seekCeil to term
			// they are already on.
			final SeekStatus status = seekCeil(mCurrent);
			assert status == SeekStatus.FOUND;
			mLastSeekExact = false;
		}
		mLastSeek = null;

		// restore queue
		pushTop();

		// gather equal top fields
		if (mQueue.size() > 0) 
			pullTop();
		else 
			mCurrent = null;

		return mCurrent;
	}

	@Override
	public int getDocFreq() throws IOException {
		int sum = 0;
		for (int i=0; i < mNumTop; i++) {
			sum += mTop[i].mTerms.getDocFreq();
		}
		return sum;
	}

	@Override
	public long getTotalTermFreq() throws IOException {
		long sum = 0;
		for (int i=0; i < mNumTop; i++) {
			final long v = mTop[i].mTerms.getTotalTermFreq();
			if (v == -1) 
				return v;
			
			sum += v;
		}
		return sum;
	}

	@Override
	public IDocsEnum getDocs(Bits liveDocs, IDocsEnum reuse, int flags) 
			throws IOException {
		MultiDocsEnum docsEnum;
		// Can only reuse if incoming enum is also a MultiDocsEnum
		if (reuse != null && reuse instanceof MultiDocsEnum) {
			docsEnum = (MultiDocsEnum) reuse;
			// ... and was previously created w/ this MultiTermsEnum:
			if (!docsEnum.canReuse(this)) 
				docsEnum = new MultiDocsEnum(this, mSubs.length);
			
		} else {
			docsEnum = new MultiDocsEnum(this, mSubs.length);
		}
    
		final MultiBits multiLiveDocs;
		if (liveDocs instanceof MultiBits) 
			multiLiveDocs = (MultiBits) liveDocs;
		else 
			multiLiveDocs = null;

		int upto = 0;

		for (int i=0; i < mNumTop; i++) {
			final TermsEnumWithSlice entry = mTop[i];
			final Bits b;

			if (multiLiveDocs != null) {
				// optimize for common case: requested skip docs is a
				// congruent sub-slice of MultiBits: in this case, we
				// just pull the liveDocs from the sub reader, rather
				// than making the inefficient
				// Slice(Multi(sub-readers)):
				final MultiBits.SubResult sub = multiLiveDocs.getMatchingSub(entry.mSubSlice);
				if (sub.getMatches()) {
					b = sub.getResult();
				} else {
					// custom case: requested skip docs is foreign:
					// must slice it on every access
					b = new BitsSlice(liveDocs, entry.mSubSlice);
				}
			} else if (liveDocs != null) {
				b = new BitsSlice(liveDocs, entry.mSubSlice);
			} else {
				// no deletions
				b = null;
			}

			assert entry.mIndex < docsEnum.getSubDocsEnumLength() : entry.mIndex + " vs " 
				+ docsEnum.getSubDocsEnumLength() + "; " + mSubs.length;
			
			final DocsEnum subDocsEnum = (DocsEnum)entry.mTerms.getDocs(
					b, docsEnum.getSubDocsEnumAt(entry.mIndex), flags);
			
			if (subDocsEnum != null) {
				docsEnum.setSubDocsEnumAt(entry.mIndex, subDocsEnum);
				mSubDocs[upto].set(subDocsEnum, entry.mSubSlice);
				upto++;
				
			} else {
				// One of our subs cannot provide freqs:
				assert false;
			}
		}

		if (upto == 0) 
			return null;
		
		return docsEnum.reset(mSubDocs, upto);
	}

	@Override
	public IDocsAndPositionsEnum getDocsAndPositions(Bits liveDocs, 
			IDocsAndPositionsEnum reuse, int flags) throws IOException {
		MultiDocsAndPositionsEnum docsAndPositionsEnum;
		// Can only reuse if incoming enum is also a MultiDocsAndPositionsEnum
		if (reuse != null && reuse instanceof MultiDocsAndPositionsEnum) {
			docsAndPositionsEnum = (MultiDocsAndPositionsEnum) reuse;
			// ... and was previously created w/ this MultiTermsEnum:
			if (!docsAndPositionsEnum.canReuse(this)) 
				docsAndPositionsEnum = new MultiDocsAndPositionsEnum(this, mSubs.length);
			
		} else {
			docsAndPositionsEnum = new MultiDocsAndPositionsEnum(this, mSubs.length);
		}
    
		final MultiBits multiLiveDocs;
		if (liveDocs instanceof MultiBits) 
			multiLiveDocs = (MultiBits) liveDocs;
		else 
			multiLiveDocs = null;

		int upto = 0;

		for (int i=0; i < mNumTop; i++) {
			final TermsEnumWithSlice entry = mTop[i];
			final Bits b;

			if (multiLiveDocs != null) {
				// Optimize for common case: requested skip docs is a
				// congruent sub-slice of MultiBits: in this case, we
				// just pull the liveDocs from the sub reader, rather
				// than making the inefficient
				// Slice(Multi(sub-readers)):
				final MultiBits.SubResult sub = multiLiveDocs.getMatchingSub(mTop[i].mSubSlice);
				if (sub.getMatches()) {
					b = sub.getResult();
				} else {
					// custom case: requested skip docs is foreign:
					// must slice it on every access (very
					// inefficient)
					b = new BitsSlice(liveDocs, mTop[i].mSubSlice);
				}
			} else if (liveDocs != null) {
				b = new BitsSlice(liveDocs, mTop[i].mSubSlice);
			} else {
				// no deletions
				b = null;
			}

			assert entry.mIndex < docsAndPositionsEnum.getSubDocsAndPositionsEnumLength() : entry.mIndex 
				+ " vs " + docsAndPositionsEnum.getSubDocsAndPositionsEnumLength() + "; " + mSubs.length;
			
			final DocsAndPositionsEnum subPostings = (DocsAndPositionsEnum)entry.mTerms.getDocsAndPositions(
					b, docsAndPositionsEnum.getSubDocsAndPositionsEnumAt(entry.mIndex), flags);

			if (subPostings != null) {
				docsAndPositionsEnum.setSubDocsAndPositionsEnumAt(entry.mIndex, subPostings);
				mSubDocsAndPositions[upto].set(subPostings, entry.mSubSlice);
				upto++;
				
			} else {
				if (entry.mTerms.getDocs(b, null, 0) != null) {
					// At least one of our subs does not store
					// offsets or positions -- we can't correctly
					// produce a MultiDocsAndPositions enum
					return null;
				}
			}
		}

		if (upto == 0) 
			return null;
		
		return docsAndPositionsEnum.reset(mSubDocsAndPositions, upto);
	}

	private final static class TermsEnumWithSlice {
		private final ReaderSlice mSubSlice;
		private TermsEnum mTerms;
		private BytesRef mCurrent;
		private final int mIndex;

		public TermsEnumWithSlice(int index, ReaderSlice subSlice) {
			mSubSlice = subSlice;
			mIndex = index;
			assert subSlice.getLength() >= 0: "length=" + subSlice.getLength();
		}

		public void reset(TermsEnum terms, BytesRef term) {
			mTerms = terms;
			mCurrent = term;
		}

		@Override
		public String toString() {
			return mSubSlice.toString() + ":" + mTerms;
		}
	}

	private final static class TermMergeQueue extends PriorityQueue<TermsEnumWithSlice> {
		private Comparator<BytesRef> mTermComp;
		
		public TermMergeQueue(int size) {
			super(size);
		}

		@Override
		protected boolean lessThan(TermsEnumWithSlice termsA, TermsEnumWithSlice termsB) {
			final int cmp = mTermComp.compare(termsA.mCurrent, termsB.mCurrent);
			if (cmp != 0) 
				return cmp < 0;
			else 
				return termsA.mSubSlice.getStart() < termsB.mSubSlice.getStart();
		}
	}

	@Override
	public String toString() {
		return "MultiTermsEnum(" + Arrays.toString(mSubs) + ")";
	}
	
}
