package org.javenstudio.lightning.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.PluginFactory;
import org.javenstudio.falcon.util.PluginFactoryHolder;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.logging.DefaultLogger;
import org.javenstudio.lightning.logging.ListenerConfig;
import org.javenstudio.lightning.logging.LogWatcher;

public final class CoreContainer {
	private static final Logger LOG = Logger.getLogger(CoreContainer.class);

	private final Map<Core,String> mCoreToName = new ConcurrentHashMap<Core,String>();
	private final Map<String,Core> mCores = new LinkedHashMap<String,Core>();
	
	private final Map<String,Throwable> mInitFailures = 
			Collections.synchronizedMap(new LinkedHashMap<String,Throwable>());
	
	private final Map<String,PluginFactoryHolder> mFactories = 
			new HashMap<String,PluginFactoryHolder>();
	
	private volatile boolean mIsShutdown = false;
	private boolean mLoaded = false;
	
	private final CoreContainers mContainers;
	private final CoreFactory mFactory;
	private final ContextLoader mLoader;
	private final LogWatcher<?> mLogWatcher;
	
	private String mDefaultCoreName;
	private Properties mProperties;
	private CoreContainerConfig mConfig;
	private CoreSetting mSetting;
	
	public CoreContainer(CoreContainers containers, 
			CoreFactory factory, ContextLoader loader) { 
		if (containers == null || factory == null || loader == null) 
			throw new NullPointerException();
		mContainers = containers;
		mFactory = factory;
		mLoader = loader;
		mLogWatcher = initLogWatcher();
	}
	
	public final CoreContainers getContainers() { return mContainers; }
	public final CoreFactory getFactory() { return mFactory; }
	public final ContextLoader getContextLoader() { return mLoader; }
	
	public final Properties getProperties() { return mProperties; }
	public final CoreContainerConfig getConfig() { return mConfig; }
	public final CoreSetting getSetting() { return mSetting; }
	
	public final String getDefaultCoreName() { return mDefaultCoreName; }
	public final String getHomeDir() { return mContainers.getHomeDir(); }
	
	public final Map<String,Throwable> getInitFailures() { return mInitFailures; }
	public final boolean isShutdown() { return mIsShutdown; }
	
	public final LogWatcher<?> getLogWatcher() { return mLogWatcher; }
	
	protected LogWatcher<?> initLogWatcher() { 
		return DefaultLogger.getLogWatcher();
	}
	
	@Override
	public String toString() { 
		return "CoreContainer{factory=" + mFactory + ",loader=" + mLoader + "}";
	}
	
	public synchronized void onInited() throws ErrorException { 
		
	}
	
