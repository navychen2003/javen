package org.javenstudio.lightning.core.search;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IDirectoryReader;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.index.IndexContext;
import org.javenstudio.common.indexdb.index.IndexParams;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.AdminParams;
import org.javenstudio.falcon.util.FileUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.NumberUtils;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.PluginFactory;
import org.javenstudio.falcon.util.PluginFactoryHolder;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.lightning.context.ConfigPluginInfo;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreCloseHook;
import org.javenstudio.lightning.core.CoreContainer;
import org.javenstudio.lightning.core.CoreFactory;
import org.javenstudio.lightning.request.LocalRequestInput;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseWriter;
import org.javenstudio.lightning.response.ResponseWriters;
import org.javenstudio.lightning.response.writer.JSONResponseWriter;
import org.javenstudio.lightning.response.writer.XMLResponseWriter;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.SearchHelper;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.SearcherRef;
import org.javenstudio.falcon.search.SearchCloseHook;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.SearchResolver;
import org.javenstudio.falcon.search.ISearchResponse;
import org.javenstudio.falcon.search.SearchControl;
import org.javenstudio.falcon.search.component.SearchComponent;
import org.javenstudio.falcon.search.component.SearchComponents;
import org.javenstudio.falcon.search.handler.LukeRequestHandler;
import org.javenstudio.falcon.search.query.QueryBuilderFactory;
import org.javenstudio.falcon.search.query.ValueSourceFactory;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.shard.ShardHandlerFactory;
import org.javenstudio.falcon.search.store.DirectoryFactory;
import org.javenstudio.falcon.search.transformer.TransformerFactory;
import org.javenstudio.falcon.search.update.UpdateIndexer;
import org.javenstudio.falcon.search.update.UpdateProcessorChain;

public class SearchCore extends Core implements ISearchCore {
	private static final Logger LOG = Logger.getLogger(SearchCore.class);
	
	private static final String SHARD_HANDLER_FACTORY = "search.ShardHandlerFactory";
	
	private final IndexSchema mIndexSchema;
	private final IndexContext mIndexContext;
	
	private final SearchControl mSearchControl;
	private final UpdateIndexer mUpdateIndexer;
	
	private final Map<String,SearchComponent> mSearchComponents;
	private final Map<String,UpdateProcessorChain> mUpdateChains;
	private final Map<String,TransformerFactory> mTransformerFactories;
	
	private final QueryBuilderFactory mQueryFactory;
	private final ValueSourceFactory mParserFactory;
	
	private String mLastNewIndexDir = null;
	
	public SearchCore(CoreFactory factory, String dataDir, 
			SearchConfig config, SearchDescriptor cd, IndexSchema schema) 
			throws ErrorException {
		this(factory, dataDir, config, cd, schema, null);
	}
	
	public SearchCore(CoreFactory factory, String dataDir, 
			SearchConfig config, SearchDescriptor cd, IndexSchema schema, 
			SearchCore prev) throws ErrorException {
		super(factory, dataDir, config, cd, prev);
		
		if (schema == null) {
			schema = new IndexSchema(config.getContextLoader(), 
					IndexSchema.DEFAULT_SCHEMA_FILE);
		}
		
		mIndexSchema = schema;
		mIndexContext = createIndexContext();
		mSearchComponents = loadSearchComponents();
		mTransformerFactories = loadTransformerFactories();
		mUpdateChains = loadUpdateProcessorChains();
		mQueryFactory = new QueryBuilderFactory(this);
		mParserFactory = new ValueSourceFactory(this);
		
		mSearchControl = new SearchControl(this, prev);
		mUpdateIndexer = loadUpdateIndexer();
		
		onInited();
	}
	
	@Override
	protected ResponseWriters loadResponseWriters() throws ErrorException { 
		ResponseWriters writers = super.loadResponseWriters();
		
		ResponseWriter xmlWriter = new SearchResponseWriter.XMLWriterImpl();
		ResponseWriter jsonWriter = new SearchResponseWriter.JSONWriterImpl();
		
		writers.registerWriter(XMLResponseWriter.TYPE, xmlWriter);
		writers.registerWriter(JSONResponseWriter.TYPE, jsonWriter);
		writers.registerWriter(ResponseWriters.STANDARD_TYPE, xmlWriter);
		
		//writers.registerWriter(VelocityResponseWriter.TYPE, new VelocityResponseWriter());
		
		return writers;
	}
	
	@Override
	public NamedList<Object> getParsedResponse(Request req, 
			Response rsp) throws ErrorException { 
		return SearchResolver.getParsedResponse(
				(ISearchRequest)req, (ISearchResponse)rsp);
	}
	
	@Override
	public final SearchConfig getSearchConfig() { 
		return (SearchConfig)getConfig();
	}
	
	public final SearchDescriptor getSearchDescriptor() { 
		return (SearchDescriptor)getDescriptor();
	}
	
