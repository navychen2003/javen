package org.javenstudio.hornet.search.comparator;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IIntsReader;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.search.cache.FieldCache;

/** 
 * Sorts by field's natural Term sort order, using
 *  ordinals.  This is functionally equivalent to {@link
 *  TermValComparator}, but it first resolves the string
 *  to their relative ordinal positions (using the index
 *  returned by {@link FieldCache#getTermsIndex}), and
 *  does most comparisons using the ordinals.  For medium
 *  to large results, this comparator will be much faster
 *  than {@link TermValComparator}.  For very small
 *  result sets it may be slower. 
 */
@SuppressWarnings("unused")
public final class TermOrdValComparator extends FieldComparator<BytesRef> {
	
	/** Ords for each slot. */
	private final int[] mOrds;

	/** Values for each slot. */
	private final BytesRef[] mValues;

	/** 
	 * Which reader last copied a value into the slot. When
	 * we compare two slots, we just compare-by-ord if the
	 * readerGen is the same; else we must compare the
	 * values (slower).
	 */
	private final int[] mReaderGen;

	/** Gen of current reader we are on. */
	private int mCurrentReaderGen = -1;

	/** Current reader's doc ord/values. */
	private IDocTermsIndex mTermsIndex;

	private final String mField;

	/** Bottom slot, or -1 if queue isn't full yet */
	private int mBottomSlot = -1;

	/** 
	 * Bottom ord (same as ords[bottomSlot] once bottomSlot
	 * is set).  Cached for faster compares.
	 */
	private int mBottomOrd;

	/** True if current bottom slot matches the current reader. */
	private boolean mBottomSameReader;

	/** 
	 * Bottom value (same as values[bottomSlot] once
	 * bottomSlot is set).  Cached for faster compares.
	 */
	private BytesRef mBottomValue;

	private final BytesRef mTempBR = new BytesRef();

	public TermOrdValComparator(int numHits, String field) {
		mOrds = new int[numHits];
		mValues = new BytesRef[numHits];
		mReaderGen = new int[numHits];
		mField = field;
	}

	@Override
	public int compare(int slot1, int slot2) {
		if (mReaderGen[slot1] == mReaderGen[slot2]) 
			return mOrds[slot1] - mOrds[slot2];

		final BytesRef val1 = mValues[slot1];
		final BytesRef val2 = mValues[slot2];
		if (val1 == null) {
			if (val2 == null) 
				return 0;
			return -1;
		} else if (val2 == null) {
			return 1;
		}
		return val1.compareTo(val2);
	}

