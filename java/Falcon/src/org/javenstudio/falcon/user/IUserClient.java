package org.javenstudio.falcon.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.device.Device;
import org.javenstudio.falcon.user.device.DeviceType;

public interface IUserClient {

	public static enum Op { 
		ACCESS, REFRESH, HEARTBEAT
	}
	
	public static interface Factory { 
		public IUserClient create(IMember user, String token) 
				throws ErrorException;
	}
	
	public IMember getUser();
	
	public Device getDevice();
	public DeviceType getDeviceType();
	
	public String getClientId();
	public String getClientKey();
	public String getToken();
	public String getLanguage();
	public String getTheme();
	
	public long getLoginTime();
	public long getUpdateTime();
	public long getAccessTime();
	
	public boolean isRememberMe();
	public void refreshToken() throws ErrorException;
	
	public void logout();
	public void close();
	
}
