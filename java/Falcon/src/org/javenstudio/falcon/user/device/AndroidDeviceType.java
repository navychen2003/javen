package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;

public class AndroidDeviceType extends BaseDeviceType {

	public static final String DEV_ANDROID = "android";
	public static final String DEV_ANDROID_TITLE = "Android";
	
	public AndroidDeviceType(DeviceManager manager) { 
		super(manager, DEV_ANDROID, DEV_ANDROID_TITLE);
	}
	
	@Override
	public AndroidDevice newDevice(String key, String name, String ver) 
			throws ErrorException {
		return new AndroidDevice(this, key, name, ver);
	}
	
}
