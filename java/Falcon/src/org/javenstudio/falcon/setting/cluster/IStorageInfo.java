package org.javenstudio.falcon.setting.cluster;

public interface IStorageInfo {

	public IHostNode getHostNode();
	public IHostUser getStorageUser();
	public ILibraryInfo[] getStorageLibraries();
	public long getRequestTime();
	
}
