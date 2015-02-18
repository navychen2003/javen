package org.javenstudio.provider.account.space;

public interface ITotalSpaceData {

	public long getRemainingSpace();
	public long getFreeSpace();
	public long getTotalSpace();
	public long getUsedSpace();
	public float getUsedPercent();
	
}
