package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class IOSDeviceType extends BaseDeviceType {

	public static final String DEV_IOS = "ios";
	public static final String DEV_IOS_TITLE = "Apple";
	
	public IOSDeviceType(DeviceManager manager) { 
		super(manager, DEV_IOS, DEV_IOS_TITLE);
	}
	
	@Override
	public IOSDevice newDevice(String key, String name, String ver) 
			throws ErrorException {
		return new IOSDevice(this, key, name, ver);
	}
	
}
