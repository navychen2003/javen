package org.javenstudio.falcon.setting.cluster;

public interface IClusterManager {

	public IHostCluster getCluster(String clusterId);
	public IHostCluster getClusterSelf();
	
}
