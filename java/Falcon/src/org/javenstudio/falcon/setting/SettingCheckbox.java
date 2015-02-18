package org.javenstudio.falcon.setting;

import org.javenstudio.falcon.util.NamedList;

public class SettingCheckbox extends Setting {

	private boolean mChecked = false;
	
	public SettingCheckbox(SettingManager manager, String name) { 
		this(manager, name, null);
	}
	
	public SettingCheckbox(SettingManager manager, String name, String value) { 
		super(manager, name, value);
	}

	@Override
	public String getType() {
		return Setting.CHECKBOX_TYPE;
	}
	
	public boolean isChecked() { return mChecked; }
	public void setChecked(boolean b) { mChecked = b; }
	
	@Override
	protected void toNamedList(NamedList<Object> result) { 
		super.toNamedList(result);
		
		result.add(Setting.CHECKED_NAME, isChecked());
	}
	
}
