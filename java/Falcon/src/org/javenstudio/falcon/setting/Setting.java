package org.javenstudio.falcon.setting;

import java.util.Date;

import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public abstract class Setting {

	public static final String CATEGORIES_NAME = "categories";
	public static final String GROUPS_NAME = "groups";
	public static final String SETTINGS_NAME = "settings";
	public static final String OPTIONS_NAME = "options";
	
	public static final String TEXT_TYPE = "text";
	public static final String SELECT_TYPE = "select";
	public static final String CHECKBOX_TYPE = "checkbox";
	
	public static final String NAME_NAME = "name";
	public static final String TYPE_NAME = "type";
	public static final String VALUE_NAME = "value";
	public static final String TITLE_NAME = "title";
	public static final String DESC_NAME = "desc";
	public static final String CHECKED_NAME = "checked";
	public static final String UPDATETIME = "updateTime";
	
	private final SettingManager mManager;
	
	private String mName;
	private String mValue;
	private String mTitle;
	private String mDesc;
	private long mUpdateTime = 0;
	
	public Setting(SettingManager manager, String name) { 
		this(manager, name, null);
	}
	
	public Setting(SettingManager manager, String name, String value) { 
		if (manager == null || name == null) throw new NullPointerException();
		mManager = manager;
		mName = name;
		mValue = value;
	}
	
	public SettingManager getManager() { return mManager; }
	
	public abstract String getType();
	
	public String getName() { return mName; }
	public void setName(String name) { mName = name; }
	
	public String getValue() { return mValue; }
	public void setValue(String value) { mValue = value; }
	
	public String getTitle() { return mTitle; }
	public void setTitle(String text) { mTitle = text; }
	
	public String getDescription() { return mDesc; }
	public void setDescription(String text) { mDesc = text; }
	
	public long getUpdateTime() { return mUpdateTime; }
	public void setUpdateTime(long time) { mUpdateTime = time; }
	public void setUpdateTime() { setUpdateTime(System.currentTimeMillis()); }
	
	public final NamedList<Object> toNamedList() { 
		NamedList<Object> result = new NamedMap<Object>();
		toNamedList(result);
		
		return result;
	}
	
	protected void toNamedList(NamedList<Object> result) { 
		result.add(Setting.NAME_NAME, Setting.toString(getName()));
		result.add(Setting.VALUE_NAME, Setting.toString(getValue()));
		
		if (getManager().isSaveAll()) {
			result.add(Setting.TYPE_NAME, Setting.toString(getType()));
			result.add(Setting.TITLE_NAME, Setting.toString(getTitle()));
			result.add(Setting.DESC_NAME, Setting.toString(getDescription()));
			
			long time = getUpdateTime();
			if (time > 0)
				result.add(Setting.UPDATETIME, new Date(time));
		}
	}
	
	public static String toString(Object o) { 
		if (o == null) return "";
		if (o instanceof String) return (String)o;
		if (o instanceof CharSequence) return ((CharSequence)o).toString();
		return o.toString();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{name=" + getName() + "}";
	}
	
}
