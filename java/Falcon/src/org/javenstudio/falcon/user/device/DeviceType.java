package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingCategory;
import org.javenstudio.falcon.setting.SettingGroup;
import org.javenstudio.falcon.util.IParams;

public abstract class DeviceType {

	private final DeviceManager mManager;
	private final String mType;
	private final String mTitle;
	
	protected DeviceType(DeviceManager manager, String type, String title) { 
		if (manager == null || type == null || title == null) 
			throw new NullPointerException();
		mManager = manager;
		mType = type;
		mTitle = title;
	}
	
	public DeviceManager getManager() { return mManager; }
	public String getName() { return mType; }
	public String getTitle() { return mTitle; }
	
	@Override
	public boolean equals(Object obj) { 
		if (obj == this) return true;
		if (obj == null || !(obj instanceof DeviceType)) return false;
		
		DeviceType other = (DeviceType)obj;
		return this.getManager() == other.getManager() && 
				this.getName().equals(other.getName()) && 
				this.getTitle().equals(other.getTitle());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{type=" + mType + ",title=" + mTitle + "}";
	}
	
	public abstract String getLanguage();
	public abstract Device newDevice(String key, 
			String name, String ver) throws ErrorException;
	
	protected abstract SettingGroup initDeviceSetting(
			SettingCategory category, SettingGroup.UpdateHandler handler) 
			throws ErrorException;
	
	protected abstract boolean updateDeviceSetting(SettingGroup group, 
			IParams params) throws ErrorException;
	
	protected abstract boolean loadDeviceSetting(String name, 
			Object val) throws ErrorException;
	
}
