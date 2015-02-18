package org.javenstudio.falcon.setting.cluster;

import java.util.ArrayList;
import java.util.TreeMap;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserName;
import org.javenstudio.falcon.user.UserName;

public abstract class HostCluster implements IHostCluster {
	private static final Logger LOG = Logger.getLogger(HostCluster.class);

	private final HostManager mManager;
	private final String mClusterId;
	private String mDomain = null;
	private String mMailDomain = null;
	private String mSecret = null;
	private long mHeartbeatTimeout;
	
	private final TreeMap<String,IHostNode> mHostMap = 
			new TreeMap<String,IHostNode>();
	
	//private final TreeSet<IHostNode> mHostSet = 
	//		new TreeSet<IHostNode>(new Comparator<IHostNode>() {
	//			@Override
	//			public int compare(IHostNode o1, IHostNode o2) {
	//				if (o1 == null || o2 == null) {
	//					if (o1 == null && o2 == null) return 0;
	//					if (o1 == null) return -1;
	//					return 1;
	//				}
	//				int hash1 = o1.getHostHash();
	//				int hash2 = o2.getHostHash();
	//				return hash1 > hash2 ? 1 : (hash1 < hash2 ? -1 : 0);
	//			}
	//		});
	
	public HostCluster(HostManager manager, String clusterId) {
		if (manager == null || clusterId == null) throw new NullPointerException();
		mClusterId = clusterId;
		mManager = manager;
		mHeartbeatTimeout = HostHelper.getLong(manager.getConf(), 
				"cluster.host.heartbeat.timeout", HostHelper.HEARTBEAT_TIMEOUT);
	}
	
	public HostManager getManager() { return mManager; }
	public String getClusterId() { return mClusterId; }
	
	public String getDomain() { return mDomain; }
	public void setDomain(String domain) { mDomain = domain; }
	
	public String getMailDomain() { return mMailDomain; }
	public void setMailDomain(String domain) { mMailDomain = domain; }
	
	public String getSecret() { return mSecret; }
	public void setSecret(String secret) { mSecret = secret; }
	
	public void scanHosts(IScanListener listener) throws ErrorException {
		if (LOG.isDebugEnabled()) LOG.debug("scanHosts: cluster=" + this);
		synchronized (mHostMap) { 
			IHostNode[] hosts = getHosts();
			if (hosts != null) {
				for (IHostNode host : hosts) {
					if (host == null) continue;
					
					if (host.getStatusCode() == IHostNode.STATUS_UNKNOWN ||
						host.getStatusCode() == IHostNode.STATUS_OK) {
						if (!host.isSelf() && System.currentTimeMillis() - host.getHeartbeatTime() > 
								mHeartbeatTimeout) {
							if (LOG.isInfoEnabled())
								LOG.info("scanHosts: heartbeat timeout, host=" + host);
							removeHost(host, "heartbeat timeout");
							continue;
						}
					} else if (!host.isSelf()) {
						if (LOG.isInfoEnabled())
							LOG.info("scanHosts: error host, host=" + host);
						removeHost(host, "error host");
						continue;
					}
					
					if (listener != null)
						listener.onHostFound(this, host);
				}
			}
		}
	}
	
	public void removeHost(IHostNode node, String reason) throws ErrorException { 
		if (node == null) return;
		
		synchronized (mHostMap) { 
			final String key = node.getHostKey();
			if (key == null || key.length() == 0) {
				//throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				//		"wrong host key: " + key);
				return;
			}
			
			IHostNode n = mHostMap.remove(key);
			if (n == null) return;
			
			//mHostSet.remove(n);
			//if (n2 != n) {
			//	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
			//			"wrong host key: " + n.getHostKey() + " and hash: " + n.getHostHash());
			//}
			
			if (LOG.isInfoEnabled())
				LOG.info("removeHost: host=" + n + " reason=" + reason);
			
			n.onRemoved();
		}
	}
	
