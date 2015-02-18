package org.javenstudio.provider.account.space;

public interface IUserSpaceData {

	public String getHostName();
	public String getDisplayName();
	public String getCategory();
	public boolean isGroup();
	
	public long getRemainingSpace();
	public long getFreeSpace();
	public long getTotalSpace();
	public long getUsedSpace();
	
}
