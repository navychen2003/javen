package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.setting.SettingTable;

public class WebDevice extends Device {

	private final WebDeviceType mType;
	private String mLanguage;
	
	public WebDevice(WebDeviceType type, String key, 
			String name, String ver) throws ErrorException { 
		super(key, name, ver);
		if (type == null) throw new NullPointerException();
		mType = type;
		mLanguage = "";
	}
	
	public WebDeviceType getType() { return mType; }
	
	public String getLanguage() { return mLanguage; }
	public void setLanguage(String lang) { mLanguage = lang; }
	
	@Override
	public void putFields(IDatabase.Row row) throws ErrorException { 
		if (row == null) return;
		super.putFields(row);
		
		SettingTable.addAttr(row, SettingTable.LANG_QUALIFIER, getLanguage());
	}
	
	@Override
	public void getFields(IDatabase.Result res) throws ErrorException { 
		if (res == null) return;
		super.getFields(res);
		
		setLanguage(SettingTable.getAttrString(res, SettingTable.LANG_QUALIFIER));
	}
	
	@Override
	protected void addReadableFields(StringBuffer sbuf) {
		sbuf.append("Language: ").append(getLanguage()).append("\n");
	}
	
}
