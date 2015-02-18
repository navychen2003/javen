package org.javenstudio.falcon.datum.data;

import org.javenstudio.falcon.datum.ISectionSet;

final class SqSectionSet implements ISectionSet {

	private final SqGroup[] mGroups;
	private final SqSort[] mSorts;
	private final SqSection[] mSections;
	private final String mSort;
	private final int mTotalCount;
	private final int mOffset;
	private final int mStart;
	private final int mCount;
	
	SqSectionSet(SqGroup[] groups, SqSort[] sorts, 
			SqSection[] sections, String sort, int start, int count, 
			int totalCount, int offset) { 
		mGroups = groups;
		mSorts = sorts;
		mSort = sort;
		mSections = sections;
		mTotalCount = totalCount;
		mOffset = offset;
		mCount = count;
		mStart = start;
	}
	
	@Override
	public int getTotalCount() { 
		return mTotalCount; 
	}
	
	@Override
	public int getSectionStart() { 
		return mOffset + mStart; 
	}
	
	@Override
	public int getSectionCount() { 
		return mCount;
	}
	
	@Override
	public SqSection getSectionAt(int index) { 
		if (index >= 0 && index < mCount && mSections != null) {
			int idx = index + mStart;
			if (idx >= 0 && idx < mSections.length)
				return mSections[idx];
		}
		
		return null;
	}
	
	@Override
	public SqGroup[] getGroups() {
		return mGroups;
	}

	@Override
	public SqSort[] getSorts() {
		return mSorts;
	}
	
	@Override
	public String getSortName() { 
		return mSort;
	}
	
}
