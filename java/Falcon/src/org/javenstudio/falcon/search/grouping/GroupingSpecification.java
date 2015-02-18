package org.javenstudio.falcon.search.grouping;

import org.javenstudio.common.indexdb.ISort;

/**
 * Encapsulates the grouping options like fields group sort 
 * and more specified by clients.
 *
 */
public class GroupingSpecification {

	private String[] mFields = new String[]{};
	private String[] mQueries = new String[]{};
	private String[] mFunctions = new String[]{};
	
	private int mOffset;
	private int mLimit;
	private int mGroupOffset;
	private int mGroupLimit;
	
	private ISort mGroupSort;
	private ISort mSortWithinGroup;
	
	private boolean mIncludeGroupCount;
	private boolean mIsMain;
	
	private Grouping.Format mResponseFormat;
	
	private boolean mNeedScore;
	private boolean mTruncateGroups;

	public String[] getFields() { return mFields; }

	public void setFields(String[] fields) {
		if (fields == null) 
			return;

		mFields = fields;
	}

	public String[] getQueries() { return mQueries; }

	public void setQueries(String[] queries) {
		if (queries == null) 
			return;

		mQueries = queries;
	}

	public String[] getFunctions() { return mFunctions; }

	public void setFunctions(String[] functions) {
		if (functions == null) 
			return;

		mFunctions = functions;
	}

	public int getGroupOffset() { return mGroupOffset; }
	public void setGroupOffset(int groupOffset) { mGroupOffset = groupOffset; }

	public int getGroupLimit() { return mGroupLimit; }
	public void setGroupLimit(int groupLimit) { mGroupLimit = groupLimit; }

	public int getOffset() { return mOffset; }
	public void setOffset(int offset) { mOffset = offset; }

	public int getLimit() { return mLimit; }
	public void setLimit(int limit) { mLimit = limit; }

	public ISort getGroupSort() { return mGroupSort; }
	public void setGroupSort(ISort groupSort) { mGroupSort = groupSort; }

	public ISort getSortWithinGroup() { return mSortWithinGroup; }
	public void setSortWithinGroup(ISort s) { mSortWithinGroup = s; }

	public boolean isIncludeGroupCount() { return mIncludeGroupCount; }
	public void setIncludeGroupCount(boolean count) { mIncludeGroupCount = count; }

	public boolean isMain() { return mIsMain; }
	public void setIsMain(boolean main) { mIsMain = main; }

	public Grouping.Format getResponseFormat() { return mResponseFormat; }
	public void setResponseFormat(Grouping.Format f) { mResponseFormat = f; }

	public boolean isNeedScore() { return mNeedScore; }
	public void setNeedScore(boolean needScore) { mNeedScore = needScore; }

	public boolean isTruncateGroups() { return mTruncateGroups; }
	public void setTruncateGroups(boolean truncate) { mTruncateGroups = truncate; }
	
}
