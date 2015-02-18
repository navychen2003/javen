package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class HostDevice extends BaseDevice {

	public HostDevice(HostDeviceType type, String key, 
			String name, String ver) throws ErrorException { 
		super(type, key, name, ver);
	}
	
}
