package org.javenstudio.lightning.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.table.store.StoreFile;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.job.JobSubmit;
import org.javenstudio.lightning.core.tray.TrayHelper;
import org.javenstudio.lightning.http.HttpHelper;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.FileSystem;

public class CoreContainers {
	private static final Logger LOG = Logger.getLogger(CoreContainers.class);
	
	private final List<CoreService> mServices = 
			new ArrayList<CoreService>();
	
	private final List<CoreContainer> mContainers = 
			new ArrayList<CoreContainer>();
	
	private final String mHomeDir;
	private final CoreAdminConfig mAdminConfig;
	private final CoreAdminSetting mSetting;
	private final ContextLoader mDefaultLoader;
	private final CoreCluster mCluster;
	private final CoreStore mStore;
	
	private CoreAdmin mAdmin = null;
	private boolean mInited = false;
	
	public CoreContainers(String homeDir, String appDir) throws ErrorException {
		if (homeDir == null) throw new NullPointerException();
		if (appDir == null) appDir = homeDir;
		mHomeDir = homeDir;
		
		final ContextLoader loader = CoreFactory.createDefaultLoader(homeDir);
		final CoreAdminConfig config = CoreAdminConfig.load(loader, homeDir, appDir);

		mAdminConfig = config;
		mDefaultLoader = loader;
		mSetting = new CoreAdminSetting(this);
		mCluster = new CoreCluster(this);
		mStore = new CoreStore(this);
		
		TrayHelper.initTray(config.getConf(), mCluster.getHostTooltip());
	}
	
	public String getHomeDir() { return mHomeDir; }
	public ContextLoader getDefaultLoader() { return mDefaultLoader; }
	
	public CoreAdminConfig getAdminConfig() { return mAdminConfig; }
	public CoreAdminSetting getSetting() { return mSetting; }
	public CoreCluster getCluster() { return mCluster; }
	public CoreStore getUserStore() { return mStore; }
	
	public Configuration getConfiguration() { 
		return getAdminConfig().getConf(); 
	}
	
	public synchronized CoreAdmin getAdmin() { 
		if (mAdmin == null) 
			throw new NullPointerException("CoreAdmin not inititalized");
		return mAdmin; 
	}
	
	public CoreContainerConfig loadConfig(ContextLoader loader) 
			throws ErrorException { 
		if (loader == null) throw new NullPointerException();
		
		//final String configFileName = CoreAdminConfig.DEFAULT_LIGHTNING_XML_FILENAME;
		//final String configDefault = CoreAdminConfig.DEFAULT_LIGHTNING_XML;
		
		final CoreAdminConfig adminConfig = getAdminConfig();
		final File fconf = adminConfig.getConfigFile();
		
		if (fconf != null && fconf.exists()) { 
			return CoreContainerConfig.loadConfig(fconf, loader, adminConfig);
			
		} else { 
			if (LOG.isErrorEnabled())
				LOG.error("loadConfig: config file: " + fconf + " not found");
			
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Lightning config file: " + fconf + " not found");
			
			//if (LOG.isInfoEnabled())
			//	LOG.info("no " + configFileName + " file found - use default");
			
			//try {
			//	return CoreContainerConfig.loadConfig("<default>", 
			//			new ByteArrayInputStream(configDefault.getBytes("UTF-8")), 
			//			loader, adminConfig);
				
			//} catch (UnsupportedEncodingException e) { 
			//	throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			//}
		}
	}
	
	public synchronized boolean isInited() { return mInited; }
	
	public synchronized void onInited() throws ErrorException { 
		if (mInited) throw new RuntimeException("CoreContainers Initialized");
		
		if (LOG.isDebugEnabled()) LOG.debug("onInited");
		
		getSetting().loadSettings();
		
		for (CoreContainer c : mContainers) { 
			c.onInited();
		}
		
		mInited = true;
	}
	
	public synchronized void init(CoreInitializer initer) 
			throws ErrorException { 
		if (initer == null) throw new NullPointerException();
		if (mInited) throw new RuntimeException("CoreContainers Initialized");
		
		if (LOG.isInfoEnabled())
			LOG.info("init: core: " + initer);
		
		CoreContainer container = initer.initialize(this, initer.getCoreName());
		if (container == null) 
			throw new NullPointerException();
		
		for (CoreContainer c : mContainers) { 
			if (c == container) return;
		}
		
		mContainers.add(container);
	}
	
