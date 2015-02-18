package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class WinDeviceType extends BaseDeviceType {

	public static final String DEV_WIN = "win";
	public static final String DEV_WIN_TITLE = "Windows";
	
	public WinDeviceType(DeviceManager manager) { 
		super(manager, DEV_WIN, DEV_WIN_TITLE);
	}
	
	@Override
	public WinDevice newDevice(String key, String name, String ver) 
			throws ErrorException {
		return new WinDevice(this, key, name, ver);
	}
	
}
