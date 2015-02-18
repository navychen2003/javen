package org.javenstudio.falcon.setting;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public final class SettingCategory {

	private final List<SettingGroup> mGroups = 
			new ArrayList<SettingGroup>();
	
	private final SettingManager mManager;
	private final String mName;
	
	private String mTitle;
	private String mDesc;
	
	SettingCategory(SettingManager manager, String name) { 
		if (manager == null || name == null) throw new NullPointerException();
		mManager = manager;
		mName = name;
	}
	
	public SettingManager getManager() { return mManager; }
	
	public String getName() { return mName; }
	//public void setName(String name) { mName = name; }
	
	public String getTitle() { return mTitle; }
	public void setTitle(String text) { mTitle = text; }
	
	public String getDescription() { return mDesc; }
	public void setDescription(String text) { mDesc = text; }
	
	public SettingGroup createGroup(String name, 
			SettingGroup.UpdateHandler handler) throws ErrorException { 
		if (name == null || name.length() == 0 || handler == null) 
			return null;
		
		synchronized (mGroups) { 
			for (SettingGroup item : mGroups) { 
				if (name.equals(item.getName())) {
					if (item.getUpdateHandler() != handler) { 
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"Setting group: " + name + " already existed");
					}
					return item;
				}
			}
			
			SettingGroup group = new SettingGroup(this, name, handler);
			mGroups.add(group);
			
			return group;
		}
	}
	
	public SettingGroup getGroup(String name) { 
		if (name == null || name.length() == 0) 
			return null;
		
		synchronized (mGroups) { 
			for (SettingGroup item : mGroups) { 
				if (name.equals(item.getName()))
					return item;
			}
			
			//SettingGroup group = new SettingGroup(this, name);
			//mGroups.add(group);
			
			return null; //group;
		}
	}
	
	public SettingGroup[] getGroups() { 
		synchronized (mGroups) { 
			return mGroups.toArray(new SettingGroup[mGroups.size()]);
		}
	}
	
	public NamedList<Object> toNamedList() { 
		NamedList<Object> result = new NamedMap<Object>();
		
		result.add(Setting.NAME_NAME, Setting.toString(getName()));
		
		if (getManager().isSaveAll()) {
			result.add(Setting.TITLE_NAME, Setting.toString(getTitle()));
			result.add(Setting.DESC_NAME, Setting.toString(getDescription()));
		}
		
		synchronized (mGroups) { 
			ArrayList<Object> list = new ArrayList<Object>();
			
			for (SettingGroup item : mGroups) { 
				list.add(item.toNamedList());
			}
			
			result.add(Setting.GROUPS_NAME, list.toArray(new Object[list.size()]));
		}
		
		return result;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + getName() + "}";
	}
	
}
