package org.javenstudio.lightning.core;

import java.net.URI;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.cluster.ClusterHelper;
import org.javenstudio.falcon.setting.cluster.HostMode;
import org.javenstudio.falcon.setting.cluster.IAuthInfo;
import org.javenstudio.falcon.setting.cluster.IClusterManager;
import org.javenstudio.falcon.setting.cluster.IFetchListener;
import org.javenstudio.falcon.setting.cluster.HostManager;
import org.javenstudio.falcon.setting.cluster.HostSelf;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.setting.cluster.IHostInfo;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.setting.cluster.IScanListener;
import org.javenstudio.falcon.util.IParams;
import org.javenstudio.lightning.http.HttpHelper;
import org.javenstudio.lightning.http.IHttpResult;
import org.javenstudio.lightning.http.SimpleHttpListener;
import org.javenstudio.raptor.conf.Configuration;

public class CoreCluster extends ClusterHelper implements IClusterManager {
	private static final Logger LOG = Logger.getLogger(CoreCluster.class);
	
	private final CoreContainers mContainers;
	private final HostSelf mHostSelf;
	private final HostManager mManager;
	
	public CoreCluster(final CoreContainers containers) throws ErrorException { 
		if (containers == null) throw new NullPointerException();
		mContainers = containers;
		mHostSelf = containers.getSetting().getGlobal().newHostSelf();
		mManager = new HostManager() {
				@Override
				public void fetchUri(String location, IFetchListener listener) {
					CoreCluster.fetch(location, listener);
				}
				@Override
				public HostSelf getHostSelf() {
					return mHostSelf;
				}
				@Override
				public Configuration getConf() {
					return containers.getConfiguration();
				}
			};
		mManager.addHost(mHostSelf);
	}
	
	public CoreContainers getContainers() { return mContainers; }
	public HostSelf getHostSelf() { return mHostSelf; }
	
	public String getHostTooltip() {
		StringBuilder sbuf = new StringBuilder();
		sbuf.append("Anybox ");
		if (getHostSelf().getHostMode() != HostMode.HOST)
			sbuf.append(getHostSelf().getHostMode().toString());
		sbuf.append(" host: ");
		sbuf.append(getHostSelf().getHostName());
		sbuf.append("(");
		sbuf.append(getHostSelf().getHostAddress());
		sbuf.append(":");
		sbuf.append(getHostSelf().getHttpPort());
		sbuf.append(")");
		return sbuf.toString();
	}
	
	public IHostCluster getClusterSelf() {
		IHostCluster cluster = getHostSelf().getCluster();
		if (cluster != null) return cluster;
		return getCluster(getHostSelf().getClusterId());
	}
	
	public synchronized void scanClusters(IScanListener listener) throws ErrorException {
		synchronized (mManager) { 
			mManager.scanClusters(listener);
		}
	}
	
	public synchronized IHostCluster[] getClusters() { 
		synchronized (mManager) { 
			return mManager.getClusters();
		}
	}
	
	public synchronized IHostCluster getCluster(String clusterId) {
		if (clusterId == null || clusterId.length() == 0)
			return null;
		
		synchronized (mManager) { 
			return mManager.getCluster(clusterId);
		}
	}
	
	public synchronized void addHost(IHostNode node) throws ErrorException {
		if (node == null) return;
		
		synchronized (mManager) { 
			mManager.addHost(node);
			mManager.requestPong(node);
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mManager.close();
	}
	
	public synchronized void joinHost() {
		String joinAddress = getContainers().getAdminConfig().getJoinAddress();
		if (joinAddress == null || joinAddress.length() == 0)
			return;
		
		if (joinAddress.equalsIgnoreCase(getHostSelf().getHostAddress()) || 
			joinAddress.equalsIgnoreCase(getHostSelf().getLanAddress()) || 
			joinAddress.equalsIgnoreCase(getHostSelf().getHostDomain())) {
			if (LOG.isWarnEnabled())
				LOG.warn("joinHost: cannot join to self: " + getHostSelf() 
						+ " joinAddress=" + joinAddress);
			return;
		}
		
		HostMode mode = getHostSelf().getHostMode();
		if (mode == HostMode.ATTACH) {
			mManager.addAttachAddress(
					getContainers().getAdminConfig().getJoinAddress(), 
					getContainers().getAdminConfig().getAttachUsers());
			mManager.requestAttachHosts(getHostSelf());
		} else {
			mManager.requestJoin(getHostSelf(), 
					getContainers().getAdminConfig().getJoinAddress());
		}
	}
	
	public IHostInfo getAttachHost(String attachHostKey) throws ErrorException {
		return mManager.getAttachHost(attachHostKey);
	}
	
	public IAuthInfo attachAuth(IParams req) throws ErrorException {
		return mManager.attachAuth(req);
	}
	
	public IHostInfo[] getJoinHosts() {
		HostMode mode = getHostSelf().getHostMode();
		if (mode == HostMode.ATTACH)
			return mManager.getAttachHosts();
		else //if (mode == HostMode.JOIN || mode == HostMode.NAMED || mode == HostMode.HOST)
			return mManager.getJoinHosts();
	}
	
	public static void fetch(String location, IFetchListener listener) {
		try {
			SimpleHttpListener sl = new SimpleHttpListener();
			HttpHelper.fetchURL(URI.create(location), sl);
			
			IHttpResult result = sl.getResult();
			if (listener != null) {
				listener.onContentFetched(result.getContentAsString(), 
						sl.getException());
			}
		} catch (Throwable e) {
			if (listener != null)
				listener.onContentFetched(null, e);
		}
	}
	
}
