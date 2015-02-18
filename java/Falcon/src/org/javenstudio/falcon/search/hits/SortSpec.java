package org.javenstudio.falcon.search.hits;

import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.search.SortField;

/***
 * SortSpec encapsulates a Sort and a count of the number of documents
 * to return.
 */
public class SortSpec {
	
	public static boolean includesScore(ISort sort) {
		if (sort == null) 
			return true;
		
		for (ISortField sf : sort.getSortFields()) {
			if (sf.getType() == SortField.Type.SCORE) 
				return true;
		}
		
		return false;
	}
	
	private ISort mSort;
	private int mNum;
	private int mOffset;

	public SortSpec(ISort sort, int num) {
		this(sort, 0, num);
	}

	public SortSpec(ISort sort, int offset, int num) {
		mSort = sort;
		mOffset = offset;
		mNum = num;
	}
  
	public void setSort(ISort s) {
		mSort = s;
	}

	public boolean includesScore() {
		return includesScore(mSort);
	}

	/**
	 * Gets the Sort object, or null for the default sort
	 * by score descending.
	 */
	public ISort getSort() { 
		return mSort; 
	}

	/**
	 * Offset into the list of results.
	 */
	public int getOffset() { 
		return mOffset; 
	}

	/**
	 * Gets the number of documents to return after sorting.
	 *
	 * @return number of docs to return, or -1 for no cut off (just sort)
	 */
	public int getCount() { 
		return mNum; 
	}

	@Override
	public String toString() {
		return "SortSpec{start=" + mOffset+ ",rows=" + mNum 
				+ (mSort == null ? "" : ",sort=" + mSort) + "}"; 
	}
	
}
