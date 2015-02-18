package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class AndroidDevice extends BaseDevice {

	public AndroidDevice(AndroidDeviceType type, String key, 
			String name, String ver) throws ErrorException { 
		super(type, key, name, ver);
	}
	
}
