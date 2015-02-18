package org.javenstudio.lightning.core.user;

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

public class UserCoreFactory extends CoreFactory {
	private static final Logger LOG = Logger.getLogger(UserCoreFactory.class);
	
	@Override
	public String getCoreNodeName() { 
		return "usercore";
	}
	
	@Override
	public RequestConfig createRequestConfig(ContextResource config) 
			throws ErrorException { 
		return new UserRequestConfig(config);
	}
	
	@Override
	public Response createCoreResponse(Core core, Request request, 
			ResponseOutput output) throws ErrorException { 
		return new UserResponse((UserCore)core, request, output);
	}
	
	@Override
	public ContextLoader createContextLoader(String instanceDir, 
			ClassLoader parent, Properties properties) { 
		return new UserContextLoader(instanceDir, parent, properties);
	}
	
	@Override
	public Core createCore(CoreConfig config, CoreDescriptor cd) 
			throws ErrorException {
		return new UserCore(this, (String)null, 
				(UserConfig)config, (UserDescriptor)cd, null);
	}
	
	@Override
	public CoreConfig createCoreConfig(CoreAdminConfig conf, 
			ContextLoader loader, String name) throws ErrorException {
		return new UserConfig(conf, loader, name, null);
	}
	
	@Override
	public CoreDescriptor createCoreDescriptor(CoreContainer cores, 
			String name, String instanceDir) throws ErrorException { 
		return new UserDescriptor(cores, name, instanceDir);
	}
	
	@Override
	public CoreSetting createCoreSetting(CoreAdminSetting setting) 
			throws ErrorException { 
		return new UserSetting(setting);
	}
	
	@Override
	public void registerCoreHandlers(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		CoreRequestHandlers.registerHandlers(handlers, core);
		UserCore dcore = (UserCore)core;
		
		handlers.register("/login", 
				UserLoginHandler.createHandler(dcore));
		handlers.register("/message", 
				UserMessageHandler.createHandler(dcore));
		handlers.register("/heartbeat", 
				UserHeartbeatHandler.createHandler(dcore));
		handlers.register("/setting", 
				UserSettingHandler.createHandler(dcore));
		handlers.register("/profile", 
				UserProfileHandler.createHandler(dcore));
		handlers.register("/userinfo", 
				UserInfoHandler.createHandler(dcore));
		handlers.register("/friend", 
				UserFriendHandler.createHandler(dcore));
		handlers.register("/find", 
				UserFindHandler.createHandler(dcore));
		handlers.register("/group", 
				UserGroupHandler.createHandler(dcore));
		handlers.register("/member", 
				UserMemberHandler.createHandler(dcore));
		handlers.register("/contact", 
				UserContactHandler.createHandler(dcore));
		handlers.register("/announcement", 
				UserAnnouncementHandler.createHandler(dcore));
		handlers.register("/cluster", 
				UserClusterHandler.createHandler(dcore));
		handlers.register("/publish", 
				UserPublishHandler.createHandler(dcore));
		handlers.register("/space", 
				UserSpaceHandler.createHandler(dcore));
	}
	
	@Override
	public void onCoreHandlersInited(RequestHandlers handlers, Core core) 
			throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("init default handler for " + core);
		
		if (handlers.get("") == null) 
			handlers.register("", handlers.get("/login")); //default handler
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
		//UserCore.registerShardHandlerFactory(container);
	}
	
}
