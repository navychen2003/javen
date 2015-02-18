package org.javenstudio.lightning.core;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;

public class CoreInitializer {
	private static final Logger LOG = Logger.getLogger(CoreInitializer.class);
	
	private final CoreFactory mFactory;
	
	public CoreInitializer(CoreFactory factory) { 
		if (factory == null) throw new NullPointerException();
		mFactory = factory;
	}
	
	public String getCoreName() { 
		return mFactory.getCoreNodeName();
	}
	
	public CoreContainer initialize(CoreContainers containers, String name) 
			throws ErrorException { 
		if (containers == null) throw new NullPointerException();
		if (LOG.isDebugEnabled())
			LOG.debug("initialize: " + this + " name=" + name);
		
		String homeDir = containers.getHomeDir();
		
		ContextLoader loader = mFactory.createContextLoader(homeDir);
		CoreContainer cores = new CoreContainer(containers, mFactory, loader);
		
		CoreContainerConfig config = containers.loadConfig(loader);
		cores.init(config);
		
		return cores;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{factory=" + mFactory + "}";
	}
	
}
