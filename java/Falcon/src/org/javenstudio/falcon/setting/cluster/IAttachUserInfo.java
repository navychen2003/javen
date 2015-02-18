package org.javenstudio.falcon.setting.cluster;

public interface IAttachUserInfo {

	public String getUserKey();
	public String getUserName();
	public String getUserEmail();
	public String getNickName();
	public String getCategory();
	public String getType();
	public String getFlag();
	public String getIdle();
	public String getToken();
	public String getClient();
	public String getDeviceKey();
	public String getAuthKey();
	
	public long getUsedSpace();
	public long getUsableSpace();
	public long getPurchasedSpace();
	public long getFreeSpace();
	public long getCapacitySpace();
	
	public long getModifiedTime();
	
}
