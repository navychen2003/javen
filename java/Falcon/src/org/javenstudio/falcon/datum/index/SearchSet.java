package org.javenstudio.falcon.datum.index;

import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionSet;

final class SearchSet implements ISectionSet {

	private final SearchGroup[] mGroups;
	private final SearchSort[] mSorts;
	private final ISection[] mSections;
	private final String mSort;
	private final int mTotalCount;
	private final int mStart;
	
	public SearchSet(ISection[] sections, int start, int totalCount) { 
		this(new SearchGroup[] { new SearchGroup("all", "All") }, 
			 new SearchSort[] {}, sections, (String)null, 
			 start, totalCount);
	}
	
	public SearchSet(SearchGroup[] groups, SearchSort[] sorts, 
			ISection[] sections, String sort, int start, int totalCount) { 
		mGroups = groups;
		mSorts = sorts;
		mSort = sort;
		mSections = sections;
		mTotalCount = totalCount;
		mStart = start;
	}
	
	@Override
	public int getTotalCount() { 
		return mTotalCount; 
	}
	
	@Override
	public int getSectionStart() { 
		return mStart; 
	}
	
	@Override
	public int getSectionCount() { 
		return mSections != null ? mSections.length : 0;
	}
	
	@Override
	public ISection getSectionAt(int index) { 
		if (index >= 0 && mSections != null && index < mSections.length) 
			return mSections[index];
		
		return null;
	}
	
	@Override
	public SearchGroup[] getGroups() {
		return mGroups;
	}

	@Override
	public SearchSort[] getSorts() {
		return mSorts;
	}
	
	@Override
	public String getSortName() { 
		return mSort;
	}
	
}
