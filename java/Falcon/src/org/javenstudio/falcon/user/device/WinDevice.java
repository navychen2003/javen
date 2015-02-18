package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class WinDevice extends BaseDevice {

	public WinDevice(WinDeviceType type, String key, 
			String name, String ver) throws ErrorException { 
		super(type, key, name, ver);
	}
	
}
