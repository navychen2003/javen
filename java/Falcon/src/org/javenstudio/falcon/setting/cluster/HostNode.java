package org.javenstudio.falcon.setting.cluster;

import org.javenstudio.falcon.ErrorException;

public class HostNode extends HostNodeBase {

	private final HostMode mMode;
	private final String mHostKey;
	private final int mHostHash;
	private final String mClusterId;
	private final String mClusterDomain;
	private final String mClusterSecret;
	private final String mMailDomain;
	private final String mHostDomain;
	private final String mHostAddress;
	private final String mHostName;
	private final String mAdminUser;
	private final String mLanAddress;
	private final int mHttpPort;
	private final int mHttpsPort;
	
	private IHeartbeatData mHeartbeatData = null;
	private long mHeartbeatTime = System.currentTimeMillis();
	private int mStatusCode = 0;
	
	private int mPingTimes = 0;
	private int mPingFailed = 0;
	
	public HostNode(HostMode mode, String clusterId, String clusterDomain, String clusterSecret,
			String mailDomain, String hostDomain, String hostAddress, String hostName, 
			int httpPort, int httpsPort, String adminUser, String hostKey, int hostHash, 
			String lanAddr) throws ErrorException {
		if (mode == null || isEmpty(clusterId, hostAddress, hostName, hostKey))
			throw new IllegalArgumentException("wrong host arguments");
		mMode = mode;
		mClusterId = clusterId;
		mClusterDomain = clusterDomain;
		mClusterSecret = clusterSecret;
		mMailDomain = mailDomain;
		mHostDomain = hostDomain;
		mHostAddress = hostAddress;
		mHostName = hostName;
		mHttpPort = httpPort;
		mHttpsPort = httpsPort;
		mAdminUser = adminUser;
		mLanAddress = lanAddr;
		mHostKey = hostKey; 
		mHostHash = hostHash;
	}
	
	public final String getClusterId() { 
		IHostCluster cluster = getCluster();
		if (cluster != null) return cluster.getClusterId();
		return mClusterId; 
	}
	
	public final String getClusterDomain() { 
		IHostCluster cluster = getCluster();
		if (cluster != null) return cluster.getDomain();
		return mClusterDomain; 
	}
	
	public final String getMailDomain() { 
		IHostCluster cluster = getCluster();
		if (cluster != null) return cluster.getMailDomain();
		return mMailDomain; 
	}
	
	public final String getClusterSecret() {
		return mClusterSecret; 
	}
	
	public final HostMode getHostMode() { return mMode; }
	public final String getHostKey() { return mHostKey; }
	public final int getHostHash() { return mHostHash; }
	
	public final String getHostDomain() { return mHostDomain; }
	public final String getHostAddress() { return mHostAddress; }
	public final String getHostName() { return mHostName; }
	public final String getLanAddress() { return mLanAddress; }
	
	public final int getHttpPort() { return mHttpPort; }
	public final int getHttpsPort() { return mHttpsPort; }
	
	public final String getAdminUser() { return mAdminUser; }
	public final boolean isSelf() { return false; }
	
	public IHeartbeatData getHeartbeatData() { return mHeartbeatData; }
	public void setHeartbeatData(IHeartbeatData data) { mHeartbeatData = data; }
	
	public long getHeartbeatTime() { return mHeartbeatTime; }
	public void setHeartbeatTime(long time) { mHeartbeatTime = time; }
	
	public int getStatusCode() { return mStatusCode; }
	public void setStatusCode(int code) { mStatusCode = code; }
	
	public void setPingTimes(int val) { mPingTimes = val; }
	public int getPingTimes() { return mPingTimes; }
	
	public void setPingFailed(int val) { mPingFailed = val; }
	public int getPingFailed() { return mPingFailed; }
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + getHostKey() + ",hash=" + getHostHash()
				+ ",clusterId=" + getClusterId() + ",clusterDomain=" + getClusterDomain() 
				+ ",mailDomain=" + getMailDomain() + ",hostDomain=" + getHostDomain() 
				+ ",hostAddress=" + getHostAddress() + ",hostName=" + getHostName() 
				+ ",httpPort=" + getHttpPort() + ",httpsPort=" + getHttpsPort() 
				+ ",isself=" + isSelf() + ",heartbeatTime=" + getHeartbeatTime() 
				+ ",statusCode=" + getStatusCode() + ",lanAddress=" + getLanAddress() 
				+ ",pingTimes=" + mPingTimes + ",pingFailed=" + mPingFailed + "}";
	}
	
	public static boolean isEmpty(CharSequence... texts) {
		if (texts != null) {
			for (CharSequence text : texts) {
				if (text == null || text.length() == 0)
					return true;
			}
		}
		return false;
	}
	
}
