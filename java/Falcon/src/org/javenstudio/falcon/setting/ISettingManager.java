package org.javenstudio.falcon.setting;

import org.javenstudio.falcon.ErrorException;

public interface ISettingManager {

	public SettingCategory[] getCategories();
	public SettingCategory getCategory(String name);
	
	public void saveSettings() throws ErrorException;
	
}
