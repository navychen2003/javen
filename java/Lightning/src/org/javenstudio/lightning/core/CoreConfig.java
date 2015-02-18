package org.javenstudio.lightning.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.PluginInfo;
import org.javenstudio.lightning.Constants;
import org.javenstudio.lightning.context.Config;
import org.javenstudio.lightning.context.ConfigPluginInfo;
import org.javenstudio.lightning.handler.RequestHandler;

public class CoreConfig extends Config {
	private static final Logger LOG = Logger.getLogger(CoreConfig.class);

	// falcon.xml node constants
	private static final String CORE_NAME = "name";
	private static final String CORE_CONFIG = "config";
	private static final String CORE_INSTDIR = "instanceDir";
	private static final String CORE_DATADIR = "dataDir";
	//private static final String CORE_COLLECTION = "collection";
	//private static final String CORE_ROLES = "roles";
	private static final String CORE_PROPERTIES = "properties";
	
	public static String getCoreName(ContextNode node) throws ErrorException { 
		return node.getAttribute(CORE_NAME);
	}
	
	public static String getCoreInstanceDir(ContextNode node) throws ErrorException { 
		return node.getAttribute(CORE_INSTDIR);
	}
	
	public static String getCoreConfig(ContextNode node) throws ErrorException { 
		return node.getAttribute(CORE_CONFIG);
	}
	
	public static String getCoreProperties(ContextNode node) throws ErrorException { 
		return node.getAttribute(CORE_PROPERTIES);
	}
	
	public static String getCoreDataDir(ContextNode node) throws ErrorException { 
		return node.getAttribute(CORE_DATADIR);
	}
	
	
	private Map<String, List<PluginInfo>> mPluginStore = 
			new LinkedHashMap<String, List<PluginInfo>>();
	
	private final CoreAdminConfig mAdminConfig;
	private final Config mConfig;
	private final String mDataDir;
	
	/** 
	 * Creates a configuration instance from a resource loader, a configuration 
	 * 	name and a stream.
	 * If the stream is null, the resource loader will open the configuration stream.
	 * If the stream is not null, no attempt to load the resource will occur 
	 * 	(the name is not used).
	 * 
	 * @param loader the resource loader
	 * @param name the configuration name
	 * @param is the configuration stream
	 */
	public CoreConfig(CoreAdminConfig conf, ContextLoader loader, 
			String name, InputStream is) throws ErrorException {
		mConfig = (Config)loader.openResource(name, is, "/config/");
		mDataDir = get("dataDir", null);
		mAdminConfig = conf;
		
		loadPluginInfo(RequestHandler.class, "requestHandler", true, true);
	}
	
	public final CoreAdminConfig getAdminConfig() { return mAdminConfig; }
	public final String getLocalStoreDir() { return getAdminConfig().getLocalStoreDir(); }
	public final String[] getStoreUris() { return getAdminConfig().getStoreUris(); }
	public final String getDataDir() { return mDataDir; }
	
	private void loadPluginInfo(Class<?> clazz, String tag, 
			boolean requireName, boolean requireClass) throws ErrorException {
	    List<PluginInfo> result = readPluginInfos(tag, requireName, requireClass);
	    if (!result.isEmpty()) 
	    	mPluginStore.put(clazz.getName(),result);
	}
  
	public List<PluginInfo> readPluginInfos(String tag, 
			boolean requireName, boolean requireClass) throws ErrorException {
		ArrayList<PluginInfo> result = new ArrayList<PluginInfo>();
		
		Iterator<ContextNode> nodes = getNodes(tag);
		while (nodes.hasNext()) { 
			ContextNode node = nodes.next();
			PluginInfo pluginInfo = new ConfigPluginInfo(node, 
					"[config.xml] " + tag, requireName, requireClass);
			if (pluginInfo.isEnabled()) 
				result.add(pluginInfo);
		}
		
		return result;
	}
  
	/**
	 * Config keeps a repository of plugins by the type. The known interfaces are the types.
	 * @param type The key is FQN of the plugin class there are a few  known types: 
	 * Formatter, Fragmenter, RequestHandler,QueryParserPlugin, QueryResponseWriter, 
	 * ValueSourceParser, SearchComponent, QueryConverter, EventListener, DirectoryFactory,
	 * IndexDeletionPolicy, IndexReaderFactory, {@link TransformerFactory}
	 */
	public List<PluginInfo> getPluginInfos(String type) {
		List<PluginInfo> result = mPluginStore.get(type);
		return result == null ? Collections.<PluginInfo>emptyList(): result; 
	}
  
	public PluginInfo getPluginInfo(String type) {
		List<PluginInfo> result = mPluginStore.get(type);
		return result == null || result.isEmpty() ? null: result.get(0);
	}
	
	public static Properties getProperties(String instanceDir, 
			String file, Properties defaults) {
		if (file == null) 
			file = "conf" + File.separator + Constants.CORE_PROPERTIES_FILENAME;
		
	    File corePropsFile = new File(file);
	    if (!corePropsFile.isAbsolute())
	    	corePropsFile = new File(instanceDir, file);
	    
	    Properties p = defaults;
	    if (corePropsFile.exists() && corePropsFile.isFile()) {
	    	p = new Properties(defaults);
	    	InputStream is = null;
	    	try {
	    		is = new FileInputStream(corePropsFile);
	    		p.load(is);
	    	} catch (IOException e) {
	    		if(LOG.isWarnEnabled())
	    			LOG.warn("Error loading properties ",e);
	    	} finally{
	    		IOUtils.closeQuietly(is);        
	    	}
	    }
	    
	    return p;
	}

	@Override
	public String getResourceName() {
		return mConfig.getResourceName();
	}

	@Override
	public String getName() {
		return mConfig.getName();
	}

	@Override
	public ContextLoader getContextLoader() {
		return mConfig.getContextLoader();
	}

	@Override
	public ContextNode getNode(String path) throws ErrorException {
		return mConfig.getNode(path);
	}

	@Override
	public ContextList getNodes(String path) throws ErrorException {
		return mConfig.getNodes(path);
	}

	@Override
	public String getVal(String path, boolean errIfMissing)
			throws ErrorException {
		return mConfig.getVal(path, errIfMissing);
	}
	
}
