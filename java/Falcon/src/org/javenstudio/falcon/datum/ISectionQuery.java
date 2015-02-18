package org.javenstudio.falcon.datum;

public interface ISectionQuery {

	public static interface Filter {
		public boolean acceptSection(ISection section);
	}
	
	public String getQuery();
	public String getParam(String name);
	public String getSortParam();
	public Filter getFilter();
	
	public long getResultStart();
	public int getResultCount();
	
	public ISectionCollector getCollector();
	public boolean isByFolder();
	
}
