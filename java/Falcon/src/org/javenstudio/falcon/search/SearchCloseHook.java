package org.javenstudio.falcon.search;

public interface SearchCloseHook {

	public void preClose(ISearchCore core);
	public void postClose(ISearchCore core);
	
}
