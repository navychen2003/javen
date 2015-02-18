package org.javenstudio.falcon.setting.cluster;

public interface IHostUser {

	public String getUserKey();
	public String getUserName();
	public String getUserEmail();
	public String getNickName();
	public String getCategory();
	public String getType();
	public String getFlag();
	public String getIdle();
	
	public long getUsedSpace();
	public long getUsableSpace();
	public long getPurchasedSpace();
	public long getFreeSpace();
	public long getCapacitySpace();
	
	public long getModifiedTime();
	
	public ILibraryInfo[] getLibraries();
	
}
