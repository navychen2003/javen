package org.javenstudio.falcon.setting.cluster;

import org.javenstudio.raptor.conf.Configuration;

public abstract class HostSelf extends HostNodeBase {

	private long mHeartbeatTime = System.currentTimeMillis();
	private int mStatusCode = IHostNode.STATUS_OK;
	private int mPingTimes = 0;
	private int mPingFailed = 0;
	
	public abstract void setHostAddress(String addr);
	public abstract String getLanAddress();
	
	public final boolean isSelf() { return true; }
	
	public long getHeartbeatTime() { return mHeartbeatTime; }
	public void setHeartbeatTime(long time) { mHeartbeatTime = time; }
	
	public int getStatusCode() { return mStatusCode; }
	public void setStatusCode(int code) { mStatusCode = code; }
	
	public int increatePingTimes() { return ++mPingTimes; }
	public int getPingTimes() { return mPingTimes; }
	
	public int increatePingFailed() { return ++mPingFailed; }
	public int getPingFailed() { return mPingFailed; }
	public void resetPingFailed() { mPingFailed = 0; }
	
	public long getJoinSleepMillis(Configuration conf) {
		int code = getStatusCode();
		if (code == IHostNode.STATUS_OK)
			return HostHelper.getLong(conf, "cluster.join.intervaltime", HostHelper.JOIN_INTERVAL_TIME);
		
		if (code == IHostNode.STATUS_UNKNOWN && mPingFailed < 10)
			return HostHelper.getLong(conf, "cluster.join.intervaltime", HostHelper.JOIN_INTERVAL_TIME);
		
		return 0;
	}
	
	public long getAttachSleepMillis(Configuration conf) {
		int code = getStatusCode();
		if (code == IHostNode.STATUS_OK)
			return HostHelper.getLong(conf, "cluster.attach.intervaltime", HostHelper.ATTACH_INTERVAL_TIME);
		
		int retryTimes = HostHelper.getInt(conf, "cluster.attach.retrytimes", 20);
		if (retryTimes <= 0) retryTimes = 20;
		
		if (code == IHostNode.STATUS_UNKNOWN && mPingFailed < retryTimes)
			return HostHelper.getLong(conf, "cluster.attach.intervaltime", HostHelper.ATTACH_INTERVAL_TIME);
		
		return 0;
	}
	
	@Override
	public IHostCluster createCluster(HostManager manager, String clusterId) {
		return new HostClusterSelf(manager, clusterId, this);
	}
	
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
