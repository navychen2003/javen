package org.javenstudio.falcon.setting;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public final class SettingGroup {

	public static interface UpdateHandler { 
		public boolean updateSetting(SettingGroup group, Object input) 
				throws ErrorException;
	}
	
	private final List<Setting> mSettings = 
			new ArrayList<Setting>();
	
	private final SettingCategory mCategory;
	private final String mName;
	
	private final UpdateHandler mHandler;
	private String mTitle = null;
	private String mDesc = null;
	
	public SettingGroup(SettingCategory category, 
			String name, UpdateHandler handler) { 
		if (category == null || name == null || handler == null) 
			throw new NullPointerException();
		mCategory = category;
		mName = name;
		mHandler = handler;
	}
	
	public SettingCategory getCategory() { return mCategory; }
	
	public String getName() { return mName; }
	//public void setName(String name) { mName = name; }
	
	public String getTitle() { return mTitle; }
	public void setTitle(String text) { mTitle = text; }
	
	public String getDescription() { return mDesc; }
	public void setDescription(String text) { mDesc = text; }
	
	//public void setUpdateHandler(UpdateHandler handler) { mHandler = handler; }
	public UpdateHandler getUpdateHandler() { return mHandler; }
	
	public Setting[] getSettings() { 
		synchronized (mSettings) { 
			return mSettings.toArray(new Setting[mSettings.size()]);
		}
	}
	
	public Setting getSetting(String name) { 
		if (name == null) return null;
		
		synchronized (mSettings) { 
			for (Setting item : mSettings) { 
				if (name.equals(item.getName())) 
					return item;
			}
			return null;
		}
	}
	
	public void addSetting(Setting setting) { 
		if (setting == null) return;
		
		if (setting.getManager() != getCategory().getManager())
			throw new IllegalArgumentException("Setting has wrong manager");
		
		synchronized (mSettings) { 
			for (Setting item : mSettings) { 
				if (item == setting) return;
			}
			
			mSettings.add(setting);
		}
	}
	
	public void removeSetting(Setting setting) { 
		if (setting == null) return;
		
		synchronized (mSettings) { 
			for (int i=0; i < mSettings.size(); ) { 
				Setting item = mSettings.get(i);
				if (item == null || item == setting) { 
					mSettings.remove(i);
					continue;
				}
				i ++;
			}
		}
	}
	
	public void removeSetting(String name) { 
		if (name == null) return;
		
		synchronized (mSettings) { 
			for (int i=0; i < mSettings.size(); ) { 
				Setting item = mSettings.get(i);
				if (item == null || name.equals(item.getName())) { 
					mSettings.remove(i);
					continue;
				}
				i ++;
			}
		}
	}
	
	public NamedList<Object> toNamedList() { 
		NamedList<Object> result = new NamedMap<Object>();
		
		result.add(Setting.NAME_NAME, Setting.toString(getName()));
		
		if (getCategory().getManager().isSaveAll()) {
			result.add(Setting.TITLE_NAME, Setting.toString(getTitle()));
			result.add(Setting.DESC_NAME, Setting.toString(getDescription()));
		}
		
		synchronized (mSettings) { 
			ArrayList<Object> list = new ArrayList<Object>();
			
			for (Setting item : mSettings) { 
				list.add(item.toNamedList());
			}
			
			result.add(Setting.SETTINGS_NAME, list.toArray(new Object[list.size()]));
		}
		
		return result;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + getName() 
				+ ",handler=" + mHandler + "}";
	}
	
}
