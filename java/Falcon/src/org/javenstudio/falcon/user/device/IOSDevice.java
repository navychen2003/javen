package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class IOSDevice extends BaseDevice {

	public IOSDevice(IOSDeviceType type, String key, 
			String name, String ver) throws ErrorException { 
		super(type, key, name, ver);
	}
	
}
