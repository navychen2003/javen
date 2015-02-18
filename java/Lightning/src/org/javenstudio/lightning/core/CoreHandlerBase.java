package org.javenstudio.lightning.core;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.ISettingManager;
import org.javenstudio.falcon.setting.SettingManager;
import org.javenstudio.falcon.setting.SettingManagers;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.device.DeviceManager;
import org.javenstudio.lightning.handler.RequestHandlerBase;
import org.javenstudio.util.StringUtils;

public abstract class CoreHandlerBase extends RequestHandlerBase {

	public abstract Core getCore();
	
	public CoreAdminSetting getAdminSetting() { 
		return getCore().getDescriptor().getContainer().getContainers().getSetting(); 
	}
	
	public CoreAdminConfig getAdminConfig() { 
		return getCore().getDescriptor().getContainer().getContainers().getAdminConfig();
	}
	
	public boolean isRequestCache() { 
		return getCore().getDescriptor().getContainer().getContainers()
				.getAdminConfig().isRequestCache();
	}
	
	public ISettingManager checkUserSetting(String[] keyToken, 
			IUserClient.Op op) throws ErrorException { 
		if (keyToken != null && keyToken.length == 3)
			return checkUserSetting(keyToken[0], keyToken[1], keyToken[2], op);
		throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
				"Unauthorized Access");
	}
	
	public ISettingManager checkUserSetting(String hostKey, 
			String userKey, String token, IUserClient.Op op) throws ErrorException { 
		DeviceManager manager = UserHelper.checkUserDevice(hostKey, userKey, token, op);
		if (manager == null) return null;
		if (manager.getUser().isManager()) { 
			return new SettingManagers(new SettingManager[] {
					(SettingManager)manager, getAdminSetting()
				});
		} else
			return (SettingManager)manager;
	}
	
	public static int parseInt(String str) { 
		try { 
			return Integer.parseInt(StringUtils.trim(str));
		} catch (Throwable e) {
			return 0;
		}
	}
	
	public static long parseLong(String str) { 
		try { 
			return Long.parseLong(StringUtils.trim(str));
		} catch (Throwable e) {
			return 0;
		}
	}
	
	public static String toString(Object o) { 
		if (o == null) return "";
		if (o instanceof String) return (String)o;
		if (o instanceof CharSequence) return ((CharSequence)o).toString();
		return o.toString();
	}
	
	public static String trim(String s) { 
		return StringUtils.trim(s);
	}
	
}
