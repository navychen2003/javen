package org.javenstudio.falcon.search.hits;

import java.util.List;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;

/** 
 * A hash key encapsulating a query, a list of filters, and a sort
 *
 */
public final class QueryResultKey {
	
	private static ISortField[] sDefaultSort = new ISortField[0];
	
	private final IQuery mQuery;
	private final ISort mSort;
	private final ISortField[] mSortFields;
	private final List<IQuery> mFilters;
	
	// non-comparable flags... ignored by hashCode and equals
	@SuppressWarnings("unused")
	private final int mNonComparableFlags; 
	
	// cached hashCode
	private final int mCachedHashCode; 

	@SuppressWarnings("unused") 
	public QueryResultKey(IQuery query, List<IQuery> filters, 
			ISort sort, int nc_flags) {
		mQuery = query;
		mSort = sort;
		mFilters = filters;
		mNonComparableFlags = nc_flags;

		int h = query.hashCode();
		if (filters != null) {
			for (IQuery filt : mFilters) { 
				h += filters.hashCode();
			}
		}

		mSortFields = (mSort != null) ? 
				mSort.getSortFields() : sDefaultSort;
		
		for (ISortField sf : mSortFields) {
			h = h*29 + sf.hashCode();
		}

    	mCachedHashCode = h;
	}

	@Override
	public int hashCode() {
		return mCachedHashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof QueryResultKey)) 
			return false;
		
		QueryResultKey other = (QueryResultKey)o;

		// fast check of the whole hash code... most hash tables will only use
		// some of the bits, so if this is a hash collision, it's still likely
		// that the full cached hash code will be different.
		if (this.mCachedHashCode != other.mCachedHashCode) 
			return false;

		// check for the thing most likely to be different (and the fastest things)
		// first.
		if (this.mSortFields.length != other.mSortFields.length) 
			return false;
		
		if (!this.mQuery.equals(other.mQuery)) 
			return false;
		
		if (!isEqual(this.mFilters, other.mFilters)) 
			return false;

		for (int i=0; i < mSortFields.length; i++) {
			ISortField sf1 = this.mSortFields[i];
			ISortField sf2 = other.mSortFields[i];
			
			if (!isEqual(sf1, sf2)) 
				return false;
		}

		return true;
	}

	private static boolean isEqual(Object o1, Object o2) {
		if (o1 == o2) return true; // takes care of identity and null cases
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}
	
}