	@Override
	public int compareBottom(int doc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void copy(int slot, int doc) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int compareDocToValue(int doc, BytesRef value) {
		BytesRef docValue = mTermsIndex.getTerm(doc, mTempBR);
		if (docValue == null) {
			if (value == null) 
				return 0;
			return -1;
		} else if (value == null) {
			return 1;
		}
		return docValue.compareTo(value);
	}

	/** 
	 * Base class for specialized (per bit width of the
	 * ords) per-segment comparator.  NOTE: this is messy;
	 * we do this only because hotspot can't reliably inline
	 * the underlying array access when looking up doc->ord
	 */
	abstract class PerSegmentComparator extends FieldComparator<BytesRef> {
    
		@Override
		public IFieldComparator<BytesRef> setNextReader(IAtomicReaderRef context) throws IOException {
			return TermOrdValComparator.this.setNextReader(context);
		}

		@Override
		public int compare(int slot1, int slot2) {
			return TermOrdValComparator.this.compare(slot1, slot2);
		}

		@Override
		public void setBottom(final int bottom) {
			TermOrdValComparator.this.setBottom(bottom);
		}

		@Override
		public BytesRef getValue(int slot) {
			return TermOrdValComparator.this.getValue(slot);
		}

		@Override
		public int compareValues(BytesRef val1, BytesRef val2) {
			if (val1 == null) {
				if (val2 == null) 
					return 0;
				return -1;
			} else if (val2 == null) {
				return 1;
			}
			return val1.compareTo(val2);
		}

		@Override
		public int compareDocToValue(int doc, BytesRef value) {
			return TermOrdValComparator.this.compareDocToValue(doc, value);
		}
	}

	// Used per-segment when bit width of doc->ord is 8:
	private final class ByteOrdComparator extends PerSegmentComparator {
		
		private final byte[] mReaderOrds;
		private final IDocTermsIndex mTermsIndex;
		private final int mDocBase;

		public ByteOrdComparator(byte[] readerOrds, IDocTermsIndex termsIndex, int docBase) {
			mReaderOrds = readerOrds;
			mTermsIndex = termsIndex;
			mDocBase = docBase;
		}

		@Override
		public int compareBottom(int doc) {
			assert mBottomSlot != -1;
			
			final int docOrd = (mReaderOrds[doc]&0xFF);
			if (mBottomSameReader) {
				// ord is precisely comparable, even in the equal case
				return mBottomOrd - docOrd;
			} else if (mBottomOrd >= docOrd) {
				// the equals case always means bottom is > doc
				// (because we set bottomOrd to the lower bound in
				// setBottom):
				return 1;
			} else {
				return -1;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			final int ord = mReaderOrds[doc]&0xFF;
			mOrds[slot] = ord;
			if (ord == 0) {
				mValues[slot] = null;
			} else {
				assert ord > 0;
				if (mValues[slot] == null) 
					mValues[slot] = new BytesRef();
				mTermsIndex.lookup(ord, mValues[slot]);
			}
			mReaderGen[slot] = mCurrentReaderGen;
		}
	}

	// Used per-segment when bit width of doc->ord is 16:
	private final class ShortOrdComparator extends PerSegmentComparator {
		
		private final short[] mReaderOrds;
		private final IDocTermsIndex mTermsIndex;
		private final int mDocBase;

		public ShortOrdComparator(short[] readerOrds, IDocTermsIndex termsIndex, int docBase) {
			mReaderOrds = readerOrds;
			mTermsIndex = termsIndex;
			mDocBase = docBase;
		}

		@Override
		public int compareBottom(int doc) {
			assert mBottomSlot != -1;
			
			final int docOrd = (mReaderOrds[doc]&0xFFFF);
			if (mBottomSameReader) {
				// ord is precisely comparable, even in the equal case
				return mBottomOrd - docOrd;
			} else if (mBottomOrd >= docOrd) {
				// the equals case always means bottom is > doc
				// (because we set bottomOrd to the lower bound in
				// setBottom):
				return 1;
			} else {
				return -1;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			final int ord = mReaderOrds[doc]&0xFFFF;
			mOrds[slot] = ord;
			if (ord == 0) {
				mValues[slot] = null;
			} else {
				assert ord > 0;
				if (mValues[slot] == null) 
					mValues[slot] = new BytesRef();
				mTermsIndex.lookup(ord, mValues[slot]);
			}
			mReaderGen[slot] = mCurrentReaderGen;
		}
	}

	// Used per-segment when bit width of doc->ord is 32:
	private final class IntOrdComparator extends PerSegmentComparator {
		
		private final int[] mReaderOrds;
		private final IDocTermsIndex mTermsIndex;
		private final int mDocBase;

		public IntOrdComparator(int[] readerOrds, IDocTermsIndex termsIndex, int docBase) {
			mReaderOrds = readerOrds;
			mTermsIndex = termsIndex;
			mDocBase = docBase;
		}

		@Override
		public int compareBottom(int doc) {
			assert mBottomSlot != -1;
			
			final int docOrd = mReaderOrds[doc];
			if (mBottomSameReader) {
				// ord is precisely comparable, even in the equal case
				return mBottomOrd - docOrd;
			} else if (mBottomOrd >= docOrd) {
				// the equals case always means bottom is > doc
				// (because we set bottomOrd to the lower bound in
				// setBottom):
				return 1;
			} else {
				return -1;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			final int ord = mReaderOrds[doc];
			mOrds[slot] = ord;
			if (ord == 0) {
				mValues[slot] = null;
			} else {
				assert ord > 0;
				if (mValues[slot] == null) 
					mValues[slot] = new BytesRef();
				mTermsIndex.lookup(ord, mValues[slot]);
			}
			mReaderGen[slot] = mCurrentReaderGen;
		}
	}

	// Used per-segment when bit width is not a native array
	// size (8, 16, 32):
	private final class AnyOrdComparator extends PerSegmentComparator {
		
		private final IIntsReader mReaderOrds;
		private final IDocTermsIndex mTermsIndex;
		private final int mDocBase;

		public AnyOrdComparator(IIntsReader readerOrds, IDocTermsIndex termsIndex, int docBase) {
			mReaderOrds = readerOrds;
			mTermsIndex = termsIndex;
			mDocBase = docBase;
		}

		@Override
		public int compareBottom(int doc) {
			assert mBottomSlot != -1;
			
			final int docOrd = (int) mReaderOrds.get(doc);
			if (mBottomSameReader) {
				// ord is precisely comparable, even in the equal case
				return mBottomOrd - docOrd;
			} else if (mBottomOrd >= docOrd) {
				// the equals case always means bottom is > doc
				// (because we set bottomOrd to the lower bound in
				// setBottom):
				return 1;
			} else {
				return -1;
			}
		}

		@Override
		public void copy(int slot, int doc) {
			final int ord = (int) mReaderOrds.get(doc);
			mOrds[slot] = ord;
			if (ord == 0) {
				mValues[slot] = null;
			} else {
				assert ord > 0;
				if (mValues[slot] == null) 
					mValues[slot] = new BytesRef();
				mTermsIndex.lookup(ord, mValues[slot]);
			}
			mReaderGen[slot] = mCurrentReaderGen;
		}
	}

	@Override
	public IFieldComparator<BytesRef> setNextReader(IAtomicReaderRef context) throws IOException {
		final int docBase = context.getDocBase();
		mTermsIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mField);
		
		final IIntsReader docToOrd = mTermsIndex.getDocToOrd();
		FieldComparator<BytesRef> perSegComp = null;
		
		if (docToOrd.hasArray()) {
			final Object arr = docToOrd.getArray();
			if (arr instanceof byte[]) {
				perSegComp = new ByteOrdComparator((byte[]) arr, mTermsIndex, docBase);
			} else if (arr instanceof short[]) {
				perSegComp = new ShortOrdComparator((short[]) arr, mTermsIndex, docBase);
			} else if (arr instanceof int[]) {
				perSegComp = new IntOrdComparator((int[]) arr, mTermsIndex, docBase);
			}
			// Don't specialize the long[] case since it's not
			// possible, ie, worse case is MAX_INT-1 docs with
			// every one having a unique value.
		}
		if (perSegComp == null) 
			perSegComp = new AnyOrdComparator(docToOrd, mTermsIndex, docBase);

		mCurrentReaderGen ++;
		if (mBottomSlot != -1) 
			perSegComp.setBottom(mBottomSlot);

		return perSegComp;
	}
  
	@Override
	public void setBottom(final int bottom) {
		mBottomSlot = bottom;
		mBottomValue = mValues[mBottomSlot];
		
		if (mCurrentReaderGen == mReaderGen[mBottomSlot]) {
			mBottomOrd = mOrds[mBottomSlot];
			mBottomSameReader = true;
			
		} else {
			if (mBottomValue == null) {
				// 0 ord is null for all segments
				assert mOrds[mBottomSlot] == 0;
				mBottomOrd = 0;
				mBottomSameReader = true;
				mReaderGen[mBottomSlot] = mCurrentReaderGen;
				
			} else {
				final int index = binarySearch(mTempBR, mTermsIndex, mBottomValue);
				if (index < 0) {
					mBottomOrd = -index - 2;
					mBottomSameReader = false;
				} else {
					mBottomOrd = index;
					// exact value match
					mBottomSameReader = true;
					mReaderGen[mBottomSlot] = mCurrentReaderGen;            
					mOrds[mBottomSlot] = mBottomOrd;
				}
			}
		}
	}

	@Override
	public BytesRef getValue(int slot) {
		return mValues[slot];
	}
	
	static int binarySearch(BytesRef br, IDocTermsIndex a, BytesRef key) {
		return binarySearch(br, a, key, 1, a.getNumOrd()-1);
	}

	static int binarySearch(BytesRef br, IDocTermsIndex a, BytesRef key, int low, int high) {
		while (low <= high) {
			final int mid = (low + high) >>> 1;
			final BytesRef midVal = a.lookup(mid, br);
			final int cmp;
			if (midVal != null) 
				cmp = midVal.compareTo(key);
			else 
				cmp = -1;

			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid;
		}
		return -(low + 1);
	}
	
}
