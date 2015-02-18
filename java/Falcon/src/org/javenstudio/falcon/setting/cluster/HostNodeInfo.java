package org.javenstudio.falcon.setting.cluster;

public class HostNodeInfo extends HostNodeBase {

	private final IHostInfo mNode;
	
	private IHeartbeatData mHeartbeatData = null;
	private long mHeartbeatTime = System.currentTimeMillis();
	private int mStatusCode = 0;
	
	private int mPingTimes = 0;
	private int mPingFailed = 0;
	
	public HostNodeInfo(IHostInfo node) {
		if (node == null) throw new NullPointerException();
		mNode = node;
		mHeartbeatTime = node.getHeartbeatTime();
		mStatusCode = node.getStatusCode();
	}
	
	public final String getClusterId() { 
		IHostCluster cluster = getCluster();
		if (cluster != null) return cluster.getClusterId();
		return mNode.getClusterId(); 
	}
	
	public final String getClusterDomain() { 
		IHostCluster cluster = getCluster();
		if (cluster != null) return cluster.getDomain();
		return mNode.getClusterDomain(); 
	}
	
	public final String getMailDomain() { 
		IHostCluster cluster = getCluster();
		if (cluster != null) return cluster.getMailDomain();
		return mNode.getMailDomain(); 
	}
	
	public final String getClusterSecret() {
		return null;
	}
	
	public final HostMode getHostMode() { return mNode.getHostMode(); }
	public final String getHostKey() { return mNode.getHostKey(); }
	public final int getHostHash() { return mNode.getHostHash(); }
	
	public final String getHostDomain() { return mNode.getHostDomain(); }
	public final String getHostAddress() { return mNode.getHostAddress(); }
	public final String getHostName() { return mNode.getHostName(); }
	public final String getLanAddress() { return mNode.getLanAddress(); }
	
	public final int getHttpPort() { return mNode.getHttpPort(); }
	public final int getHttpsPort() { return mNode.getHttpsPort(); }
	
	public final String getAdminUser() { return mNode.getAdminUser(); }
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
	
}
