package org.javenstudio.falcon.user.device;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.user.UserClient;

abstract class BaseUserClient extends UserClient {

	private final BaseDevice mDevice;
	private String mLanguage = null;
	
	public BaseUserClient(Member user, BaseDevice device, 
			String token) throws ErrorException { 
		super(user, token);
		if (user == null || device == null) throw new NullPointerException();
		mDevice = device;
		mLanguage = device.getType().getLanguage();
	}
	
	@Override
	public BaseDevice getDevice() { 
		return mDevice;
	}
	
	@Override
	public BaseDeviceType getDeviceType() { 
		return getDevice().getType();
	}
	
	@Override
	public String getLanguage() { 
		return mLanguage; //getDeviceType().getLanguage();
	}
	
	public void setLanguage(String lang) { 
		mLanguage = Strings.getInstance().getLanguage(lang); 
	}

	@Override
	public String getTheme() {
		return null;
	}

	@Override
	public boolean isRememberMe() {
		return false;
	}
	
}
