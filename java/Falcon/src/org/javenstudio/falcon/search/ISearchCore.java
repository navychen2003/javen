package org.javenstudio.falcon.search;

import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.PluginHolder;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.falcon.search.component.SearchComponent;
import org.javenstudio.falcon.search.query.QueryBuilderFactory;
import org.javenstudio.falcon.search.query.ValueSourceFactory;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.shard.ShardHandlerFactory;
import org.javenstudio.falcon.search.store.DirectoryFactory;
import org.javenstudio.falcon.search.transformer.TransformerFactory;
import org.javenstudio.falcon.search.update.UpdateIndexer;
import org.javenstudio.falcon.search.update.UpdateProcessorChain;

public interface ISearchCore extends PluginHolder {

	public String getDataDir();
	public String getIndexDir();
	public String getNewIndexDir();
	public String getName();
	
	public ISearchConfig getSearchConfig();
	public SearchControl getSearchControl();
	public UpdateIndexer getUpdateIndexer();
	
	public DirectoryFactory getDirectoryFactory();
	public QueryBuilderFactory getQueryFactory();
	public TransformerFactory getTransformerFactory(String name);
	public ValueSourceFactory getValueSourceFactory();
	
	public ShardHandlerFactory getShardHandlerFactory() 
			throws ErrorException;
	
	public IIndexContext getIndexContext();
	public IIndexFormat getIndexFormat();
	public IndexSchema getSchema();
	
	public IndexParams createIndexParams() throws ErrorException;
	public boolean hasResponseWriter(ISearchRequest req) 
			throws ErrorException;
	
	public void close();
	
	public ISearchResponse createLocalResponse(Searcher searcher, 
			ISearchRequest request) throws ErrorException;
	
	public ISearchRequest createLocalRequest(Searcher searcher, 
			Params params) throws ErrorException;
	
	public SearchComponent getSearchComponent(String name) 
			throws ErrorException;
	
	public UpdateProcessorChain getUpdateProcessingChain(final String name) 
			throws ErrorException;
	
	public void registerInfoMBean(Object instance) 
			throws ErrorException;
	
	public void removeInfoMBean(Object instance) 
			throws ErrorException;
	
	public void addCloseHook(SearchCloseHook hook);
	
	public ContextLoader getContextLoader();
	
	public <T> T createPlugin(PluginInfo info, Class<T> cast) 
			throws ErrorException;
	
	public <T> T initPlugins(Map<String, T> registry, Class<T> type) 
			throws ErrorException;
	
	public <T> List<T> initPlugins(List<PluginInfo> pluginInfos, Class<T> type) 
			throws ErrorException;
	
	public <T> T newInstance(String cname, Class<T> expectedType) 
			throws ErrorException;
	
}