	public void addHost(IHostNode node) throws ErrorException { 
		if (node == null) return;
		
		synchronized (mHostMap) { 
			final String key = node.getHostKey();
			if (key == null || key.length() == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"wrong host key: " + key);
			}
			
			IHostNode n = mHostMap.get(key);
			if (n == node) return;
			
			if (n != null && n instanceof HostSelf) {
				//throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				//		"Host node: " + key + " is self");
				
				if (LOG.isWarnEnabled())
					LOG.warn("addHost: ignore self host=" + n);
				
				return;
			}
			
			if (n != null && n != node) { 
				//throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				//		"Host node: " + key + " already existed");
				
				if (LOG.isDebugEnabled())
					LOG.debug("addHost: replaced host=" + n);
				
				//mHostSet.remove(n);
			}
			
			mHostMap.put(key, node);
			//mHostSet.add(node);
			
			if (node.getHostMode() == HostMode.HOST || node.getHostMode() == HostMode.NAMED) {
				String domain = node.getClusterDomain();
				String mailDomain = node.getMailDomain();
				String secret = node.getClusterSecret();
				
				if (domain != null && domain.length() > 0) setDomain(domain);
				if (mailDomain != null && mailDomain.length() > 0) setMailDomain(mailDomain);
				if (secret != null && secret.length() > 0) setSecret(secret);
			}
			
			node.setCluster(this);
			
			if (LOG.isInfoEnabled())
				LOG.info("addHost: host=" + node);
		}
	}
	
	public IHostNode getHostByKey(String key) { 
		if (key == null) return null;
		
		synchronized (mHostMap) { 
			return mHostMap.get(key);
		}
	}
	
	public IHostNode[] getNamedHosts() {
		return getNamedHosts(null);
	}
	
	public IHostNode[] getNamedHosts(String ignoreKey) {
		ArrayList<IHostNode> list = new ArrayList<IHostNode>();
		
		synchronized (mHostMap) { 
			for (IHostNode host : mHostMap.values()) {
				if (host == null) continue;
				if (host.getHostMode() == HostMode.HOST || 
					host.getHostMode() == HostMode.NAMED) {
					if (ignoreKey == null || !ignoreKey.equals(host.getHostKey()))
						list.add(host);
				}
			}
		}
		
		return list.toArray(new IHostNode[list.size()]);
	}
	
	public IHostNode selectNamedHost(String ignoreKey) {
		IHostNode[] hosts = getNamedHosts(ignoreKey);
		if (hosts == null || hosts.length == 0)
			return null;
		
		if (hosts.length == 1) {
			return hosts[0];
		}
		
		int idx = (int)System.currentTimeMillis() % hosts.length;
		if (idx >= 0 && idx < hosts.length) {
			IHostNode host =  hosts[idx];
			if (host != null) return host;
		}
		
		for (IHostNode host : hosts) {
			if (host != null) return host;
		}
		
		return null;
	}
	
	@Override
	public IHostList getHostListByHash(int hash) {
		ArrayList<IHostNode> list = new ArrayList<IHostNode>();
		
		synchronized (mHostMap) { 
			for (IHostNode host : mHostMap.values()) {
				if (host == null || host.getHostMode() == HostMode.ATTACH)
					continue;
				list.add(host);
			}
		}
		
		final IHostNode[] hosts = list.toArray(new IHostNode[list.size()]);
		if (hosts == null || hosts.length == 0)
			return null;
		
		int foundIdx = 0;
		
		if (hosts.length > 1) {
			if (hash < 0) hash *= (-1);
			foundIdx = hash % hosts.length;
		}
		
		if (foundIdx < 0 || foundIdx >= hosts.length)
			foundIdx = 0;
		
		final int hostIndex = foundIdx;
		
		return new IHostList() {
				private int mHostIndex = hostIndex;
				@Override
				public IHostCluster getCluster() {
					return HostCluster.this;
				}
				@Override
				public IHostNode getHostAt(int index) {
					return index >= 0 && index < hosts.length ? hosts[index] : null;
				}
				@Override
				public IHostNode currentHost() {
					return getHostAt(mHostIndex);
				}
				@Override
				public IHostNode nextHost() {
					if (mHostIndex >= 0 && mHostIndex < hosts.length-1)
						mHostIndex ++;
					else
						mHostIndex = 0;
					return getHostAt(mHostIndex);
				}
				@Override
				public IHostNode prevHost() {
					if (mHostIndex > 0 && mHostIndex < hosts.length)
						mHostIndex --;
					else
						mHostIndex = hosts.length -1;
					return getHostAt(mHostIndex);
				}
				@Override
				public int getHostCount() {
					return hosts.length;
				}
				@Override
				public int getHostIndex() {
					return mHostIndex;
				}
				@Override
				public String toString() {
					return "HostList{hostnum=" + hosts.length 
							+ ",hostidx=" + getHostIndex() + ",host=" + currentHost() 
							+ "}";
				}
			};
	}
	
	public int getHostCount() { 
		synchronized (mHostMap) { 
			return mHostMap.size();
		}
	}
	
	public IHostNode[] getHosts() { 
		synchronized (mHostMap) { 
			return mHostMap.values().toArray(new IHostNode[mHostMap.size()]);
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close: " + this);
		synchronized (mHostMap) { 
			mHostMap.clear();
			//mHostSet.clear();
		}
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + mClusterId 
				+ ",domain=" + mDomain + "}";
	}
	
	@Override
	public IUserName parseUserName(String name) throws ErrorException {
		return UserName.parse(this, name);
	}
	
}
