package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountAuth;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.util.Base64Util;
import org.javenstudio.util.StringUtils;

final class AnyboxHost {
	private static final Logger LOG = Logger.getLogger(AnyboxHost.class);
	
	static void doInit(final AnyboxApp app, final AccountAuth.Callback cb) 
			throws IOException {
		if (app == null) throw new NullPointerException();
		if (LOG.isDebugEnabled()) LOG.debug("doInit");
		
		Map<String,Set<String>> map = new HashMap<String,Set<String>>();
		
		HostData[] hosts = ContentHelper.getInstance().queryHosts();
		if (hosts != null) {
			for (HostData host : hosts) {
				if (host == null) continue;
				String clusterId = host.getClusterId();
				if (clusterId != null && clusterId.length() > 0) {
					Set<String> set = map.get(clusterId);
					if (set == null) {
						set = new HashSet<String>();
						map.put(clusterId, set);
					}
				}
			}
		}
		
		String idkeyParams = "";
		
		for (Map.Entry<String,Set<String>> entry : map.entrySet()) {
			String clusterId = entry.getKey();
			Set<String> set = entry.getValue();
			
			if (clusterId == null || clusterId.length() == 0)
				continue;
			
			StringBuilder sbuf = new StringBuilder();
			sbuf.append(clusterId);
			
			if (set != null && set.size() > 0) {
				for (String hostKey : set) {
					if (hostKey != null && hostKey.length() > 0) {
						sbuf.append('/');
						sbuf.append(hostKey);
					}
				}
			}
			
			idkeyParams += "&secret.idkey=" + StringUtils.URLEncode(
					Base64Util.encodeSecret(sbuf.toString()));
		}
		
		final String url = app.getAnyboxSiteUrl() 
				+ "/lightning/user/cluster?action=get&wt=secretjson"
				+ AnyboxAuth.getInitParams(app) + idkeyParams;
		
		HostListener listener = new HostListener();
		if (cb != null) cb.onRequestStart(app, listener.getAuthAction());
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			error = listener.mError;
			if (error == null || error.getCode() == 0) {
				error = null;
				loadData(app, listener.mData);
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("getHeartbeat: error: " + e, e);
		}
	}
	
	static class HostListener extends AnyboxApi.SecretJSONListener {
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			if (LOG.isDebugEnabled())
				LOG.debug("handleData: data=" + data);
			
