package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class HostDeviceType extends BaseDeviceType {

	public static final String DEV_HOST = "host";
	public static final String DEV_HOST_TITLE = "Host";
	
	public HostDeviceType(DeviceManager manager) { 
		super(manager, DEV_HOST, DEV_HOST_TITLE);
	}
	
	@Override
	public HostDevice newDevice(String key, String name, String ver) 
			throws ErrorException {
		return new HostDevice(this, key, name, ver);
	}
	
}
