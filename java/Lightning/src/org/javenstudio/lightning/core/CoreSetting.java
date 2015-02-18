package org.javenstudio.lightning.core;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingConf;

public abstract class CoreSetting extends SettingConf {
	
	public CoreSetting(CoreAdminSetting manager) { 
		super(manager);
	}
	
	public CoreAdminSetting getAdminSetting() { return (CoreAdminSetting)getSettingManager(); }
	public CoreContainers getContainers() { return getAdminSetting().getContainers(); }
	
	@Override
	protected void loadSetting(String categoryName, String groupName, 
			String name, Object val) throws ErrorException { 
	}
	
}
