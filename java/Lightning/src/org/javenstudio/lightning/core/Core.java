package org.javenstudio.lightning.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.InfoMBean;
import org.javenstudio.falcon.util.InfoMBeanRegistry;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.falcon.util.PluginHolder;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.logging.LogWatcher;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.request.RequestAcceptor;
import org.javenstudio.lightning.request.RequestConfig;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.request.RequestParsers;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;
import org.javenstudio.lightning.response.ResponseWriter;
import org.javenstudio.lightning.response.ResponseWriters;

public abstract class Core implements PluginHolder, RequestAcceptor, InfoMBean {
	private static final Logger LOG = Logger.getLogger(Core.class);
	
	// this core current usage count
	private final AtomicInteger mRefCount = new AtomicInteger(1);
	private final InfoMBeanRegistry mInfoRegistry;
	
	private Collection<CoreCloseHook> mCloseHooks = null;
	
	private final CoreFactory mFactory;
	private final CoreDescriptor mDescriptor;
	private final CoreConfig mConfig;
	
	private CoreRequestHandlers mHandlers;
	private RequestParsers mParsers;
	private ResponseWriters mWriters;
	
	private final String mDataDir;
	private final long mStartTime;
	private boolean mInited = false;
	
	/**
	 * Creates a new core and register it in the list of cores.
	 * If a core with the same name already exists, it will be stopped 
	 * and replaced by this one.
	 *
	 * @param dataDir the index directory
	 * @param config a config instance
	 */
	public Core(CoreFactory factory, String dataDir, 
			CoreConfig config, CoreDescriptor cd) throws ErrorException {
		this(factory, dataDir, config, cd, null);
	}
	
	/**
	 * Creates a new core and register it in the list of cores.
	 * If a core with the same name already exists, it will be stopped 
	 * and replaced by this one.
	 * @param dataDir the index directory
	 * @param config a config instance
	 * @param schema a schema instance
	 */
	public Core(CoreFactory factory, String dataDir, 
			CoreConfig config, CoreDescriptor cd, Core prev) 
			throws ErrorException {
		mInfoRegistry = new InfoMBeanRegistry();
		mFactory = factory;
		mDescriptor = cd;
		mConfig = config;
		
		if (dataDir == null) {
			if (cd.usingDefaultDataDir()) 
				dataDir = config.getDataDir();
			if (dataDir == null) 
				dataDir = cd.getDataDir();
		}

		mDataDir = ContextLoader.normalizeDir(dataDir);
		mStartTime = System.currentTimeMillis();

		if (LOG.isInfoEnabled()) {
			LOG.info("Opening new Core at " + config.getContextLoader().getInstanceDir() 
					+ ", dataDir=" + mDataDir);
		}
		
	}
	
	protected final void onInited() throws ErrorException {
		if (mInited) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Core already inited: " + mDescriptor.getName());
		}
		
		mHandlers = new CoreRequestHandlers(this);
		mHandlers.initHandlers(mConfig);
		
		mParsers = loadRequestParsers(
				mFactory.createRequestConfig(mConfig));
		
		mWriters = loadResponseWriters();
		
		mInfoRegistry.register(this);
		
