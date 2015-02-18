package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;

final class AnyboxHost {
	private static final Logger LOG = Logger.getLogger(AnyboxHost.class);
	
	public static class HostClusterData {
		private final String mClusterId;
		private String mClusterDomain;
		private String mMailDomain;
		private int mHostCount;
		private HostNodeData[] mHosts;
		
		private HostClusterData(String id) {
			if (id == null) throw new NullPointerException();
			mClusterId = id;
		}
		
		public String getClusterId() { return mClusterId; }
		public String getClusterDomain() { return mClusterDomain; }
		public String getMailDomain() { return mMailDomain; }
		public int getHostCount() { return mHostCount; }
		public HostNodeData[] getHosts() { return mHosts; }
	}
	
	public static class HostNodeData implements IHostInfo {
		private final String mClusterId;
		private final String mHostKey;
		private HostMode mMode;
		private String mAttachUsers;
		private String mClusterDomain;
		private String mMailDomain;
		private String mAdmin;
		private String mDomain;
		private String mHostAddr;
		private String mHostName;
		private String mLanAddr;
		private int mHttpPort;
		private int mHttpsPort;
		private long mHeartbeat;
		private int mStatus;
		private int mHostHash;
		private int mHostCount;
		private boolean mSelf;
		
		private HostNodeData(String id, String key) {
			if (id == null || key == null) throw new NullPointerException();
			mClusterId = id;
			mHostKey = key;
		}
		
		public HostMode getHostMode() { return mMode; }
		public String getClusterId() { return mClusterId; }
		public String getClusterDomain() { return mClusterDomain; }
		public String getMailDomain() { return mMailDomain; }
		public String getHostKey() { return mHostKey; }
		public String getAdminUser() { return mAdmin; }
		public String getHostDomain() { return mDomain; }
		public String getHostAddress() { return mHostAddr; }
		public String getHostName() { return mHostName; }
		public String getLanAddress() { return mLanAddr; }
		public int getHttpPort() { return mHttpPort; }
		public int getHttpsPort() { return mHttpsPort; }
		public long getHeartbeatTime() { return mHeartbeat; }
		public int getStatusCode() { return mStatus; }
		public int getHostHash() { return mHostHash; }
		public int getHostCount() { return mHostCount; }
		public boolean isSelf() { return mSelf; }
		
		@Override
		public String getAttachUserNames(String hostkey, int count) { 
			return mAttachUsers; 
		}
		
		@Override
		public String toString() {
			return "HostNode{clusterId=" + mClusterId +",clusterDomain=" + mClusterDomain 
					+ ",mailDomain=" + mMailDomain + ",hostKey=" + mHostKey 
					+ ",admin=" + mAdmin + ",domain=" + mDomain + ",hostAddr=" + mHostAddr 
					+ ",hostName=" + mHostName + ",lanAddr=" + mLanAddr 
					+ ",httpPort=" + mHttpPort + ",httpsPort=" + mHttpsPort 
					+ ",heartbeat=" + mHeartbeat + ",status=" + mStatus + ",mode=" + mMode
					+ ",hash=" + mHostHash + ",hostcount=" + mHostCount 
					+ ",self=" + mSelf + "}";
		}
	}
	
	static class HostGetData implements IHeartbeatData {
		private final HostNodeData mHostSelf;
		private final Map<String,Map<String,HostNodeData>> mHosts;
		private final AnyboxUser.UserData[] mUsers;
		private final long mRequestTime = System.currentTimeMillis();
		
		public HostGetData(Map<String,Map<String,HostNodeData>> hosts, HostNodeData hostSelf, 
				AnyboxUser.UserData[] users) {
			if (hosts == null || hostSelf == null) throw new NullPointerException();
			mHostSelf = hostSelf;
			mHosts = hosts;
			mUsers = users;
		}
		
		public HostNodeData getHost(String clusterId, String hostKey) {
			if (clusterId == null || hostKey == null) return null;
			
			Map<String,HostNodeData> map = mHosts.get(clusterId);
			if (map != null) return map.get(hostKey);
			
			return null;
		}
		
		public HostNodeData getHostSelf() { return mHostSelf; }
		public AnyboxUser.UserData[] getUsers() { return mUsers; }
		public long getRequestTime() { return mRequestTime; }
	}
	
	static HostGetData loadGetData(AnyboxData data) throws IOException {
		if (data == null) return null;
		
		HostNodeData hostSelf = loadHost(data.get("hostself"));
		HostClusterData[] clusters = loadClusters(data.get("clusters"));
		AnyboxUser.UserData[] users = AnyboxUser.loadUsers(data.get("users"));
		
		Map<String,Map<String,HostNodeData>> hosts = 
				new HashMap<String,Map<String,HostNodeData>>();
		
		if (hostSelf == null) throw new IOException("Missing host self");
		addHost(hosts, hostSelf);
		
		if (clusters != null) {
			for (HostClusterData cluster : clusters) {
				if (cluster == null) continue;
				HostNodeData[] list = cluster.getHosts();
				if (list != null) {
					for (HostNodeData host : list) {
						addHost(hosts, host);
					}
				}
			}
		}
		
		return new HostGetData(hosts, hostSelf, users);
	}
	
