package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public interface IDeviceStore {

	public NamedList<Object> loadDeviceSetting(DeviceManager manager) 
			throws ErrorException;
	
	public void saveDeviceSetting(DeviceManager manager, 
			NamedList<Object> items) throws ErrorException;
	
}
