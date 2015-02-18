package org.javenstudio.hornet.index.term;

import org.javenstudio.common.indexdb.index.segment.ReaderSlice;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * Concatenates multiple Bits together, on every lookup.
 *
 * <p><b>NOTE</b>: This is very costly, as every lookup must
 * do a binary search to locate the right sub-reader.
 *
 */
public final class MultiBits implements Bits {
	
	private final Bits[] mSubs;
	private final boolean mDefaultValue;

	// length is 1+subs.length (the last entry has the maxDoc):
	private final int[] mStarts;

	public MultiBits(Bits[] subs, int[] starts, boolean defaultValue) {
		assert starts.length == 1+subs.length;
		
		mSubs = subs;
		mStarts = starts;
		mDefaultValue = defaultValue;
	}

	private boolean checkLength(int reader, int doc) {
		final int length = mStarts[1+reader] - mStarts[reader];
		assert doc - mStarts[reader] < length: "doc=" + doc + " reader=" + reader 
			+ " starts[reader]=" + mStarts[reader] + " length=" + length;
		
		return true;
	}

	public boolean get(int doc) {
		final int reader = ReaderUtil.subIndex(doc, mStarts);
		assert reader != -1;
		final Bits bits = mSubs[reader];
		
		if (bits == null) {
			return mDefaultValue;
			
		} else {
			assert checkLength(reader, doc);
			return bits.get(doc-mStarts[reader]);
		}
	}
  
	@Override
	public String toString() {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append(mSubs.length).append(" subs: ");
		for (int i=0; i < mSubs.length; i++) {
			if (i != 0) sbuf.append("; ");
			if (mSubs[i] == null) {
				sbuf.append("s=").append(mStarts[i]).append(" l=null");
			} else {
				sbuf.append("s=").append(mStarts[i]).append(" l=")
					.append(mSubs[i].length()).append(" b=").append(mSubs[i]);
			}
		}
		sbuf.append(" end=").append(mStarts[mSubs.length]);
		return sbuf.toString();
	}

	/**
	 * Represents a sub-Bits from 
	 * {@link MultiBits#getMatchingSub(ReaderSlice) getMatchingSub()}.
	 */
	public final static class SubResult {
		private boolean mMatches = false;
		private Bits mResult = null;
		
		public SubResult() {}
		
		public void set(Bits result, boolean matches) { 
			mMatches = matches;
			mResult = result;
		}
		
		public final boolean getMatches() { return mMatches; }
		public final Bits getResult() { return mResult; }
	}

	/**
	 * Returns a sub-Bits matching the provided <code>slice</code>
	 * <p>
	 * Because <code>null</code> usually has a special meaning for
	 * Bits (e.g. no deleted documents), you must check
	 * {@link SubResult#matches} instead to ensure the sub was 
	 * actually found.
	 */
	public SubResult getMatchingSub(ReaderSlice slice) {
		final int reader = ReaderUtil.subIndex(slice.getStart(), mStarts);
		final SubResult subResult = new SubResult();
		
		assert reader != -1;
		assert reader < mSubs.length: "slice=" + slice + " starts[-1]=" + mStarts[mStarts.length-1];
		
		if (mStarts[reader] == slice.getStart() && mStarts[1+reader] == slice.getStart()+slice.getLength()) 
			subResult.set(mSubs[reader], true);
		else 
			subResult.set(null, false);
		
		return subResult;
	}

	public int length() {
		return mStarts[mStarts.length-1];
	}
	
}
