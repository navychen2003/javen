package org.javenstudio.falcon.setting;

import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;

public abstract class SettingConf extends ValueHelper {
	private static final Logger LOG = Logger.getLogger(SettingConf.class);

	private final SettingManager mManager;
	
	public SettingConf(SettingManager manager) { 
		if (manager == null) throw new NullPointerException();
		mManager = manager;
		mManager.addSetting(this);
	}
	
	public final SettingManager getSettingManager() { 
		return mManager;
	}
	
	final void loadSettings(NamedList<Object> data) throws ErrorException {
		if (data != null) { 
			if (LOG.isDebugEnabled())
				LOG.debug("loadSettings: conf=" + this);
			
			List<?> categories = (List<?>)data.get(Setting.CATEGORIES_NAME);
			for (int i=0; categories != null && i < categories.size(); i++) { 
				@SuppressWarnings("unchecked")
				NamedList<Object> category = (NamedList<Object>)categories.get(i);
				if (category != null) { 
					String name = (String)category.get(Setting.NAME_NAME);
					if (name != null) 
						loadSettingCategory(name, category);
				}
			}
		}
	}

	private void loadSettingCategory(String categoryName, 
			NamedList<Object> category) throws ErrorException { 
		//if (LOG.isDebugEnabled())
		//	LOG.debug("loadSettingCategory: categoryName=" + categoryName);
		
		List<?> groups = (List<?>)category.get(Setting.GROUPS_NAME);
		for (int i=0; groups != null && i < groups.size(); i++) { 
			@SuppressWarnings("unchecked")
			NamedList<Object> group = (NamedList<Object>)groups.get(i);
			if (group != null) { 
				String name = (String)group.get(Setting.NAME_NAME);
				if (name != null) 
					loadSettingGroup(categoryName, name, group);
			}
		}
	}
	
	private void loadSettingGroup(String categoryName, String groupName, 
			NamedList<Object> group) throws ErrorException {
		//if (LOG.isDebugEnabled()) {
		//	LOG.debug("loadSettingCategory: categoryName=" + categoryName 
		//			+ " groupName=" + groupName);
		//}
		
		List<?> settings = (List<?>)group.get(Setting.SETTINGS_NAME);
		for (int i=0; settings != null && i < settings.size(); i++) { 
			@SuppressWarnings("unchecked")
			NamedList<Object> setting = (NamedList<Object>)settings.get(i);
			if (group == null) continue;
			
			String name = (String)setting.get(Setting.NAME_NAME);
			String type = (String)setting.get(Setting.TYPE_NAME);
			
			if (name == null) continue;
			
			if (type != null && type.equals(Setting.CHECKBOX_TYPE)) { 
				Object val = setting.get(Setting.CHECKED_NAME);
				loadSettingValue(categoryName, groupName, name, val);
				
			} else { 
				Object val = setting.get(Setting.VALUE_NAME);
				loadSettingValue(categoryName, groupName, name, val);
			}
		}
	}
	
	private void loadSettingValue(String categoryName, String groupName, 
			String name, Object val) throws ErrorException { 
		//if (LOG.isDebugEnabled()) {
		//	LOG.debug("loadSettingValue: categoryName=" + categoryName 
		//			+ " groupName=" + groupName + " name=" + name + " value=" + val);
		//}
		
		loadSetting(categoryName, groupName, name, val);
	}
	
	protected abstract void loadSetting(String categoryName, String groupName, 
			String name, Object val) throws ErrorException;
	
}