	private static void addHost(Map<String,Map<String,HostNodeData>> hosts, HostNodeData host) {
		if (hosts == null || host == null) return;
		
		Map<String,HostNodeData> map = hosts.get(host.getClusterId());
		if (map == null) {
			map = new HashMap<String,HostNodeData>();
			hosts.put(host.getClusterId(), map);
		}
		
		map.put(host.getHostKey(), host);
	}
	
	static class HostJoinData {
		private final HostNodeData mHostSelf;
		private final HostNodeData mHost;
		private final HostClusterData mCluster;
		
		public HostJoinData(HostNodeData hostSelf, HostNodeData host, HostClusterData cluster) {
			if (hostSelf == null || host == null) throw new NullPointerException();
			mHostSelf = hostSelf;
			mHost = host;
			mCluster = cluster;
		}
		
		public HostNodeData getHostSelf() { return mHostSelf; }
		public HostNodeData getHost() { return mHost; }
		public HostClusterData getCluster() { return mCluster; }
	}
	
	static HostJoinData loadJoinData(AnyboxData data) throws IOException {
		if (data == null) return null;
		
		HostNodeData hostSelf = loadHost(data.get("hostself"));
		HostNodeData host = loadHost(data.get("host"));
		HostClusterData cluster = loadCluster(data.get("cluster"));
		
		if (hostSelf == null) throw new IOException("Missing host self");
		if (host == null) throw new IOException("Missing host");
		
		return new HostJoinData(hostSelf, host, cluster);
	}
	
	static class HostAttachData {
		private final HostNodeData mHostSelf;
		private final HostNodeData mHost;
		private final HostClusterData mCluster;
		private final HostNodeData[] mAttachHosts;
		
		public HostAttachData(HostNodeData hostSelf, HostNodeData host, 
				HostClusterData cluster, HostNodeData[] attachHosts) {
			if (hostSelf == null || host == null || attachHosts == null) 
				throw new NullPointerException();
			mHostSelf = hostSelf;
			mHost = host;
			mCluster = cluster;
			mAttachHosts = attachHosts;
		}
		
		public HostNodeData getHostSelf() { return mHostSelf; }
		public HostNodeData getHost() { return mHost; }
		public HostClusterData getCluster() { return mCluster; }
		public HostNodeData[] getAttachHosts() { return mAttachHosts; }
	}
	
	static HostAttachData loadAttachData(AnyboxData data) throws IOException {
		if (data == null) return null;
		
		HostNodeData hostSelf = loadHost(data.get("hostself"));
		HostNodeData host = loadHost(data.get("host"));
		HostClusterData cluster = loadCluster(data.get("cluster"));
		HostNodeData[] attachHosts = loadHosts(data.get("attachhosts"));
		
		if (hostSelf == null) throw new IOException("Missing host self");
		if (host == null) throw new IOException("Missing host");
		if (attachHosts == null) throw new IOException("Missing attach hosts");
		
		return new HostAttachData(hostSelf, host, cluster, attachHosts);
	}
	
	private static HostClusterData[] loadClusters(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		String[] names = data.getNames();
		ArrayList<HostClusterData> list = new ArrayList<HostClusterData>();
		
		if (names != null) {
			for (String name : names) {
				HostClusterData cluster = loadCluster(data.get(name));
				if (cluster != null) list.add(cluster);
			}
		}
		
		return list.toArray(new HostClusterData[list.size()]);
	}
	
	private static HostClusterData loadCluster(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadCluster: data=" + data);
		
		String clusterid = data.getString("clusterid");
		if (clusterid == null) return null;
		
		HostClusterData cluster = new HostClusterData(clusterid);
		cluster.mClusterDomain = data.getString("clusterdomain");
		cluster.mMailDomain = data.getString("maildomain");
		cluster.mHostCount = data.getInt("hostcount", 0);
		cluster.mHosts = loadHosts(data.get("hosts"));
		
		return cluster;
	}
	
	private static HostNodeData[] loadHosts(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		String[] names = data.getNames();
		ArrayList<HostNodeData> list = new ArrayList<HostNodeData>();
		
		if (names != null) {
			for (String name : names) {
				HostNodeData host = loadHost(data.get(name));
				if (host != null) list.add(host);
			}
		}
		
		return list.toArray(new HostNodeData[list.size()]);
	}
	
	private static HostNodeData loadHost(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadHost: data=" + data);
		
		String clusterid = data.getString("clusterid");
		String key = data.getString("key");
		if (clusterid == null || key == null)
			return null;
		
		HostNodeData host = new HostNodeData(clusterid, key);
		host.mAttachUsers = data.getString("attachusers");
		host.mClusterDomain = data.getString("clusterdomain");
		host.mMailDomain = data.getString("maildomain");
		host.mAdmin = data.getString("admin");
		host.mDomain = data.getString("domain");
		host.mHostAddr = data.getString("hostaddr");
		host.mHostName = data.getString("hostname");
		host.mLanAddr = data.getString("lanaddr");
		host.mHttpPort = data.getInt("httpport", 80);
		host.mHttpsPort = data.getInt("httpsport", 443);
		host.mHeartbeat = data.getLong("heartbeat", 0);
		host.mStatus = data.getInt("status", 0);
		host.mHostHash = data.getInt("hashcode", 0);
		host.mSelf = data.getBool("self", false);
		
		host.mMode = HostHelper.parseMode(data.getString("mode"));
		host.mHostCount = data.getInt("hostcount", 0);
		
		return host;
	}
	
}
