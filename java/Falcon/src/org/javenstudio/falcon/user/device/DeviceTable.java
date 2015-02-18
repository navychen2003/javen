package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.setting.SettingTable;

public class DeviceTable {
	//private static final Logger LOG = Logger.getLogger(DeviceTable.class);

	static void saveDevices(DeviceManager manager) 
			throws ErrorException {
		if (manager == null) return;
		
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		SettingTable.saveSettings(database);
	}
	
	static void addDevice(DeviceManager manager, Device item) 
			throws ErrorException {
		if (manager == null || item == null) return;
		
		IDatabase database = manager.getUser().getDataManager().getDatabase();
		SettingTable.addSetting(database, item);
	}
	
	static Device getDevice(DeviceGroup group, String key) 
			throws ErrorException {
		if (group == null || key == null) return null;
		
		IDatabase database = group.getManager().getUser().getDataManager().getDatabase();
		return (Device)SettingTable.loadSetting(database, key, group);
	}
	
}
