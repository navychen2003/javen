package org.javenstudio.falcon.setting;

public class SettingText extends Setting {

	public SettingText(SettingManager manager, String name) { 
		this(manager, name, null);
	}
	
	public SettingText(SettingManager manager, String name, String value) { 
		super(manager, name, value);
	}

	@Override
	public String getType() {
		return Setting.TEXT_TYPE;
	}
	
}
