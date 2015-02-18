package org.javenstudio.falcon.search;

import java.util.List;

import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.search.cache.SearchCacheConfig;
import org.javenstudio.falcon.search.update.UpdateIndexerConfig;

public interface ISearchConfig {

	public ContextLoader getContextLoader();
	
	public List<PluginInfo> getPluginInfos(String type);
	public PluginInfo getPluginInfo(String type);
	
	public String getIndexLockType();
	public UpdateIndexerConfig getIndexerConfig();;
	
	public int getMaxWarmingSearchers();
	public int getQueryResultWindowSize();
	public int getQueryResultMaxDocsCached();
	
	public boolean useFilterForSortedQuery();
	public boolean useColdSearcher();
	
	public boolean isEnableLazyFieldLoading();
	public boolean isReopenReaders();
	public boolean isUnlockOnStartup();
	
	public SearchCacheConfig getFilterCacheConfig();
	public SearchCacheConfig getQueryResultCacheConfig();
	public SearchCacheConfig getDocumentCacheConfig();
	public SearchCacheConfig getFieldValueCacheConfig();
	public SearchCacheConfig[] getUserCacheConfigs();
	
}
