package org.javenstudio.lightning.core.search;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.lightning.core.CoreAdminConfig;
import org.javenstudio.lightning.core.CoreConfig;
import org.javenstudio.falcon.search.ISearchConfig;
import org.javenstudio.falcon.search.cache.CacheRegenerator;
import org.javenstudio.falcon.search.cache.FastLRUCache;
import org.javenstudio.falcon.search.cache.SearchCache;
import org.javenstudio.falcon.search.cache.SearchCacheConfig;
import org.javenstudio.falcon.search.update.DefaultUpdateIndexer;
import org.javenstudio.falcon.search.update.UpdateIndexerConfig;

public class SearchConfig extends CoreConfig implements ISearchConfig {

	private final UpdateIndexerConfig mIndexerConfig;
	
	private final int mMaxWarmingSearchers;
	private final int mQueryResultWindowSize;
	private final int mQueryResultMaxDocsCached;
	
	private final boolean mReopenReaders;
	private final boolean mUnlockOnStartup;
	private final boolean mUseColdSearcher;
	
	private final boolean mEnableLazyFieldLoading;
	private final boolean mUseFilterForSortedQuery;
	
	private final SearchCacheConfig mFilterCacheConfig;
	private final SearchCacheConfig mQueryResultCacheConfig;
	private final SearchCacheConfig mDocumentCacheConfig;
	private final SearchCacheConfig mFieldValueCacheConfig;
	private final SearchCacheConfig[] mUserCacheConfigs;
	
	public SearchConfig(CoreAdminConfig config, ContextLoader loader, 
			String name, InputStream is) throws ErrorException {
		super(config, loader, name, is);
		
		mIndexerConfig = loadUpdateIndexerConfig(this);
		mMaxWarmingSearchers = getInt("query/maxWarmingSearchers", Integer.MAX_VALUE);
		mReopenReaders = getBool("indexConfig/reopenReaders", true);
		mUnlockOnStartup = getBool("indexConfig/unlockOnStartup", false);
	    mUseColdSearcher = getBool("query/useColdSearcher", false);
	    
	    mEnableLazyFieldLoading = getBool("query/enableLazyFieldLoading", false);
	    mUseFilterForSortedQuery = getBool("query/useFilterForSortedQuery", false);
	    mQueryResultWindowSize = Math.max(1, getInt("query/queryResultWindowSize", 1));
	    mQueryResultMaxDocsCached = getInt("query/queryResultMaxDocsCached", Integer.MAX_VALUE);
	    
	    mFilterCacheConfig = getSearchCacheConfig(this, "query/filterCache");
	    mQueryResultCacheConfig = getSearchCacheConfig(this, "query/queryResultCache");
	    mDocumentCacheConfig = getSearchCacheConfig(this, "query/documentCache");
	    mUserCacheConfigs = getMultipleSearchCacheConfigs(this, "query/cache");
	    
	    SearchCacheConfig conf = getSearchCacheConfig(this, "query/fieldValueCache");
	    if (conf == null) {
	    	Map<String,String> args = new HashMap<String,String>();
	    	args.put("name", "fieldValueCache");
	    	args.put("size", "10000");
	    	args.put("initialSize", "10");
	    	args.put("showItems", "-1");
	    	conf = new SearchCacheConfig(FastLRUCache.class, args, null);
	    }
	    mFieldValueCacheConfig = conf;
	    
	}
	
	public final UpdateIndexerConfig getIndexerConfig() { 
		return mIndexerConfig; 
	}
	
	public final String getIndexLockType() { return null; }
	
	public final int getMaxWarmingSearchers() { return mMaxWarmingSearchers; }
	public final int getQueryResultWindowSize() { return mQueryResultWindowSize; }
	public final int getQueryResultMaxDocsCached() { return mQueryResultMaxDocsCached; }
	
	public final boolean useFilterForSortedQuery() { return mUseFilterForSortedQuery; }
	public final boolean isEnableLazyFieldLoading() { return mEnableLazyFieldLoading; }
	
	public final boolean isReopenReaders() { return mReopenReaders; }
	public final boolean isUnlockOnStartup() { return mUnlockOnStartup; }
	public final boolean useColdSearcher() { return mUseColdSearcher; }
	
	public final SearchCacheConfig getFilterCacheConfig() { return mFilterCacheConfig; }
	public final SearchCacheConfig getQueryResultCacheConfig() { return mQueryResultCacheConfig; }
	public final SearchCacheConfig getDocumentCacheConfig() { return mDocumentCacheConfig; }
	public final SearchCacheConfig getFieldValueCacheConfig() { return mFieldValueCacheConfig; }
	public final SearchCacheConfig[] getUserCacheConfigs() { return mUserCacheConfigs; }
	
	private static UpdateIndexerConfig loadUpdateIndexerConfig(ContextResource conf) 
			throws ErrorException { 
    	return new UpdateIndexerConfig(
    			conf.get("updateIndexer/@class", DefaultUpdateIndexer.class.getName()), 
    			conf.getInt("updateIndexer/autoCommit/maxDocs", -1), 
    			conf.getInt("updateIndexer/autoCommit/maxTime", -1), 
    			conf.getInt("updateIndexer/autoSoftCommit/maxDocs", -1), 
    			conf.getInt("updateIndexer/autoSoftCommit/maxTime", -1), 
    			conf.getInt("updateIndexer/commitIntervalLowerBound", -1), 
    			conf.getBool("updateIndexer/autoCommit/openSearcher", true));
    }
	
	private static SearchCacheConfig[] getMultipleSearchCacheConfigs(ContextResource config, 
			String configPath) throws ErrorException {
		ContextList nodes = config.getNodes(configPath);
		if (nodes == null || nodes.getLength() == 0) 
			return null;
		
		SearchCacheConfig[] configs = new SearchCacheConfig[nodes.getLength()];
		for (int i=0; i < nodes.getLength(); i++) {
			configs[i] = getSearchCacheConfig(config, nodes.getNodeAt(i));
		}
		
		return configs;
	}

	private static SearchCacheConfig getSearchCacheConfig(ContextResource config, 
			String xpath) throws ErrorException {
		ContextNode node = config.getNode(xpath);
		return getSearchCacheConfig(config, node);
	}

	@SuppressWarnings("unchecked")
	private static SearchCacheConfig getSearchCacheConfig(ContextResource config, 
			ContextNode node) throws ErrorException {
		if (node == null) return null;
		
		SearchCacheConfig conf = new SearchCacheConfig();
		conf.setNodeName(node.getNodeName());
		conf.setCacheArgs(node.getAttributes());
		
		String nameAttr = conf.getCacheArgs().get("name");  // OPTIONAL
		if (nameAttr == null) 
			conf.getCacheArgs().put("name", conf.getNodeName());

    	ContextLoader loader = config.getContextLoader();
    	
    	conf.setCacheImpl(conf.getCacheArgs().get("class"));
    	conf.setRegenImpl(conf.getCacheArgs().get("regenerator"));
    	
    	try {
	    	conf.setCacheClass((Class<SearchCache<?,?>>) 
	    			loader.findClass(conf.getCacheImpl(), SearchCache.class));
	    	
	    	if (conf.getRegenImpl() != null) {
	    		conf.setRegenerator(loader.newInstance(
	    				conf.getRegenImpl(), CacheRegenerator.class));
	    	}
    	} catch (ClassNotFoundException ex) { 
    		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
    	}
    
    	return conf;
	}
	
}
