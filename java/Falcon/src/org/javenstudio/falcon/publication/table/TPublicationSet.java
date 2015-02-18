package org.javenstudio.falcon.publication.table;

import org.javenstudio.falcon.publication.IPublication;
import org.javenstudio.falcon.publication.IPublicationSet;

final class TPublicationSet implements IPublicationSet {

	private final TPublication[] mPublications;
	private final int mTotalCount;
	private final int mStart;
	private int mIndex = 0;
	
	public TPublicationSet(TPublication[] publishes, 
			int totalCount, int start) {
		mPublications = publishes;
		mTotalCount = totalCount;
		mStart = start;
	}
	
	@Override
	public int getTotalCount() {
		return mTotalCount;
	}
	
	@Override
	public int getStart() {
		return mStart;
	}
	
	@Override
	public IPublication[] getPublications() {
		return mPublications;
	}
	
	@Override
	public synchronized void first() {
		mIndex = 0;
	}
	
	@Override
	public synchronized IPublication next() {
		if (mPublications != null && mPublications.length > 0) {
			if (mIndex >= 0 && mIndex < mPublications.length) 
				return mPublications[mIndex++];
		}
		return null;
	}
	
}
