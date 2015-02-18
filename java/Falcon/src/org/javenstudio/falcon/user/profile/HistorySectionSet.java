package org.javenstudio.falcon.user.profile;

public final class HistorySectionSet {

	private final HistorySection[] mSections;
	private final int mSectionStart;
	private final int mTotalCount;
	
	public HistorySectionSet(HistorySection[] sections, 
			int start, int totalcount) {
		mSections = sections;
		mSectionStart = start >= 0 ? start : 0;
		mTotalCount = totalcount >= 0 ? totalcount : 0;
	}
	
	public int getTotalCount() {
		return mTotalCount;
	}

	public int getSectionStart() {
		return mSectionStart;
	}

	public int getSectionCount() {
		return mSections != null ? mSections.length : 0;
	}

	public HistorySection getSectionAt(int index) {
		if (mSections != null && index >= 0 && index < mSections.length)
			return mSections[index];
		return null;
	}

}
