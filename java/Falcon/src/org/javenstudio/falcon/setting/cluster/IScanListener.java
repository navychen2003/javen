package org.javenstudio.falcon.setting.cluster;

public interface IScanListener {

	public void onHostFound(HostCluster cluster, IHostNode host);
	
}