	@Override
	public final IndexSchema getSchema() { 
		return mIndexSchema;
	}
	
	@Override
	public IndexParams createIndexParams() { 
		return SearchHelper.createParams(
				mIndexSchema.getAnalyzer(), mIndexContext);
	}
	
	@Override
	public final String getIndexDir() { 
		return getDataDir() + "index/";
	}
	
	@Override
	public DirectoryFactory getDirectoryFactory() { 
		return mSearchControl.getDirectoryFactory();
	}
	
	@Override
	public QueryBuilderFactory getQueryFactory() { 
		return mQueryFactory;
	}
	
	@Override
	public ValueSourceFactory getValueSourceFactory() { 
		return mParserFactory;
	}
	
	@Override
	public SearchControl getSearchControl() { 
		return mSearchControl;
	}
	
	public IndexContext createIndexContext() { 
		return SearchHelper.getIndexContext();
	}
	
	@Override
	public IIndexContext getIndexContext() { 
		return mIndexContext;
	}
	
	@Override
	public IIndexFormat getIndexFormat() { 
		return mIndexContext.getIndexFormat();
	}
	
	@Override
	public boolean hasResponseWriter(ISearchRequest req) 
			throws ErrorException { 
		return getResponseWriter((Request)req) != null;
	}
	
	@Override
	public ISearchRequest createLocalRequest(final Searcher searcher, 
			Params params) throws ErrorException { 
		if (searcher != null) { 
			return new SearchRequest(this, 
		    			new LocalRequestInput(""), params) { 
		    		public Searcher getSearcher() { return searcher; }
		    		public void close() {}
		    	};
		}
		
		return new SearchRequest(this, 
				new LocalRequestInput(""), params);
	}
	
	@Override
	public ISearchResponse createLocalResponse(Searcher searcher, 
			ISearchRequest request) throws ErrorException { 
		return new LocalResponse(this, (SearchRequest)request);
	}
	
	public final SearcherRef getSearcherRef() throws ErrorException { 
		return mSearchControl.getSearcherRef();
	}
	
	/**
	 * RequestHandlers need access to the updateHandler so they can all talk to the
	 * same RAM indexer.  
	 */
	public UpdateIndexer getUpdateIndexer() {
		return mUpdateIndexer;
	}
	
