package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.account.AccountInfo;

public interface AccountData extends AccountInfo {

	public static final int FLAG_OK = 0;
	
	public static final int STATUS_LOGOUT = 0;
	public static final int STATUS_LOGIN = 1;
	public static final int STATUS_ERROR = 2;
	
	public long getId();
	public long getHostId();
	public String getPrefix();
	public String getFullName();
	
	public String getUserKey();
	public String getUserName();
	public String getNickName();
	public String getMailAddress();
	public String getType();
	public String getCategory();
	public String getAvatar();
	public String getBackground();
	public String getEmail();
	public String getToken();
	public String getAuthKey();
	public String getDeviceKey();
	public String getClientKey();
	
	public long getUsedSpace();
	public long getUsableSpace();
	public long getFreeSpace();
	public long getPurchased();
	public long getCapacity();
	
	public int getFlag();
	public int getStatus();
	public int getFailedCode();
	public String getFailedMessage();
	public long getCreateTime();
	public long getUpdateTime();
	public long getKeygenTime();
	
	public AccountData startUpdate();
	public long commitUpdates();
	public void commitDelete();
	
	public void setPrefix(String text);
	public void setHostId(long id);
	
	public void setUserKey(String text);
	public void setUserName(String text);
	public void setNickName(String text);
	public void setMailAddress(String text);
	public void setType(String text);
	public void setCategory(String text);
	public void setAvatar(String text);
	public void setBackground(String text);
	public void setEmail(String text);
	public void setToken(String text);
	public void setAuthKey(String text);
	public void setDeviceKey(String text);
	public void setClientKey(String text);
	
	public void setUsedSpace(long val);
	public void setUsableSpace(long val);
	public void setFreeSpace(long val);
	public void setPurchased(long val);
	public void setCapacity(long val);
	
	public void setFlag(int flag);
	public void setStatus(int status);
	public void setFailedCode(int code);
	public void setFailedMessage(String text);
	public void setCreateTime(long time);
	public void setUpdateTime(long time);
	public void setKeygenTime(long time);
	
}