		mInited = true;
	}
	
	protected CoreRequestHandlers newCoreRequestHandlers() throws ErrorException { 
		return new CoreRequestHandlers(this);
	}
	
	protected RequestParsers loadRequestParsers(RequestConfig conf) throws ErrorException { 
		return new RequestParsers(conf);
	}
	
	protected ResponseWriters loadResponseWriters() throws ErrorException { 
		return new ResponseWriters();
	}
	
	protected final void ensureInited() throws ErrorException { 
		if (!mInited) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Core not inited: " + this);
		}
	}
	
	public CoreAdminConfig getAdminConfig() { 
		return getContainers().getAdminConfig();
	}
	
	public CoreAdminSetting getAdminSetting() { 
		return getContainers().getSetting();
	}
	
	public IHostNode getHostSelf() {
		return getContainers().getCluster().getHostSelf();
	}
	
	public String getHomeDir() { 
		return getAdminConfig().getHomeDir();
	}
	
	public String getLocalStoreDir() { 
		return getAdminConfig().getLocalStoreDir();
	}
	
	public abstract NamedList<Object> getParsedResponse(Request req, 
			Response rsp) throws ErrorException;
	
	public final ContextLoader getContextLoader() { 
		return mConfig.getContextLoader(); 
	}
	
	public final String getConfigResourceName() { 
		return mConfig.getResourceName(); 
	}
	
	public final CoreFactory getFactory() { return mFactory; }
	public final CoreDescriptor getDescriptor() { return mDescriptor; }
	public final CoreConfig getConfig() { return mConfig; }
	
	public final String getName() { return mDescriptor.getName(); }
	public final String getDataDir() { return mDataDir; }
	
	public final long getStartTime() { return mStartTime; }
	public final boolean isDefault() { return mDescriptor.isDefault(); }

	public final CoreSetting getSetting() { 
		return getDescriptor().getContainer().getSetting();
	}
	
	public final GlobalSetting getGlobalSetting() { 
		return getSetting().getAdminSetting().getGlobal();
	}
	
	public final CoreContainers getContainers() { 
		return getDescriptor().getContainer().getContainers();
	}
	
	public final CoreCluster getCluster() { 
		return getContainers().getCluster();
	}
	
	public final IHostCluster getClusterSelf() { 
		return getContainers().getCluster().getClusterSelf();
	}
	
	//public final String getHostName() { 
	//	return getGlobalSetting().getHostName();
	//}
	
	public final String getFriendlyName() {
		return getGlobalSetting().getFriendlyName();
	}
	
	public final String getClusterId() { 
		return getAdminConfig().getClusterId();
	}
	
	public final String getClusterDomain() { 
		return getAdminConfig().getClusterDomain();
	}
	
	public final String getHostAddress() { 
		return getAdminConfig().getHostAddress();
	}
	
	public final int getHttpPort() { 
		return getAdminConfig().getHttpPort();
	}
	
	public final int getHttpsPort() { 
		return getAdminConfig().getHttpsPort();
	}
	
	@Override
	public Request parseRequest(RequestInput input) throws ErrorException { 
		return mParsers.parseRequest(this, input);
	}
	
	@Override
	public Response createResponse(Request request, ResponseOutput output) throws ErrorException { 
		return mFactory.createCoreResponse(this, request, output);
	}
	
	@Override
	public ResponseWriter getResponseWriter(Request request) throws ErrorException { 
		return mWriters.getWriter(request.getResponseWriterType());
	}
	
	@Override
	public RequestHandler getRequestHandler(Request request) throws ErrorException { 
		RequestInput input = request.getRequestInput();
    	String handlerName = input.getHandlerName(request.getParams());
    	
    	//if (LOG.isDebugEnabled()) {
    	//	LOG.debug("request core path: " + input.getQueryPath() 
    	//			+ " handlerName: " + handlerName);
    	//}
    	
    	return mHandlers.get(handlerName);
	}
	
	public RequestHandler createRequestHandler(String className) 
			throws ErrorException {
	    return createInstance(className, RequestHandler.class);
	}
	
	public RequestHandler getRequestHandler(String handlerName) { 
		return mHandlers.get(handlerName);
	}
	
	public Map<String,RequestHandler> getRequestHandlers(Class<?> clazz) { 
		return mHandlers.getAll(clazz);
	}
	
	public Map<String,RequestHandler> getRequestHandlers() { 
		return mHandlers.getRequestHandlers();
	}
	
	public LogWatcher<?> getLogWatcher() { 
		return getDescriptor().getContainer().getLogWatcher();
	}
	
	/**
	 * Returns a Map of name vs InfoMBean objects. 
	 * The returned map is an instance of
	 * a ConcurrentHashMap and therefore no synchronization 
	 * is needed for putting, removing
	 * or iterating over it.
	 *
	 * @return the Info Registry map which contains InfoMBean 
	 * objects keyed by name
	 * @since 1.3
	 */
	public InfoMBeanRegistry getInfoRegistry() {
		return mInfoRegistry;
	}
	
	/**
	 * Add a close callback hook
	 */
	public void addCloseHook(CoreCloseHook hook) {
		if (hook == null) return;
		
		if (mCloseHooks == null) 
			mCloseHooks = new ArrayList<CoreCloseHook>();
     
		mCloseHooks.add(hook);
	}
	
	public void registerInfoMBean(Object instance) throws ErrorException { 
		if (instance != null && instance instanceof InfoMBean) {
			InfoMBean bean = (InfoMBean)instance; 
			if (bean.getMBeanKey() != null)
				mInfoRegistry.register(bean, true);
		}
	}
	
	public void removeInfoMBean(Object instance) throws ErrorException { 
		if (instance != null && instance instanceof InfoMBean) 
			mInfoRegistry.remove((InfoMBean)instance);
	}
	
	public final <T> T newInstance(String cname, Class<T> expectedType) 
			throws ErrorException { 
		try {
			T instance = getContextLoader().newInstance(cname, expectedType);
			
			registerInfoMBean(instance);
			
			return instance;
		} catch (ClassNotFoundException ex) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
		}
	}
	
	public final <T> T createInstance(String className, Class<T> cast) 
			throws ErrorException {
		T instance = getContextLoader().createInstance(PluginHolder.class, 
				this, className, cast);
		
		registerInfoMBean(instance);
		
		return instance;
	}
	
	public final <T> T createPlugin(PluginInfo info, Class<T> cast) 
			throws ErrorException {
		return createPlugin(info, cast, null);
	}
	
	public final <T> T createPlugin(PluginInfo info, Class<T> cast, 
			String defClassName) throws ErrorException {
		T instance = getContextLoader().createPlugin(PluginHolder.class, 
				this, info, cast, defClassName);
		
		registerInfoMBean(instance);
		
		return instance;
	}
	
	/**
	 * @param registry The map to which the instance should be added to. 
	 * The key is the name attribute
	 * @param type the class or interface that the instance should extend or implement.
	 * @param defClassName If PluginInfo does not have a classname, use this as the classname
	 * @return The default instance . The one with (default=true)
	 */
	public <T> T initPlugins(Map<String, T> registry, Class<T> type, 
			String defClassName) throws ErrorException {
		return initPlugins(getConfig().getPluginInfos(type.getName()), 
				registry, type, defClassName);
	}

	public <T> T initPlugins(List<PluginInfo> pluginInfos, Map<String, T> registry, 
			Class<T> type, String defClassName) throws ErrorException {
		T def = null;
		
		for (PluginInfo info : pluginInfos) {
			T o = createPlugin(info, type, defClassName);
			
			registry.put(info.getName(), o);
			
			if (info.isDefault()) 
				def = o;
		}
		
		return def;
	}

	/**
	 * For a given List of PluginInfo return the instances as a List
	 * @param defClassName The default classname if PluginInfo#className == null
	 * @return The instances initialized
	 */
	public <T> List<T> initPlugins(List<PluginInfo> pluginInfos, Class<T> type, 
			String defClassName) throws ErrorException {
		if (pluginInfos.isEmpty()) 
			return Collections.emptyList();
		
		List<T> result = new ArrayList<T>();
		for (PluginInfo info : pluginInfos) { 
			result.add(createPlugin(info, type, defClassName)); 
		}
		
		return result;
	}

	public <T> List<T> initPlugins(List<PluginInfo> pluginInfos, Class<T> type) 
			throws ErrorException {
		return initPlugins(pluginInfos, type, (String)null);
	}
	
	/**
	 *
	 * @param registry The map to which the instance should be added to. 
	 * The key is the name attribute
	 * @param type The type of the Plugin. These should be standard ones registered 
	 * by type.getName() in Config
	 * @return     The default if any
	 */
	public <T> T initPlugins(Map<String, T> registry, Class<T> type) 
			throws ErrorException {
		return initPlugins(registry, type, (String)null);
	}
	
	/** expert: increments the core reference count */
	public void open() {
		mRefCount.incrementAndGet();
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("open: refCount=" + mRefCount.get());
	}
	
	/** Current core usage count. */
	public int getOpenCount() {
		return mRefCount.get();
	}
	
	/**
	 * Close all resources allocated by the core if it is no longer in use...
	 * <ul>
	 *   <li>searcher</li>
	 *   <li>updateHandler</li>
	 *   <li>all CloseHooks will be notified</li>
	 *   <li>All MBeans will be unregistered from MBeanServer if JMX was enabled
	 *       </li>
	 * </ul>
	 * <p>   
	 * <p>
	 * The behavior of this method is determined by the result of decrementing
	 * the core's reference count (A core is created with a reference count of 1)...
	 * </p>
	 * <ul>
	 *   <li>If reference count is > 0, the usage count is decreased by 1 and no
	 *       resources are released.
	 *   </li>
	 *   <li>If reference count is == 0, the resources are released.
	 *   <li>If reference count is &lt; 0, and error is logged and no further action
	 *       is taken.
	 *   </li>
	 * </ul>
	 * @see #isClosed() 
	 */
	public final void close() { 
	    int count = mRefCount.decrementAndGet();
	    // close is called often, and only actually closes if nothing is using it.
	    if (count > 0) { 
	    	//if (LOG.isDebugEnabled())
			//	LOG.debug("close: refCount=" + mRefCount.get());
	    	
	    	return; 
	    }
	    
	    if (count < 0) {
	    	if (LOG.isErrorEnabled()) {
		    	LOG.error("Too many close [count:" + count + "] on " + this + 
		    			". Please report this exception to administor");
	    	}
	    	return;
	    }
	    
	    if (LOG.isInfoEnabled())
	    	LOG.info("Closing " + this);
	    
	    if (mCloseHooks != null) {
	        for (CoreCloseHook hook : mCloseHooks) {
	        	try {
	        		hook.preClose(this);
	        	} catch (Throwable e) {
	        		if (LOG.isErrorEnabled())
	        			LOG.error(e.toString(), e);
	        	}
	        }
	    }
	    
	    try {
	        mInfoRegistry.clear();
	    } catch (Throwable e) {
	    	if (LOG.isErrorEnabled())
	    		LOG.error(e.toString(), e);
	    }
	    
	    onClose();
	    
	    if (mCloseHooks != null) {
	        for (CoreCloseHook hook : mCloseHooks) {
	        	try {
	        		hook.postClose(this);
	        	} catch (Throwable e) {
	        		if (LOG.isErrorEnabled())
	        			LOG.error(e.toString(), e);
	        	}
	        }
	    }
	    
	    if (LOG.isDebugEnabled())
	    	LOG.debug("Close done.");
	}
	
	protected void onClose() {}
	
	@Override
	public String toString() { 
		StringBuilder sbuf = new StringBuilder();
		sbuf.append(getClass().getSimpleName());
		sbuf.append("{");
		sbuf.append("name=").append(getName());
		sbuf.append(",dataDir=").append(mDataDir);
		sbuf.append("}");
		return sbuf.toString();
	}
	
	public String getMBeanKey() { return "Core"; }
	public String getMBeanName() { return getClass().getName(); }
	public String getMBeanVersion() { return "1.0"; }
	public String getMBeanCategory() { return InfoMBean.CATEGORY_CORE; }
	
	@Override
	public String getMBeanDescription() { 
		return "Lightning Core \"" + getName() + "\""; 
	}
	
	@Override
	public NamedList<?> getMBeanStatistics() { 
	    NamedList<Object> lst = new NamedMap<Object>();
	    CoreDescriptor cd = getDescriptor();
	    
	    lst.add("coreName", cd != null ? cd.getName() : "(null)");
	    lst.add("startTime", new Date(mStartTime));
	    lst.add("refCount", getOpenCount());

	    if (cd != null) {
	    	lst.add("aliases", cd.getContainer().getCoreNames(this));
	    	
	    	//CloudDescriptor cloudDesc = cd.getCloudDescriptor();
	    	//if (cloudDesc != null) {
	    	//	String collection = cloudDesc.getCollectionName();
	    	//	if (collection == null) {
	    	//		collection = "_notset_";
	    	//	}
	    	//	lst.add("collection", collection);
	    	//	String shard = cloudDesc.getShardId();
	    	//	if (shard == null) {
	    	//		shard = "_auto_";
	    	//	}
	    	//	lst.add("shard", shard);
	    	//}
	    }
	    
	    return lst;
	}
	
	public void getCoreStatus(NamedList<Object> info, Params params) throws ErrorException {}
	public void getCoreInfo(NamedList<Object> info) throws ErrorException {}
	public void getDirectoryInfo(NamedList<Object> info) throws ErrorException {}
	
}
