package org.javenstudio.falcon.user.device;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.Theme;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.user.UserClient;

public class WebUserClient extends UserClient {

	private final WebDevice mDevice;
	
	private String mLanguage = null;
	private String mTheme = null;
	private boolean mRememberMe = false;
	
	public WebUserClient(Member user, WebDevice device, 
			String token) throws ErrorException { 
		super(user, token);
		if (user == null || device == null) throw new NullPointerException();
		mDevice = device;
		mLanguage = device.getType().getLanguage();
		mTheme = device.getType().getTheme();
	}
	
	@Override
	public WebDevice getDevice() { 
		return mDevice;
	}
	
	@Override
	public WebDeviceType getDeviceType() { 
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
		return mTheme; //getDeviceType().getTheme();
	}
	
	public void setTheme(String name) { 
		mTheme = Theme.getThemeName(name);
	}
	
	@Override
	public boolean isRememberMe() { 
		return mRememberMe; 
	}
	
	public void setRememberMe(boolean val) { 
		mRememberMe = val; 
	}
	
}
