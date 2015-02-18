package org.javenstudio.falcon.setting.cluster;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.util.IParams;
import org.javenstudio.util.StringUtils;

public abstract class ClusterHelper {

	public abstract HostSelf getHostSelf();
	public abstract IHostCluster getClusterSelf();
	public abstract IHostCluster getCluster(String clusterId);
	
	public IHostNode parseHost(IParams params, HostMode joinMode, 
			String remoteAddr) throws ErrorException {
		if (params == null) return null;
		
		String hostAddress = remoteAddr;
		String hostKey = params.getParam("key");
		String hostDomain = params.getParam("domain");
		String lanAddress = params.getParam("lanaddr");
		String hostName = params.getParam("hostname");
		String clusterId = params.getParam("clusterid");
		String clusterDomain = params.getParam("clusterdomain");
		String clusterSecret = params.getParam("clustersecret");
		String mailDomain = params.getParam("maildomain");
		String adminUser = params.getParam("admin");
		String hostMode = params.getParam("mode");
		int pingTimes = parseInt(params.getParam("pingtimes"));
		int pingFailed = parseInt(params.getParam("pingfailed"));
		int hostCount = parseInt(params.getParam("hostcount"));
		
		if (hostAddress == null || hostAddress.length() == 0)
			hostAddress = params.getParam("hostaddr");
		
		int httpPort = 80;
		int httpsPort = 443;
		try {
			httpPort = Integer.parseInt(params.getParam("httpport", "80"));
			httpsPort = Integer.parseInt(params.getParam("httpsport", "443"));
		} catch (Throwable e) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"host params error: " + e, e);
		}
		
		HostMode mode = HostHelper.parseMode(hostMode);
		IAttachUser[] attachUsers = null;
		
		if (joinMode == HostMode.ATTACH) {
			IHostCluster cluster = getCluster(clusterId);
			if (cluster == null || cluster.getHostCount() == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		    			"No cluster: " + clusterId + " found or empty");
			}
			
			String[] attachUserNames = params.getParams("attachuser");
			Map<String,IAttachUser> users = new HashMap<String,IAttachUser>();
			
			if (attachUserNames != null) {
				for (String username : attachUserNames) {
					if (username == null) continue;
					if (users.containsKey(username)) continue;
					IHostUserData user = cluster.searchUser(username);
					if (user == null) {
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				    			"Attach user: " + username + " not found");
					} else {
						users.put(user.getUserName(), new AttachHostUser(user));
					}
				}
			}
			
			if (users.size() == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
		    			"No attach users found");
			}
			
			attachUsers = users.values().toArray(new IAttachUser[users.size()]);
		}
		
		if (joinMode != null) mode = joinMode;
		
		if (mode == HostMode.ATTACH) {
			IHostCluster cluster = getCluster(clusterId);
			StorageNode node = new StorageNode(mode, clusterId, clusterDomain, clusterSecret, 
					mailDomain, hostDomain, hostAddress, hostName, httpPort, httpsPort, 
					adminUser, hostKey, UserHelper.createHostHash(hostKey), 
					lanAddress, attachUsers, cluster);
			
			node.setPingTimes(pingTimes);
			node.setPingFailed(pingFailed);
			node.setHostCount(hostCount);
			
			return node;
		} else {
			HostNode node = new HostNode(mode, clusterId, clusterDomain, clusterSecret, 
					mailDomain, hostDomain, hostAddress, hostName, httpPort, httpsPort, 
					adminUser, hostKey, UserHelper.createHostHash(hostKey), lanAddress);
			
			node.setPingTimes(pingTimes);
			node.setPingFailed(pingFailed);
			node.setHostCount(hostCount);
			
			return node;
		}
	}
	
	public static int parseInt(String str) { 
		try { 
			return Integer.parseInt(StringUtils.trim(str));
		} catch (Throwable e) {
			return 0;
		}
	}
	
	public static long parseLong(String str) { 
		try { 
			return Long.parseLong(StringUtils.trim(str));
		} catch (Throwable e) {
			return 0;
		}
	}
	
	public static String toString(Object o) { 
		if (o == null) return "";
		if (o instanceof String) return (String)o;
		if (o instanceof CharSequence) return ((CharSequence)o).toString();
		return o.toString();
	}
	
	public static String trim(String s) { 
		return StringUtils.trim(s);
	}
	
}
