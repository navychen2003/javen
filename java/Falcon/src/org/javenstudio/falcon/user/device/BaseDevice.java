package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.setting.SettingTable;

abstract class BaseDevice extends Device {

	private final BaseDeviceType mType;
	private String mLanguage;
	
	public BaseDevice(BaseDeviceType type, String key, 
			String name, String ver) throws ErrorException { 
		super(key, name, ver);
		if (type == null) throw new NullPointerException();
		mType = type;
		mLanguage = "";
	}
	
	public BaseDeviceType getType() { return mType; }
	
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
