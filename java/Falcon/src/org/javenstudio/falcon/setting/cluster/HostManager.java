package org.javenstudio.falcon.setting.cluster;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;

public abstract class HostManager extends HostHelper {
	private static final Logger LOG = Logger.getLogger(HostManager.class);

	private final HostJob mJob;
	private volatile boolean mClosed = false;
	
	private final Map<String,IHostCluster> mClusterMap = 
			new HashMap<String,IHostCluster>();
	
	public HostManager() {
		mJob = new HostJob(this);
	}
	
	public HostJob getJob() { return mJob; }
	public abstract HostSelf getHostSelf();
	
	public void removeHost(IHostNode node, String reason) throws ErrorException { 
		if (node == null) return;
		
		synchronized (mClusterMap) { 
			final String clusterId = node.getClusterId();
			if (clusterId == null || clusterId.length() == 0) {
				//throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				//		"wrong clusterid: " + clusterId);
				return;
			}
			
			IHostCluster cluster = mClusterMap.get(clusterId);
			if (cluster == null) return;
			
			cluster.removeHost(node, reason);
			
			if (cluster.getHostCount() <= 0)
				removeCluster(cluster.getClusterId());
		}
	}
	
	public void addHost(IHostNode node) throws ErrorException { 
		if (node == null) return;
		
		synchronized (mClusterMap) { 
			final String clusterId = node.getClusterId();
			if (clusterId == null || clusterId.length() == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"wrong clusterid: " + clusterId);
			}
			
			IHostCluster cluster = mClusterMap.get(clusterId);
			if (cluster == null) { 
				cluster = node.createCluster(this, clusterId);
				mClusterMap.put(clusterId, cluster);
				
				if (LOG.isDebugEnabled())
					LOG.debug("addHost: new cluster=" + cluster);
			}
			
			cluster.addHost(node);
		}
	}
	
	public IHostCluster removeCluster(String clusterId) {
		if (clusterId == null) return null;
		
		synchronized (mClusterMap) { 
			IHostCluster cluster = mClusterMap.remove(clusterId);
			
			if (cluster != null) {
				if (LOG.isInfoEnabled())
					LOG.info("removeCluster: cluster=" + cluster);
			}
			
			return cluster;
		}
	}
	
	public IHostCluster getCluster(String clusterId) { 
		if (clusterId == null) return null;
		
		synchronized (mClusterMap) { 
			return mClusterMap.get(clusterId);
		}
	}
	
	public int getClusterCount() { 
		synchronized (mClusterMap) { 
			return mClusterMap.size();
		}
	}
	
	public IHostCluster[] getClusters() { 
		synchronized (mClusterMap) {
			return mClusterMap.values().toArray(new IHostCluster[mClusterMap.size()]);
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
		
		IHostCluster[] clusters = getClusters();
		if (clusters != null) { 
			for (IHostCluster cluster : clusters) { 
				if (cluster != null) cluster.close();
			}
		}
	}
	
	public boolean isClosed() { return mClosed; }
	
	public void scanClusters(IScanListener listener) throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("scanClusters");
		synchronized (mClusterMap) {
			IHostCluster[] clusters = getClusters();
			if (clusters != null) {
				for (IHostCluster cluster : clusters) {
					if (cluster == null) continue;
					cluster.scanHosts(listener);
					
					if (cluster.getHostCount() <= 0)
						removeCluster(cluster.getClusterId());
				}
			}
		}
	}
	
}