			if (error != null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("handleData: response error: " + error, 
							error.getException());
				}
			}
		}

		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.HOST_INIT;
		}
		
		public AccountAuth.Action getAuthAction() {
			return AccountAuth.Action.INIT;
		}
	}
	
	public static class HostGet {
		private final AnyboxApp mApp;
		private final HostNode mHostSelf;
		private final HostCluster[] mClusters;
		
		public HostGet(AnyboxApp app, HostNode self, 
				HostCluster[] clusters) {
			if (app == null) throw new NullPointerException();
			mApp = app;
			mHostSelf = self;
			mClusters = clusters;
		}
		
		public AnyboxApp getApp() { return mApp; }
		public HostNode getHostSelf() { return mHostSelf; }
		public HostCluster[] getClusters() { return mClusters; }
	}
	
	public static class HostCluster {
		private final String mClusterId;
		private String mClusterDomain;
		private String mMailDomain;
		private int mHostCount;
		private HostNode[] mHosts;
		
		private HostCluster(String id) {
			if (id == null) throw new NullPointerException();
			mClusterId = id;
		}
		
		public String getClusterId() { return mClusterId; }
		public String getClusterDomain() { return mClusterDomain; }
		public String getMailDomain() { return mMailDomain; }
		public int getHostCount() { return mHostCount; }
		public HostNode[] getHosts() { return mHosts; }
	}
	
	public static class HostNode {
		private final String mClusterId;
		private final String mHostKey;
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
		private boolean mSelf;
		
		private HostNode(String id, String key) {
			if (id == null || key == null) throw new NullPointerException();
			mClusterId = id;
			mHostKey = key;
		}
		
		public String getClusterId() { return mClusterId; }
		public String getClusterDomain() { return mClusterDomain; }
		public String getMailDomain() { return mMailDomain; }
		public String getHostKey() { return mHostKey; }
		public String getAdmin() { return mAdmin; }
		public String getDomain() { return mDomain; }
		public String getHostAddr() { return mHostAddr; }
		public String getHostName() { return mHostName; }
		public String getLanAddr() { return mLanAddr; }
		public int getHttpPort() { return mHttpPort; }
		public int getHttpsPort() { return mHttpsPort; }
		public long getHeartbeat() { return mHeartbeat; }
		public int getStatus() { return mStatus; }
		public boolean isSelf() { return mSelf; }
	}
	
	private static HostGet loadData(AnyboxApp app, AnyboxData data) 
			throws IOException {
		if (app == null || data == null) return null;
		
		HostNode hostSelf = loadHost(data.get("hostself"));
		HostCluster[] clusters = loadClusters(data.get("clusters"));
		
		Map<String,Map<String,HostNode>> hosts = 
				new HashMap<String,Map<String,HostNode>>();
		
		addHost(hosts, hostSelf);
		if (clusters != null) {
			for (HostCluster cluster : clusters) {
				if (cluster == null) continue;
				HostNode[] list = cluster.getHosts();
				if (list != null) {
					for (HostNode host : list) {
						addHost(hosts, host);
					}
				}
			}
		}
		
		HostData[] hostDatas = ContentHelper.getInstance().queryHosts();
		if (hostDatas != null) {
			for (HostData hostData : hostDatas) {
				updateHostData(hosts, hostData, app);
			}
		}
		
		app.setAccounts(null);
		
		return new HostGet(app, hostSelf, clusters);
	}
	
	private static void updateHostData(Map<String,Map<String,HostNode>> hosts, 
			HostData hostData, AnyboxApp app) {
		if (hosts == null || hostData == null || app == null) return;
		
		//String prefix = hostData.getPrefix();
		
		String clusterId = hostData.getClusterId();
		if (clusterId == null || clusterId.length() == 0)
			return;
		
		String hostKey = hostData.getHostKey();
		if (hostKey == null || hostKey.length() == 0)
			return;
		
		Map<String,HostNode> map = hosts.get(clusterId);
		if (map == null) return;
		
		//HostNode firstHost = null;
		HostNode foundHost = null;
		
		for (HostNode host : map.values()) {
			if (host == null) continue;
			//if (firstHost == null) firstHost = host;
			
			if (clusterId.equals(host.getClusterId()) && 
				hostKey.equals(host.getHostKey())) {
				foundHost = host;
				break;
			}
		}
		
		//if (foundHost == null) foundHost = firstHost;
		if (foundHost == null) return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("updateHostData: hostData=" + hostData 
					+ " foundHost=" + foundHost + " foundStatus=" 
					+ foundHost.getStatus());
		}
		
		if (foundHost.getStatus() != HostData.STATUS_OK)
			return;
		
		updateAppAddress(app, hostData, foundHost);
		
		HostData updateData = hostData.startUpdate();
		
		updateData.setPrefix(AnyboxApp.PREFIX);
		updateData.setClusterId(foundHost.getClusterId());
		updateData.setClusterDomain(foundHost.getClusterDomain());
		updateData.setMailDomain(foundHost.getMailDomain());
		updateData.setHostKey(foundHost.getHostKey());
		updateData.setHostName(foundHost.getHostName());
		updateData.setHostAddr(foundHost.getHostAddr());
		updateData.setLanAddr(foundHost.getLanAddr());
		updateData.setDomain(foundHost.getDomain());
		updateData.setAdmin(foundHost.getAdmin());
		updateData.setHttpPort(foundHost.getHttpPort());
		updateData.setHttpsPort(foundHost.getHttpsPort());
		updateData.setHeartbeat(foundHost.getHeartbeat());
		updateData.setFlag(HostData.FLAG_OK);
		updateData.setStatus(HostData.STATUS_OK);
		updateData.setFailedCode(0);
		updateData.setFailedMessage("");
		updateData.setUpdateTime(System.currentTimeMillis());
		
		updateData.commitUpdates();
	}
	
	private static void updateAppAddress(AnyboxApp app, 
			HostData hostData, HostNode foundHost) {
		if (app == null || hostData == null || foundHost == null)
			return;
		
		String clusterDomain = foundHost.getClusterDomain();
		if (clusterDomain == null || clusterDomain.length() == 0) {
			String addr = foundHost.getLanAddr();
			if (addr == null || addr.length() == 0) 
				addr = foundHost.getHostAddr();
			clusterDomain = foundHost.getHostName() + "/" + addr;
		}
		
		String clusterAddress = foundHost.getHostAddr();
		int httpPort = foundHost.getHttpPort();
		if (httpPort > 0 && httpPort != 80)
			clusterAddress = clusterAddress + ":" + httpPort;
		
		String appDomain = app.getHostDisplayName();
		String appAddress = app.getHostAddressPort();
		
		if ((appDomain != null && appDomain.equals(hostData.getDisplayName())) ||
			(appAddress != null && appAddress.equals(hostData.getRequestAddressPort()))) {
			app.setHostAddressPort(clusterAddress);
			app.setHostDisplayName(clusterDomain);
		}
	}
	
	private static void addHost(Map<String,Map<String,HostNode>> hosts, HostNode host) {
		if (hosts == null || host == null) return;
		
		Map<String,HostNode> map = hosts.get(host.getClusterId());
		if (map == null) {
			map = new HashMap<String,HostNode>();
			hosts.put(host.getClusterId(), map);
		}
		
		map.put(host.getHostKey(), host);
	}
	
	private static HostCluster[] loadClusters(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		String[] names = data.getNames();
		ArrayList<HostCluster> list = new ArrayList<HostCluster>();
		
		if (names != null) {
			for (String name : names) {
				HostCluster cluster = loadCluster(data.get(name));
				if (cluster != null) list.add(cluster);
			}
		}
		
		return list.toArray(new HostCluster[list.size()]);
	}
	
	private static HostCluster loadCluster(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		String clusterid = data.getString("clusterid");
		if (clusterid == null) return null;
		
		HostCluster cluster = new HostCluster(clusterid);
		cluster.mClusterDomain = data.getString("clusterdomain");
		cluster.mMailDomain = data.getString("maildomain");
		cluster.mHostCount = data.getInt("hostcount", 0);
		cluster.mHosts = loadHosts(data.get("hosts"));
		
		return cluster;
	}
	
	private static HostNode[] loadHosts(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		String[] names = data.getNames();
		ArrayList<HostNode> list = new ArrayList<HostNode>();
		
		if (names != null) {
			for (String name : names) {
				HostNode host = loadHost(data.get(name));
				if (host != null) list.add(host);
			}
		}
		
		return list.toArray(new HostNode[list.size()]);
	}
	
	private static HostNode loadHost(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadHost: data=" + data);
		
		String clusterid = data.getString("clusterid");
		String key = data.getString("key");
		String mode = data.getString("mode");
		if (clusterid == null || key == null)
			return null;
		
		if (mode != null && mode.equalsIgnoreCase("attach"))
			return null;
		
		HostNode host = new HostNode(clusterid, key);
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
		host.mSelf = data.getBool("self", false);
		
		return host;
	}
	
}
