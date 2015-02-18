package org.javenstudio.lightning.core.search;

import java.util.Properties;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContextLoader;
import org.javenstudio.falcon.util.ContextResource;
import org.javenstudio.lightning.core.Core;
import org.javenstudio.lightning.core.CoreAdminConfig;
import org.javenstudio.lightning.core.CoreAdminSetting;
import org.javenstudio.lightning.core.CoreConfig;
import org.javenstudio.lightning.core.CoreContainer;
import org.javenstudio.lightning.core.CoreDescriptor;
import org.javenstudio.lightning.core.CoreFactory;
import org.javenstudio.lightning.core.CoreRequestHandlers;
import org.javenstudio.lightning.core.CoreSetting;
import org.javenstudio.lightning.handler.RequestHandlers;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.request.RequestConfig;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.lightning.response.ResponseOutput;

public class SearchCoreFactory extends CoreFactory {
	private static final Logger LOG = Logger.getLogger(SearchCoreFactory.class);
	
	@Override
	public String getCoreNodeName() { 
		return "indexcore";
	}
	
	@Override
	public RequestConfig createRequestConfig(ContextResource config) 
			throws ErrorException { 
		return new SearchRequestConfig(config);
	}
	
	@Override
	public Response createCoreResponse(Core core, Request request, 
			ResponseOutput output) throws ErrorException { 
		return new SearchResponse((SearchCore)core, (SearchRequest)request, output);
	}
	
	@Override
	public ContextLoader createContextLoader(String instanceDir, 
			ClassLoader parent, Properties properties) { 
		return new SearchContextLoader(instanceDir, parent, properties);
	}
	
	@Override
	public Core createCore(CoreConfig config, CoreDescriptor cd) 
			throws ErrorException {
		return new SearchCore(this, (String)null, 
				(SearchConfig)config, (SearchDescriptor)cd, 
				null);
	}
	
	@Override
	public CoreConfig createCoreConfig(CoreAdminConfig conf, 
			ContextLoader loader, String name) throws ErrorException {
		return new SearchConfig(conf, loader, name, null);
	}
	
	@Override
	public CoreDescriptor createCoreDescriptor(CoreContainer cores, 
			String name, String instanceDir) throws ErrorException { 
		return new SearchDescriptor(cores, name, instanceDir);
	}
	
	@Override
	public CoreSetting createCoreSetting(CoreAdminSetting setting) 
			throws ErrorException { 
		return new SearchSetting(setting);
	}
	
	@Override
	public void registerCoreHandlers(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		CoreRequestHandlers.registerHandlers(handlers, core);
		
		handlers.register("/admin/luke", 
				SearchInfoHandler.createHandler((SearchCore)core));
		handlers.register("/analysis/field", 
				SearchAnalysisHandler.createHandler((SearchCore)core));
		handlers.register("/update", 
				SearchUpdateHandler.createHandler((SearchCore)core));
		handlers.register("/select", 
				SearchQueryHandler.createHandler((SearchCore)core));
		handlers.register("/dataimport", 
				SearchImportHandler.createHandler((SearchCore)core));
	}
	
	@Override
	public void onCoreHandlersInited(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("init default handler for " + core);
		
		if (handlers.get("") == null) 
			handlers.register("", handlers.get("/select")); //default handler
		if (handlers.get("") == null) 
			handlers.register("", handlers.get("/update"));
		
		if (handlers.get("") == null) {
			if (LOG.isWarnEnabled())
				LOG.warn("no default handler is registered (either '/select' or 'standard')");
		}
	}
	
	@Override
	public void onLoadConfig(CoreContainer container) throws ErrorException {
		container.getContainers().initAdmin(container);
	}
	
	@Override
	public void onLoadCore(CoreContainer container) throws ErrorException { 
		registerPluginFactory(container);
	}
	
	static void registerPluginFactory(CoreContainer container) 
			throws ErrorException { 
		SearchCore.registerShardHandlerFactory(container);
	}
	
}
