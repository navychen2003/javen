package org.javenstudio.falcon.search.query;

import org.javenstudio.common.indexdb.search.Query;

public class ExtendedQueryBase extends Query implements ExtendedQuery {
	
	protected int mCost;
	protected boolean mCache = true;
	protected boolean mCacheSep;

	@Override
	public void setCache(boolean cache) {
		mCache = cache;
	}

	@Override
	public boolean getCache() {
		return mCache;
	}

	@Override
	public void setCacheSep(boolean cacheSep) {
		mCacheSep = cacheSep;
	}

	@Override
	public boolean getCacheSep() {
		return mCacheSep;
	}

	@Override
	public void setCost(int cost) {
		mCost = cost;
	}

	public int getCost() {
		return mCost;
	}

	public String getOptions() {
		StringBuilder sb = new StringBuilder();
		if (!mCache) {
			sb.append("{!cache=false");
			sb.append(" cost=");
			sb.append(mCost);
			sb.append("}");
		} else if (mCacheSep) {
			sb.append("{!cache=sep");
			sb.append("}");
		}
		return sb.toString();
	}

	@Override
	public String toString(String field) {
		return getOptions();
	}
	
}
