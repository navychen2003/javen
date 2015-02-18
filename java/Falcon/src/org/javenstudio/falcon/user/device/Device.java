package org.javenstudio.falcon.user.device;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.util.TimeUtils;
import org.javenstudio.falcon.setting.SettingTable;
import org.javenstudio.falcon.user.UserHelper;

public abstract class Device implements SettingTable.SettingRow {

	//public static final String DEV_UNKNOWN = "unknown";
	//public static final String DEV_UNKNOWN_TITLE = "Unknown";
	//public static final String DEV_WEB = "web";
	//public static final String DEV_WEB_TITLE = "Web Browser";
	//public static final String DEV_ANDROID = "android";
	//public static final String DEV_ANDROID_TITLE = "Android Application";
	//public static final String DEV_IOS = "ios";
	//public static final String DEV_IOS_TITLE = "iPhone/iPad Application";
	//public static final String DEV_MACOS = "macos";
	//public static final String DEV_MACOS_TITLE = "Mac Application";
	//public static final String DEV_WP = "wp";
	//public static final String DEV_WP_TITLE = "Windows Phone Application";
	//public static final String DEV_WINDOWS = "windows";
	//public static final String DEV_WINDOWS_TITLE = "Windows Application";
	//public static final String DEV_LINUX = "linux";
	//public static final String DEV_LINUX_TITLE = "Linux Application";
	
	public static final long EXPIRED_MILLIS = 90l * 24 * 60 * 60 * 1000;
	
	private final String mKey;
	private final String mName;
	private final String mVersion;
	
	private String mClientKey = null;
	private String mAuthKey = null;
	private String mUserAgent = null;
	private String mUserAddr = null;
	private String mAction = null;
	private long mActionTime = 0;
	
	protected Device(String key, String name, String ver) 
			throws ErrorException { 
		if (name == null || ver == null) throw new NullPointerException();
		if (key == null || key.length() == 0) {
			key = UserHelper.newDeviceKey(getClass().getName() 
					+ "{name=" + name + ",version=" + ver + "}");
		}
		mKey = key;
		mName = name;
		mVersion = ver;
	}
	
	public abstract DeviceType getType();
	
	public final String getKey() { return mKey; }
	public final String getName() { return mName; }
	public final String getVersion() { return mVersion; }
	
	public String getClientKey() { return mClientKey; }
	public void setClientKey(String val) { mClientKey = val; }
	
	public String getAuthKey() { return mAuthKey; }
	public void setAuthKey(String val) { mAuthKey = val; }
	
	public String getUserAgent() { return mUserAgent; }
	public void setUserAgent(String val) { mUserAgent = val; }
	
	public String getUserAddr() { return mUserAddr; }
	public void setUserAddr(String addr) { mUserAddr = addr; }
	
	public String getAction() { return mAction; }
	public void setAction(String action) { mAction = action; }
	
	public long getActionTime() { return mActionTime; }
	public void setActionTime(long time) { mActionTime = time; }
	
	@Override
	public boolean equals(Object obj) { 
		if (obj == this) return true;
		if (obj == null || !(obj instanceof Device)) 
			return false;
		
		Device other = (Device)obj;
		return this.getKey().equals(other.getKey()); 
	}
	
	@Override
	public void putFields(IDatabase.Row row) throws ErrorException { 
		if (row == null) return;
		
		SettingTable.addAttr(row, SettingTable.KEY_QUALIFIER, getKey());
		SettingTable.addAttr(row, SettingTable.NAME_QUALIFIER, getName());
		SettingTable.addAttr(row, SettingTable.TYPE_QUALIFIER, "device");
		SettingTable.addAttr(row, SettingTable.CATEGORY_QUALIFIER, getType().getName());
		SettingTable.addAttr(row, SettingTable.VERSION_QUALIFIER, getVersion());
		SettingTable.addAttr(row, SettingTable.CLIENTKEY_QUALIFIER, getClientKey());
		SettingTable.addAttr(row, SettingTable.AUTHKEY_QUALIFIER, getAuthKey());
		SettingTable.addAttr(row, SettingTable.AGENT_QUALIFIER, getUserAgent());
		SettingTable.addAttr(row, SettingTable.IPADDR_QUALIFIER, getUserAddr());
		SettingTable.addAttr(row, SettingTable.ACTION_QUALIFIER, getAction());
		SettingTable.addAttr(row, SettingTable.MTIME_QUALIFIER, getActionTime());
	}
	
	@Override
	public void getFields(IDatabase.Result res) throws ErrorException { 
		if (res == null) return;
		
		setClientKey(SettingTable.getAttrString(res, SettingTable.CLIENTKEY_QUALIFIER));
		setAuthKey(SettingTable.getAttrString(res, SettingTable.AUTHKEY_QUALIFIER));
		setUserAgent(SettingTable.getAttrString(res, SettingTable.AGENT_QUALIFIER));
		setUserAddr(SettingTable.getAttrString(res, SettingTable.IPADDR_QUALIFIER));
		setAction(SettingTable.getAttrString(res, SettingTable.ACTION_QUALIFIER));
		setActionTime(SettingTable.getAttrLong(res, SettingTable.MTIME_QUALIFIER));
	}
	
	public final String toReadableTitle() {
		return getType().getName() + "/" + getName();
	}
	
	public final String toReadableString() { 
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("Class: ").append(toString(getClass().getSimpleName())).append("\n");
		sbuf.append("DeviceKey: ").append(toString(getKey())).append("\n");
		sbuf.append("DeviceName: ").append(toString(getName())).append("\n");
		sbuf.append("DeviceType: ").append(toString(getType().getName())).append("\n");
		sbuf.append("DeviceVersion: ").append(toString(getVersion())).append("\n");
		sbuf.append("UserAgent: ").append(toString(getUserAgent())).append("\n");
		sbuf.append("RemoteAddr: ").append(toString(getUserAddr())).append("\n");
		sbuf.append("Action: ").append(toString(getAction())).append("\n");
		sbuf.append("ActionTime: ").append(TimeUtils.formatDate(getActionTime())).append("\n");
		sbuf.append("ClientKey: ").append(toString(getClientKey())).append("\n");
		sbuf.append("AuthKey: ").append(toString(getAuthKey())).append("\n");
		addReadableFields(sbuf);
		return sbuf.toString();
	}
	
	public static String toString(CharSequence val) { 
		if (val != null) { 
			if (val instanceof String) 
				return (String)val;
			//if (val instanceof CharSequence)
				return val.toString();
		}
		return "";
	}
	
	protected void addReadableFields(StringBuffer sbuf) {}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{type=" + getType() 
				+ ",name=" + mName + ",version=" + mVersion + "}";
	}
	
}
