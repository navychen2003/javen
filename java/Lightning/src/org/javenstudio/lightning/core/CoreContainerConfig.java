package org.javenstudio.lightning.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextList;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.lightning.context.Config;

public final class CoreContainerConfig extends Config {
	//private static final Logger LOG = Logger.getLogger(CoreContainerConfig.class);

	private final CoreAdminConfig mAdminConfig;
	private final Config mConfig;
	
	static CoreContainerConfig loadConfig(File fconf, 
			ContextLoader loader, CoreAdminConfig config) 
			throws ErrorException { 
		try {
			return loadConfig(fconf.getName(), new FileInputStream(fconf), 
					loader, config);
		} catch (FileNotFoundException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"config file not found: " + fconf, e);
		}
	}
	
	static CoreContainerConfig loadConfig(String sourceName, 
			InputStream source, ContextLoader loader, CoreAdminConfig config) 
			throws ErrorException { 
		//if (LOG.isInfoEnabled())
		//	LOG.info("Loading CoreContainer using Home: " + dir);
		
		return new CoreContainerConfig(
				config, loader, sourceName, source); 
	}
	
	private CoreContainerConfig(CoreAdminConfig config, 
			ContextLoader loader, String sourceName, InputStream source) 
			throws ErrorException { 
		if (config == null || loader == null) throw new NullPointerException();
		mAdminConfig = config;
		mConfig = (Config)loader.openResource(sourceName, source);
	}
	
	public CoreAdminConfig getAdminConfig() { return mAdminConfig; }
	
	public Iterator<ContextNode> getCoreNodes(String coreNodeName) 
			throws ErrorException { 
		return getNodes("*/cores/" + coreNodeName);
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
