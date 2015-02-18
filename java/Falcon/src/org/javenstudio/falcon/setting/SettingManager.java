package org.javenstudio.falcon.setting;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public abstract class SettingManager implements ISettingManager {
	private static final Logger LOG = Logger.getLogger(SettingManager.class);

	private final List<SettingConf> mSettings = 
			new ArrayList<SettingConf>();
	
	private final List<SettingCategory> mCategories = 
			new ArrayList<SettingCategory>();
	
	public SettingManager() {}
	
	void addSetting(SettingConf setting) { 
		if (setting == null) return;
		
		synchronized (mSettings) { 
			for (SettingConf conf : mSettings) { 
				if (conf == setting) return;
			}
			
			mSettings.add(setting);
		}
	}
	
	protected synchronized void clear() { 
		synchronized (mSettings) { 
			mSettings.clear();
		}
		synchronized (mCategories) { 
			mCategories.clear();
		}
	}
	
	public final SettingCategory createCategory(String name) 
			throws ErrorException { 
		if (name == null || name.length() == 0) 
			return null;
		
		synchronized (mCategories) { 
			for (SettingCategory item : mCategories) { 
				if (name.equals(item.getName()))
					return item;
			}
			
			SettingCategory category = new SettingCategory(this, name);
			mCategories.add(category);
			
			if (LOG.isDebugEnabled())
				LOG.debug("createCategory: category=" + category);
			
			return category;
		}
	}
	
	public final SettingCategory getCategory(String name) { 
		if (name == null || name.length() == 0) 
			return null;
		
		synchronized (mCategories) { 
			for (SettingCategory item : mCategories) { 
				if (name.equals(item.getName()))
					return item;
			}
			
			//SettingCategory category = new SettingCategory(this, name);
			//mCategories.add(category);
			
			return null; //category;
		}
	}
	
	public SettingCategory[] getCategories() { 
		synchronized (mCategories) { 
			return mCategories.toArray(new SettingCategory[mCategories.size()]);
		}
	}
	
	public synchronized final void saveSettings() throws ErrorException { 
		saveSettingsConf(toNamedList());
	}
	
	public synchronized final void loadSettings() throws ErrorException { 
		NamedList<Object> items = loadSettingsConf();
		synchronized (mSettings) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("loadSettings: data=" + items 
						+ " settings=" + mSettings.size());
			}
			
			for (SettingConf setting : mSettings) { 
				setting.loadSettings(items);
			}
		}
	}
	
	protected abstract void saveSettingsConf(NamedList<Object> items) throws ErrorException;
	protected abstract NamedList<Object> loadSettingsConf() throws ErrorException;
	
	public final NamedList<Object> toNamedList() { 
		NamedList<Object> result = new NamedMap<Object>();
		
		synchronized (mCategories) { 
			ArrayList<Object> list = new ArrayList<Object>();
			
			for (SettingCategory item : mCategories) { 
				list.add(item.toNamedList());
			}
			
			result.add(Setting.CATEGORIES_NAME, list.toArray(new Object[list.size()]));
		}
		
		return result;
	}
	
	public boolean isSaveAll() { return true; }
	
}