	public synchronized void init(ServiceInitializer initer) 
			throws ErrorException { 
		if (initer == null) throw new NullPointerException();
		if (mInited) throw new RuntimeException("CoreContainers Initialized");
		
		if (LOG.isInfoEnabled())
			LOG.info("init: service: " + initer);
		
		Iterator<ContextNode> nodes = getAdminConfig()
				.getServiceNodes(initer.getServiceName());
		
		while (nodes.hasNext()) {
			ContextNode node = nodes.next();
			if (node == null) continue;
			
			String nodeName = node.getNodeName();
			String name = node.getAttribute("name");
			String enable = node.getAttribute("enable");
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("init: " + nodeName + ": name=" + name 
						+ " enable=" + enable);
			}
			
			if (enable != null && enable.equalsIgnoreCase("true")) {
				CoreService service = initer.initialize(this, name);
				if (service == null) 
					throw new NullPointerException();
				
				for (CoreService s : mServices) { 
					if (s == service) return;
				}
				
				mServices.add(service);
			}
		}
	}
	
	public synchronized void initCores() throws ErrorException {
		if (mInited) throw new RuntimeException("CoreContainers Initialized");
		
		Iterator<ContextNode> nodes = getAdminConfig()
				.getCoreNodes("core");
		
		while (nodes.hasNext()) {
			ContextNode node = nodes.next();
			if (node == null) continue;
			
			String nodeName = node.getNodeName();
			String name = node.getAttribute("name");
			String clazz = node.getAttribute("class");
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("init: " + nodeName + ": name=" + name 
						+ " class=" + clazz);
			}
			
			if (clazz != null && clazz.length() > 0) {
				final CoreInitializer initer;
				try {
					initer = getAdminConfig().getLoader()
							.newInstance(clazz, CoreInitializer.class);
				} catch (ClassNotFoundException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				}
				
				CoreContainer container = initer.initialize(this, name);
				if (container == null) 
					throw new NullPointerException();
				
				for (CoreContainer c : mContainers) { 
					if (c == container) return;
				}
				
				mContainers.add(container);
			}
		}
	}
	
	public synchronized void initServices() throws ErrorException { 
		if (mInited) throw new RuntimeException("CoreContainers Initialized");
		
		Iterator<ContextNode> nodes = getAdminConfig()
				.getServiceNodes("service");
		
		while (nodes.hasNext()) {
			ContextNode node = nodes.next();
			if (node == null) continue;
			
			String nodeName = node.getNodeName();
			String name = node.getAttribute("name");
			String clazz = node.getAttribute("class");
			String enable = node.getAttribute("enable");
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("init: " + nodeName + ": name=" + name 
						+ " class=" + clazz + " enable=" + enable);
			}
			
			if (clazz != null && clazz.length() > 0 && enable != null && 
				enable.equalsIgnoreCase("true")) {
				final ServiceInitializer initer;
				try {
					initer = getAdminConfig().getLoader()
							.newInstance(clazz, ServiceInitializer.class);
				} catch (ClassNotFoundException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
				}
				
				CoreService service = initer.initialize(this, name);
				if (service == null) 
					throw new NullPointerException();
				
				for (CoreService s : mServices) { 
					if (s == service) return;
				}
				
				mServices.add(service);
			}
		}
	}
	
	public synchronized CoreService getService(Class<?> clazz) { 
		if (clazz == null) return null;
		
		for (CoreService s : mServices) { 
			if (s != null && s.getClass() == clazz) 
				return s;
		}
		
		return null;
	}
	
	public synchronized void shutdown() { 
		if (LOG.isDebugEnabled()) LOG.debug("shutdown");
		
		for (JobSubmit.JobWork<?> work : JobSubmit.getWorks()) { 
			if (work != null) { 
				if (LOG.isInfoEnabled())
					LOG.info("shutdown: cancel work: " + work);
				
				work.cancel();
				work.waitDone();
			}
		}
		
		HttpHelper.closeConnections();
		
		for (CoreContainer container : mContainers) { 
			container.shutdown();
		}
		
		for (CoreService service : mServices) { 
			service.shutdown();
		}
		
		mCluster.close();
		mStore.close();
		StoreFile.shutdownBlockCache();
		
		try {
			FileSystem.closeAll();
		} catch (IOException e) { 
			if (LOG.isErrorEnabled())
				LOG.error("shutodwn: error: " + e, e);
		}
	}
	
	public synchronized Collection<CoreContainer> getContainers() { 
		List<CoreContainer> lst = new ArrayList<CoreContainer>();
		for (CoreContainer container : mContainers) { 
			lst.add(container);
		}
		return lst;
	}
	
	public synchronized Core getCore(String name) { 
		for (CoreContainer container : mContainers) { 
			Core core = container.getCore(name);
			if (core != null) 
				return core;
		}
		return null;
	}

	public synchronized Collection<Core> getCores() { 
		List<Core> lst = new ArrayList<Core>();
		for (CoreContainer container : mContainers) { 
			for (Core core : container.getCores()) {
				if (core != null)
					lst.add(core);
			}
		}
		return lst;
	}
	
	public synchronized Collection<String> getCoreNames() { 
		List<String> lst = new ArrayList<String>();
		for (CoreContainer container : mContainers) { 
			Collection<String> names = container.getCoreNames();
			if (names != null) 
				lst.addAll(names);
		}
		return lst;
	}
	
	public synchronized String getDefaultCoreName() { 
		for (CoreContainer container : mContainers) { 
			String name = container.getDefaultCoreName();
			if (name != null && name.length() > 0) 
				return name;
		}
		return null;
	}
	
	public synchronized Map<String,Throwable> getInitFailures() { 
		Map<String,Throwable> failures = new LinkedHashMap<String,Throwable>();
		for (CoreContainer container : mContainers) { 
			failures.putAll(container.getInitFailures());
		}
		return failures;
	}
	
	public synchronized void initAdmin(CoreContainer container) 
			throws ErrorException {
		if (mAdmin == null) { 
			if (LOG.isInfoEnabled())
				LOG.info("init CoreAdmin by " + container);
			
			CoreFactory factory = container.getFactory();
			ContextLoader loader = container.getContextLoader();
			CoreAdminConfig config = container.getConfig().getAdminConfig();
			
			mAdmin = new CoreAdmin(this, factory, loader, config);
		}
	}
	
}
