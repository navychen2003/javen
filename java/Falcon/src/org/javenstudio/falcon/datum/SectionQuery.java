package org.javenstudio.falcon.datum;

public class SectionQuery implements ISectionQuery {

	private final long mResultStart;
	private final int mResultCount;
	
	public SectionQuery(long start, int count) { 
		mResultStart = start;
		mResultCount = count;
	}
	
	@Override
	public long getResultStart() {
		return mResultStart;
	}

	@Override
	public int getResultCount() {
		return mResultCount;
	}

	@Override
	public String getQuery() { 
		return getParam("q");
	}
	
	@Override
	public String getSortParam() { 
		return getParam("sort");
	}
	
	@Override
	public Filter getFilter() {
		return null; //getParam("filtertype");
	}
	
	@Override
	public String getParam(String name) {
		return null;
	}
	
	@Override
	public ISectionCollector getCollector() { 
		return null;
	}
	
	@Override
	public boolean isByFolder() { 
		return true;
	}
	
}
