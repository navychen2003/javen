package org.javenstudio.falcon.setting.cluster;

public interface IHostList {

	public IHostCluster getCluster();
	public IHostNode getHostAt(int index);
	
	public IHostNode currentHost();
	public IHostNode nextHost();
	public IHostNode prevHost();
	
	public int getHostCount();
	public int getHostIndex();
	
}