	public synchronized void init(CoreContainerConfig config) throws ErrorException { 
		if (mLoaded) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"CoreContainer already loaded");
		}
		
		if (config == null) throw new NullPointerException();
		mConfig = config; 
		
		ListenerConfig watcherConf = new ListenerConfig(
				mConfig.get("*/logging/watcher/@threshold", null),
				mConfig.getInt("*/logging/watcher/@size", 50));
		
		if (watcherConf.getSize() > 0) { 
			if (LOG.isInfoEnabled())
				LOG.info("Registering Log Listener");
			
			mLogWatcher.registerListener(watcherConf);
		}
		
		mFactory.onLoadConfig(this);
		mSetting = mFactory.createCoreSetting(getContainers().getSetting());
		
		mDefaultCoreName = mConfig.getAdminConfig().getDefaultCoreName();
		
		mProperties = mConfig.readProperties(
				mConfig.getNode(Constants.DEFAULT_HOST_CONTEXT));
		
		mFactory.onLoadCore(this);
		
		Collection<ContextNode> nodes = mFactory.getCoreNodes(mConfig);
		
		for (ContextNode node : nodes) { 
			if (node != null) loadCore(node);
		}
		
		mLoaded = true;
	}
	
	private String checkDefault(String name) {
	    return (null == name || name.isEmpty()) ? mDefaultCoreName : name;
	}
	
	public void registerPluginFactory(String name, PluginFactoryHolder holder) 
			throws ErrorException { 
		if (mLoaded) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"CoreContainer already loaded");
		}
		
		if (name == null || holder == null) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"PluginFactoryHolder or name is null");
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("registerPluginFactory: name=" + name 
					+ " holder=" + holder.getClass().getName());
		}
		
		synchronized (mFactories) {
			mFactories.put(name, holder);
		}
	}
	
	public PluginFactory getPluginFactory(String name) throws ErrorException { 
		PluginFactoryHolder holder = null; 
		
		synchronized (mFactories) {
			holder = mFactories.get(name);
		}
		
		if (holder == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"PluginFactory: " + name + " not registered");
		}
		
		return holder.getFactory();
	}
	
	/** 
	 * Gets a core by name and increase its refcount.
	 * @see Core#close() 
	 * @param name the core name
	 * @return the core if found
	 */
	public Core getCore(String name) {
		name = checkDefault(name);
		synchronized (mCores) {
			Core core = mCores.get(name);
			if (core != null)
				core.open();  // increment the ref count while still synchronized
			
			return core;
		}
	}
	
	/**
	 * @return a Collection of registered Cores
	 */
	public Collection<Core> getCores() {
		List<Core> lst = new ArrayList<Core>();
		synchronized (mCores) {
			lst.addAll(this.mCores.values());
		}
		return lst;
	}

	/**
	 * @return a Collection of the names that cores are mapped to
	 */
	public Collection<String> getCoreNames() {
		List<String> lst = new ArrayList<String>();
		synchronized (mCores) {
			lst.addAll(this.mCores.keySet());
		}
		return lst;
	}

	/** 
	 * This method is currently experimental.
	 * @return a Collection of the names that a specific core is mapped to.
	 */
	public Collection<String> getCoreNames(Core core) {
		List<String> lst = new ArrayList<String>();
		synchronized (mCores) {
			for (Map.Entry<String,Core> entry : mCores.entrySet()) {
				if (core == entry.getValue()) 
					lst.add(entry.getKey());
			}
		}
		return lst;
	}
	
	private void loadCore(ContextNode node) throws ErrorException { 
		try {
			String rawName = CoreConfig.getCoreName(node);
        	if (rawName == null) {
        		throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
        				"Each core in " + Constants.LIGHTNING_XML_FILENAME + 
        				" must have a 'name'");
        	}
			
        	if (LOG.isDebugEnabled())
        		LOG.debug("loadCore: name=" + rawName + " node=" + node);
        	
        	String name = rawName;
        	CoreDescriptor p = mFactory.createCoreDescriptor(this, name, 
        			CoreConfig.getCoreInstanceDir(node));
        	
        	// deal with optional settings
            String opt = CoreConfig.getCoreConfig(node);
            if (opt != null) 
            	p.setConfigName(opt);
            
            opt = CoreConfig.getCoreProperties(node);
            if (opt != null) 
            	p.setPropertiesName(opt);
            
            opt = CoreConfig.getCoreDataDir(node);
            if (opt != null) 
            	p.setDataDir(opt);
        	
            p.setProperties(mConfig.readProperties(node));
            
            Core core = createCore(p);
            register(name, core, false);
            
            // track original names
            mCoreToName.put(core, rawName);
            
		} catch (Throwable ex) {
        	if (ex instanceof ErrorException) {
	        	throw (ErrorException)ex;
	        } else { 
	        	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
	        			"load core error", ex);
	        }
        }
	}
	
	/**
	 * Creates a new core based on a descriptor but does not register it.
	 *
	 * @param dcore a core descriptor
	 * @return the newly created core
	 */
	private Core createCore(CoreDescriptor dcore) throws ErrorException {
	    final String name = dcore.getName();
	    Exception failure = null;
	    
	    try {
	    	// Make the instanceDir relative to the cores instanceDir if not absolute
	    	File idir = new File(dcore.getInstanceDir());
	    	if (!idir.isAbsolute()) 
	    		idir = new File(getHomeDir(), dcore.getInstanceDir());
	    	
	    	String instanceDir = idir.getPath();
	    	
	    	if (LOG.isInfoEnabled()) {
	    		LOG.info("Creating Core '" + name 
	    				+ "' using instanceDir: " + instanceDir);
	    	}
	    	
	    	// Initialize the core config
	    	ContextLoader coreLoader = mFactory.createContextLoader(
	    			instanceDir, mConfig.getAdminConfig().getLibLoader(), 
	    			CoreConfig.getProperties(instanceDir, dcore.getPropertiesName(), 
	    					dcore.getProperties()));
		    
	    	CoreConfig config = mFactory.createCoreConfig(
	    			mConfig.getAdminConfig(), 
	    			coreLoader, dcore.getConfigName());
	    	
	    	Core core = mFactory.createCore(config, dcore);
	    	
	    	return core;
	    } catch (RuntimeException e4) {
	    	failure = e4;
	    	throw e4;
	    } catch (ErrorException e5) {
	    	failure = e5;
	    	throw e5;
	    } finally {
	    	if (failure != null && LOG.isErrorEnabled()) 
	    		LOG.error("Unable to create core: " + name, failure);
	    	
	    	synchronized (mInitFailures) { 
	    		// remove first so insertion order is updated and newest is last
	    		mInitFailures.remove(name);
	    		if (failure != null) 
	    			mInitFailures.put(name, failure);
	    	}
	    }
	}
	
	/**
	 * Registers a Core descriptor in the registry using the core's name.
	 * If returnPrev==false, the old core, if different, is closed.
	 * @return a previous core having the same name if it existed and returnPrev==true
	 */
	public Core register(Core core, boolean returnPrev) throws ErrorException {
		return register(core.getName(), core, returnPrev);
	}
	
	/**
	 * Registers a Core descriptor in the registry using the specified name.
	 * If returnPrevNotClosed==false, the old core, if different, is closed. 
	 * if true, it is returned w/o closing the core
	 *
	 * @return a previous core having the same name if it existed
	 */
	public Core register(String name, Core core, boolean returnPrevNotClosed) 
			throws ErrorException {
		if (core == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Can not register a null core.");
		}
		
		if (name == null || name.indexOf( '/'  ) >= 0 || name.indexOf( '\\' ) >= 0) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Invalid core name: " + name);
		}
		
		core.ensureInited();
		
		Core old = null;
		synchronized (mCores) {
			if (mIsShutdown) {
				core.close();
				throw new IllegalStateException("This CoreContainer has been shutdown");
			}
			
			old = mCores.put(name, core);
			mInitFailures.remove(name);
			
			/**
			 * set both the name of the descriptor and the name of the
			 * core, since the descriptors name is used for persisting.
			 */
			//core.setName(name);
			core.getDescriptor().setName(name);
		}

		if (old == null || old == core) {
			if (LOG.isInfoEnabled())
				LOG.info("registering core: " + name);
			
			return null;
			
		} else {
			if (LOG.isInfoEnabled())
				LOG.info("replacing core: " + name);
			
			if (!returnPrevNotClosed) 
				old.close();
			
			return old;
		}
	}
	
	/**
	 * Stops all cores.
	 */
	public void shutdown() {
		if (LOG.isInfoEnabled()) {
		    LOG.info("Shutting down CoreContainer instance="
		    		+ System.identityHashCode(this) + " home=" + getHomeDir());
		}
        mIsShutdown = true;
        
        try {
        	synchronized (mCores) {
        		for (Core core : mCores.values()) {
        			try {
        				core.close();
        			} catch (Throwable t) {
        				if (LOG.isErrorEnabled())
        					LOG.error("Error shutting down core", t);
        			}
        		}
        		mCores.clear();
        	}
        	
        	synchronized (mFactories) {
        		for (PluginFactoryHolder holder : mFactories.values()) { 
        			PluginFactory factory = holder.getFactoryOrNull();
        			if (factory != null) 
        				factory.close();
        		}
        	}
        	
        } finally {
        	//if (shardHandlerFactory != null) 
        	//	shardHandlerFactory.close();
          
        }
	}
	
}
