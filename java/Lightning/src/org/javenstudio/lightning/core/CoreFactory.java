package org.javenstudio.lightning.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextNode;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.lightning.core.search.SearchContextLoader;
import org.javenstudio.lightning.handler.RequestHandlers;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.request.RequestConfig;
import org.javenstudio.lightning.response.AdminResponse;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;

public abstract class CoreFactory {

	public abstract String getCoreNodeName();
	
	public Collection<ContextNode> getCoreNodes(CoreContainerConfig config) 
			throws ErrorException {
		Iterator<ContextNode> it = config.getCoreNodes(getCoreNodeName());
		List<ContextNode> nodes = new ArrayList<ContextNode>();
		while (it.hasNext()) { 
			nodes.add(it.next());
		}
		return nodes;
	}
	
	public static ContextLoader createDefaultLoader(String instanceDir) { 
		return createDefaultLoader(instanceDir, null, null);
	}
	
	public static ContextLoader createDefaultLoader(String instanceDir, 
			ClassLoader parent, Properties properties) { 
		return new SearchContextLoader(instanceDir, parent, properties);
	}
	
	public ContextLoader createContextLoader(String instanceDir) { 
		return createContextLoader(instanceDir, null, null);
	}
	
	public ContextLoader createContextLoader(String instanceDir, 
			ClassLoader parent, Properties properties) { 
		return createDefaultLoader(instanceDir, parent, properties);
	}
	
	public CoreDescriptor createCoreDescriptor(CoreContainer cores, 
			String name, String instanceDir) throws ErrorException { 
		return new CoreDescriptor(cores, name, instanceDir);
	}
	
	public CoreConfig createCoreConfig(CoreAdminConfig conf, 
			ContextLoader loader, String name) throws ErrorException {
		return new CoreConfig(conf, loader, name, null);
	}
	
	public abstract CoreSetting createCoreSetting(CoreAdminSetting setting) 
			throws ErrorException;
	
	public void onLoadConfig(CoreContainer container) throws ErrorException {}
	public void onLoadCore(CoreContainer container) throws ErrorException {}
	
	public abstract Core createCore(CoreConfig config, CoreDescriptor cd) 
			throws ErrorException;
	
	public abstract RequestConfig createRequestConfig(ContextResource config) 
			throws ErrorException;
	
	public Response createAdminResponse(CoreAdmin admin, Request request, 
			ResponseOutput output) throws ErrorException { 
		return new AdminResponse(output, request);
	}
	
	public abstract Response createCoreResponse(Core core, Request request, 
			ResponseOutput output) throws ErrorException;
	
	public void registerAdminHandlers(RequestHandlers handlers, CoreAdmin admin) 
			throws ErrorException { 
		CoreAdminHandlers.registerHandlers(handlers, admin);
	}
	
	public void registerCoreHandlers(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		CoreRequestHandlers.registerHandlers(handlers, core);
	}
	
	public void onCoreHandlersInited(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		// do nothing
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{name=" + getCoreNodeName() + "}";
	}
	
}
