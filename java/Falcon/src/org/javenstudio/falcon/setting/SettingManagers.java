package org.javenstudio.falcon.setting;

import java.util.ArrayList;

import org.javenstudio.falcon.ErrorException;

public final class SettingManagers implements ISettingManager {

	private final SettingManager[] mManagers;
	
	public SettingManagers(SettingManager[] managers) { 
		if (managers == null) throw new NullPointerException();
		mManagers = managers;
	}

	@Override
	public SettingCategory[] getCategories() {
		ArrayList<SettingCategory> list = new ArrayList<SettingCategory>();
		for (SettingManager manager : mManagers) { 
			if (manager == null) continue;
			SettingCategory[] categories = manager.getCategories();
			if (categories != null) { 
				for (SettingCategory category : categories) { 
					if (category != null) 
						list.add(category);
				}
			}
		}
		return list.toArray(new SettingCategory[list.size()]);
	}

	@Override
	public SettingCategory getCategory(String name) {
		for (SettingManager manager : mManagers) { 
			if (manager == null) continue;
			SettingCategory category = manager.getCategory(name);
			if (category != null) return category;
		}
		return null;
	}

	@Override
	public void saveSettings() throws ErrorException {
		for (SettingManager manager : mManagers) { 
			if (manager == null) continue;
			manager.saveSettings();
		}
	}
	
}
