package org.javenstudio.lightning.core.datum;

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

public class DatumCoreFactory extends CoreFactory {
	private static final Logger LOG = Logger.getLogger(DatumCoreFactory.class);
	
	@Override
	public String getCoreNodeName() { 
		return "datumcore";
	}
	
	@Override
	public RequestConfig createRequestConfig(ContextResource config) 
			throws ErrorException { 
		return new DatumRequestConfig(config);
	}
	
	@Override
	public Response createCoreResponse(Core core, Request request, 
			ResponseOutput output) throws ErrorException { 
		return new DatumResponse((DatumCore)core, request, output);
	}
	
	@Override
	public ContextLoader createContextLoader(String instanceDir, 
			ClassLoader parent, Properties properties) { 
		return new DatumContextLoader(instanceDir, parent, properties);
	}
	
	@Override
	public Core createCore(CoreConfig config, CoreDescriptor cd) 
			throws ErrorException {
		return new DatumCore(this, (String)null, 
				(DatumConfig)config, (DatumDescriptor)cd, null);
	}
	
	@Override
	public CoreConfig createCoreConfig(CoreAdminConfig conf, 
			ContextLoader loader, String name) throws ErrorException {
		return new DatumConfig(conf, loader, name, null);
	}
	
	@Override
	public CoreDescriptor createCoreDescriptor(CoreContainer cores, 
			String name, String instanceDir) throws ErrorException { 
		return new DatumDescriptor(cores, name, instanceDir);
	}
	
	@Override
	public CoreSetting createCoreSetting(CoreAdminSetting setting) 
			throws ErrorException { 
		return new DatumSetting(setting);
	}
	
	@Override
	public void registerCoreHandlers(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		CoreRequestHandlers.registerHandlers(handlers, core);
		DatumCore dcore = (DatumCore)core;
		
		handlers.register("/dashboard", 
				DatumDashboardHandler.createHandler(dcore));
		handlers.register("/section", 
				DatumSectionHandler.createHandler(dcore));
		handlers.register("/sectioninfo", 
				DatumSectionInfoHandler.createHandler(dcore));
		handlers.register("/folder", 
				DatumFolderHandler.createHandler(dcore));
		handlers.register("/upload", 
				DatumUploadHandler.createHandler(dcore));
		handlers.register("/update", 
				DatumUpdateHandler.createHandler(dcore));
		handlers.register("/library", 
				DatumLibraryHandler.createHandler(dcore));
		handlers.register("/artwork", 
				DatumArtworkHandler.createHandler(dcore));
		handlers.register("/value", 
				DatumValueHandler.createHandler(dcore));
		handlers.register("/fetch", 
				DatumFetchHandler.createHandler(dcore));
		handlers.register(DatumFileHandler.PATH_PREFIX, 
				DatumFileHandler.createHandler(dcore));
		handlers.register(DatumImageHandler.PATH_PREFIX, 
				DatumImageHandler.createHandler(dcore));
		handlers.register(DatumDownloadHandler.PATH_PREFIX, 
				DatumDownloadHandler.createHandler(dcore));
	}
	
	@Override
	public void onCoreHandlersInited(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("init default handler for " + core);
		
		if (handlers.get("") == null) 
			handlers.register("", handlers.get("/dashboard")); //default handler
		//if (handlers.get("") == null) 
		//	handlers.register("", handlers.get("/update"));
		
		if (handlers.get("") == null) {
			if (LOG.isWarnEnabled())
				LOG.warn("no default handler is registered (either '/dashboard' or 'standard')");
		}
	}
	
	@Override
	public void onLoadConfig(CoreContainer container) throws ErrorException {
	}
	
	@Override
	public void onLoadCore(CoreContainer container) throws ErrorException { 
		registerPluginFactory(container);
	}
	
	static void registerPluginFactory(CoreContainer container) 
			throws ErrorException { 
		//DatumCore.registerShardHandlerFactory(container);
	}
	
}
