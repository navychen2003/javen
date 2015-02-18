package org.javenstudio.hornet.search.query;

import java.util.Comparator;
import java.util.LinkedList;

import org.javenstudio.common.indexdb.index.term.TermsEnum;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.NumericType;
import org.javenstudio.common.indexdb.util.NumericUtil;

/**
 * Subclass of FilteredTermsEnum for enumerating all terms that match the
 * sub-ranges for trie range queries, using flex API.
 * <p>
 * WARNING: This term enumeration is not guaranteed to be always ordered by
 * {@link Term#compareTo}.
 * The ordering depends on how {@link NumericUtil#splitLongRange} and
 * {@link NumericUtil#splitIntRange} generates the sub-ranges. For
 * {@link MultiTermQuery} ordering is not relevant.
 */
public class NumericRangeTermsEnum<T extends Number> extends FilteredTermsEnum {

	private final NumericRangeQuery<T> mQuery;
	private final LinkedList<BytesRef> mRangeBounds = new LinkedList<BytesRef>();
	private final Comparator<BytesRef> mTermComp;
	private BytesRef mCurrentLowerBound, mCurrentUpperBound;

	public NumericRangeTermsEnum(final NumericRangeQuery<T> query, final TermsEnum tenum) {
		super(tenum);
		mQuery = query;
		
		switch (mQuery.getDataType()) {
		case LONG:
		case DOUBLE: {
			// lower
			long minBound;
			if (mQuery.getDataType() == NumericType.LONG) {
				minBound = (mQuery.getMin() == null) ? Long.MIN_VALUE : mQuery.getMin().longValue();
				
			} else {
				assert mQuery.getDataType() == NumericType.DOUBLE;
				minBound = (mQuery.getMin() == null) ? NumericRangeQuery.LONG_NEGATIVE_INFINITY
						: NumericUtil.doubleToSortableLong(mQuery.getMin().doubleValue());
			}
			
			if (!mQuery.includesMin() && mQuery.getMin() != null) {
				if (minBound == Long.MAX_VALUE) break;
				minBound++;
			}
      
			// upper
			long maxBound;
			if (mQuery.getDataType() == NumericType.LONG) {
				maxBound = (mQuery.getMax() == null) ? Long.MAX_VALUE : mQuery.getMax().longValue();
				
			} else {
				assert mQuery.getDataType() == NumericType.DOUBLE;
				maxBound = (mQuery.getMax() == null) ? NumericRangeQuery.LONG_POSITIVE_INFINITY
						: NumericUtil.doubleToSortableLong(mQuery.getMax().doubleValue());
			}
			
			if (!mQuery.includesMax() && mQuery.getMax() != null) {
				if (maxBound == Long.MIN_VALUE) break;
				maxBound--;
			}
      
			NumericUtil.splitLongRange(new NumericUtil.LongRangeBuilder() {
					@Override
					public final void addRange(BytesRef minPrefixCoded, BytesRef maxPrefixCoded) {
						mRangeBounds.add(minPrefixCoded);
						mRangeBounds.add(maxPrefixCoded);
					}
				}, mQuery.getPrecisionStep(), minBound, maxBound);
			
			break;
		}
      
		case INT:
		case FLOAT: {
			// lower
			int minBound;
			if (mQuery.getDataType() == NumericType.INT) {
				minBound = (mQuery.getMin() == null) ? Integer.MIN_VALUE : mQuery.getMin().intValue();
				
			} else {
				assert mQuery.getDataType() == NumericType.FLOAT;
				minBound = (mQuery.getMin() == null) ? NumericRangeQuery.INT_NEGATIVE_INFINITY
						: NumericUtil.floatToSortableInt(mQuery.getMin().floatValue());
			}
			
			if (!mQuery.includesMin() && mQuery.getMin() != null) {
				if (minBound == Integer.MAX_VALUE) break;
				minBound++;
			}
      
			// upper
			int maxBound;
			if (mQuery.getDataType() == NumericType.INT) {
				maxBound = (mQuery.getMax() == null) ? Integer.MAX_VALUE : mQuery.getMax().intValue();
				
			} else {
				assert mQuery.getDataType() == NumericType.FLOAT;
				maxBound = (mQuery.getMax() == null) ? NumericRangeQuery.INT_POSITIVE_INFINITY
						: NumericUtil.floatToSortableInt(mQuery.getMax().floatValue());
			}
			
			if (!mQuery.includesMax() && mQuery.getMax() != null) {
				if (maxBound == Integer.MIN_VALUE) break;
				maxBound--;
			}
      
			NumericUtil.splitIntRange(new NumericUtil.IntRangeBuilder() {
					@Override
					public final void addRange(BytesRef minPrefixCoded, BytesRef maxPrefixCoded) {
						mRangeBounds.add(minPrefixCoded);
						mRangeBounds.add(maxPrefixCoded);
					}
				}, mQuery.getPrecisionStep(), minBound, maxBound);
			
			break;
		}
      
		default:
			// should never happen
			throw new IllegalArgumentException("Invalid NumericType");
		}

		mTermComp = getComparator();
	}

	private void nextRange() {
		assert mRangeBounds.size() % 2 == 0;

		mCurrentLowerBound = mRangeBounds.removeFirst();
		assert mCurrentUpperBound == null || mTermComp.compare(mCurrentUpperBound, mCurrentLowerBound) <= 0 :
			"The current upper bound must be <= the new lower bound";
  
		mCurrentUpperBound = mRangeBounds.removeFirst();
	}

	@Override
	protected final BytesRef nextSeekTerm(BytesRef term) {
		while (mRangeBounds.size() >= 2) {
			nextRange();
    
			// if the new upper bound is before the term parameter, the sub-range is never a hit
			if (term != null && mTermComp.compare(term, mCurrentUpperBound) > 0)
				continue;
			
			// never seek backwards, so use current term if lower bound is smaller
			return (term != null && mTermComp.compare(term, mCurrentLowerBound) > 0) ?
					term : mCurrentLowerBound;
		}
  
		// no more sub-range enums available
		assert mRangeBounds.isEmpty();
		mCurrentLowerBound = mCurrentUpperBound = null;
		
		return null;
	}

	@Override
	protected final AcceptStatus accept(BytesRef term) {
		while (mCurrentUpperBound == null || mTermComp.compare(term, mCurrentUpperBound) > 0) {
			if (mRangeBounds.isEmpty())
				return AcceptStatus.END;
			
			// peek next sub-range, only seek if the current term is smaller than next lower bound
			if (mTermComp.compare(term, mRangeBounds.getFirst()) < 0)
				return AcceptStatus.NO_AND_SEEK;
			
			// step forward to next range without seeking, as next lower range bound 
			// is less or equal current term
			nextRange();
		}
		
		return AcceptStatus.YES;
	}
	
}
