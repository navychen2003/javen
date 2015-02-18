package org.javenstudio.android.account;

public interface AccountInfo {

	public String getUserKey();
	public String getUserName();
	public String getNickName();
	public String getFullName();
	public String getType();
	public String getCategory();
	public String getAvatar();
	public String getBackground();
	public String getEmail();
	
	public long getUsedSpace();
	public long getUsableSpace();
	public long getFreeSpace();
	public long getPurchased();
	public long getCapacity();
	
}