	/**
	 * @return a Search Component registered to a given name. 
	 * Throw an exception if the component is undefined
	 */
	@Override
	public SearchComponent getSearchComponent(String name) throws ErrorException {
		SearchComponent component = mSearchComponents.get(name);
		if (component == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"Unknown Search Component: " + name);
		}
		return component;
	}

	/**
	 * Accessor for all the Search Components
	 * @return An unmodifiable Map of Search Components
	 */
	//public Map<String, SearchComponent> getSearchComponents() {
	//	return mSearchComponents;
	//}
	
	@Override
	public TransformerFactory getTransformerFactory(String name) { 
		return mTransformerFactories.get(name);
	}
	
	public void addTransformerFactory(String name, TransformerFactory factory) { 
		mTransformerFactories.put(name, factory);
	}
	
	/** Configure the TransformerFactory plugins */
	protected Map<String, TransformerFactory> loadTransformerFactories() throws ErrorException { 
		Map<String, TransformerFactory> factories = new HashMap<String, TransformerFactory>();
		
		// Load any transformer factories
		initPlugins(factories, TransformerFactory.class);
		TransformerFactory.loadDefaultFactories(this, factories);
		
		return factories;
	}
	
	/**
	 * Register the default search components
	 */
	protected Map<String, SearchComponent> loadSearchComponents() throws ErrorException {
		Map<String, SearchComponent> components = new HashMap<String, SearchComponent>();
		
		initPlugins(components, SearchComponent.class);
		SearchComponents.loadDefaultComponents(this, components);
		
		return components;
	}
	
	protected UpdateIndexer loadUpdateIndexer() throws ErrorException { 
		return createInstance(
				getSearchConfig().getIndexerConfig().getClassName(), 
				UpdateIndexer.class);
	}
	
	/**
	 * Load the request processors
	 */
	protected Map<String,UpdateProcessorChain> loadUpdateProcessorChains() 
			throws ErrorException {
		Map<String, UpdateProcessorChain> map = 
				new HashMap<String, UpdateProcessorChain>();
		
		UpdateProcessorChain def = initPlugins(map, 
				UpdateProcessorChain.class, UpdateProcessorChain.class.getName());
		
		if (def == null) 
			def = map.get(null);
		
		if (def == null) {
			def = UpdateProcessorChain.createDefaultChain(this);
			registerInfoMBean(def);
		}
		
		map.put(null, def);
		map.put("", def);
		
		return map;
	}

	/**
	 * @return an update processor registered to the given name.
	 * Throw an exception if this chain is undefined
	 */
	@Override
	public UpdateProcessorChain getUpdateProcessingChain(final String name) 
			throws ErrorException {
		UpdateProcessorChain chain = mUpdateChains.get(name);
		if (chain == null) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"unknown UpdateRequestProcessorChain: " + name);
		}
		
		return chain;
	}
	
	@Override
	public String getNewIndexDir() {
		String result = SearchHelper.getNewIndexDir(this); 
		
		if (!result.equals(mLastNewIndexDir)) {
			if (LOG.isInfoEnabled())
				LOG.info("New index directory detected: old=" + mLastNewIndexDir + " new=" + result);
		}
		
		mLastNewIndexDir = result;
		return result;
	}
	
	@Override
	protected void onClose() { 
		DirectoryFactory directoryFactory = null; 
		
		if (mSearchControl != null) {
			directoryFactory = mSearchControl.getDirectoryFactory();
			mSearchControl.close();
		}
		
	    try {
	        if (mUpdateIndexer != null) {
	        	mUpdateIndexer.close();
	        	
	        } else {
	        	if (directoryFactory != null) {
	        		// :HACK: normally we rely on updateHandler to do this, 
	        		// but what if updateHandler failed to init?
	        		directoryFactory.close();
	        	}
	        }
	    } catch (Throwable e) {
	    	if (LOG.isErrorEnabled())
	    		LOG.error(e.toString(), e);
	    }
	}
	
	@Override
	public void addCloseHook(final SearchCloseHook hook) { 
		if (hook == null) return;
		
		addCloseHook(new CoreCloseHook() {
				@Override
				public void preClose(Core core) {
					hook.preClose((SearchCore)core);
				}
				@Override
				public void postClose(Core core) {
					hook.postClose((SearchCore)core);
				}
			});
	}
	
	@Override
	public ShardHandlerFactory getShardHandlerFactory() throws ErrorException { 
		return (ShardHandlerFactory)getDescriptor().getContainer()
				.getPluginFactory(SHARD_HANDLER_FACTORY);
	}
	
	static void registerShardHandlerFactory(CoreContainer container) 
			throws ErrorException { 
		container.registerPluginFactory(SHARD_HANDLER_FACTORY, new PluginFactoryHolder() {
				@Override
				protected PluginFactory createFactory() throws ErrorException {
					return loadShardHandlerFactory();
				}
			});
	}
	
	/** The default ShardHandlerFactory used to communicate with other instances */
	static ShardHandlerFactory loadShardHandlerFactory() 
			throws ErrorException { 
		Map<String,String> m = new HashMap<String,String>();
        m.put("class", HttpShardHandlerFactory.class.getName());
        
        PluginInfo info = new ConfigPluginInfo("shardHandlerFactory", m, null, 
        		Collections.<PluginInfo>emptyList());
        
        HttpShardHandlerFactory factory = new HttpShardHandlerFactory();
        factory.init(info);
        
        return factory;
	}
	
	@Override
	public void getCoreStatus(NamedList<Object> info, Params params) throws ErrorException { 
		super.getCoreStatus(info, params);
		
		String idxInfo = params.get(AdminParams.INDEX_INFO);
	    boolean isIndexInfoNeeded = Boolean.parseBoolean(idxInfo == null ? "true" : idxInfo);
		
		info.add("schema", getSchema().getResourceName());
		
		if (isIndexInfoNeeded) {
			SearcherRef searcherRef = getSearcherRef();
			try {
				IDirectoryReader reader = searcherRef.get().getDirectoryReader();
				NamedMap<Object> indexInfo = LukeRequestHandler.getIndexInfo(reader);
				long size = getIndexSize();
				
				indexInfo.add("sizeInBytes", size);
				indexInfo.add("size", NumberUtils.readableSize(size));
				info.add("index", indexInfo);
			} finally {
				searcherRef.decreaseRef();
			}
		}
	}
	
	@Override
	public void getCoreInfo(NamedList<Object> info) throws ErrorException { 
		super.getCoreInfo(info);
		
		info.add("schema", getSchema().getSchemaName());
	}
	
	@Override
	public void getDirectoryInfo(NamedList<Object> info) throws ErrorException { 
		super.getDirectoryInfo(info);
		
		info.add("dirimpl", getDirectoryFactory().getClass().getName());
		
		try {
			info.add("index", getDirectoryFactory().normalize(getIndexDir()));
			
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("Problem getting the normalized index directory path", e);
			
			info.add("index", "N/A");
		}
	}
	
	private long getIndexSize() {
		try {
			return FileUtils.sizeOfDirectory(new File(getIndexDir()));
		} catch (Exception ex) { 
			if (LOG.isErrorEnabled())
				LOG.error(ex.toString(), ex);
			
			return 0;
		}
	}
	
}
