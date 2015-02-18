package org.javenstudio.falcon.setting.cluster;

import java.util.Map;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserName;

public abstract class HostNodeBase implements IHostNode {

	public String getLanAddress() { return getHostAddress(); }
	public long getHeartbeatTime() { return System.currentTimeMillis(); }
	public int getStatusCode() { return STATUS_OK; }
	public void onRemoved() {}
	
	private IHostCluster mCluster = null;
	private int mHostCount = 1;
	
	public final IHostCluster getCluster() { return mCluster; }
	public final void setCluster(IHostCluster cluster) { mCluster = cluster; }
	
	public int getHostCount() { 
		IHostCluster cluster = getCluster();
		if (cluster != null) return cluster.getHostCount();
		return mHostCount;
	}
	
	public void setHostCount(int count) {
		mHostCount = count;
	}
	
	@Override
	public IHostCluster createCluster(HostManager manager, String clusterId) {
		if (clusterId == null) throw new NullPointerException();
		return new HostCluster(manager, clusterId) {
				@Override
				public IHostUserData searchUser(String username) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostNameData searchName(String name) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostUserData addUser(String username, String userkey, String hostkey, 
						String password, int flag, int type, Map<String,String> attrs) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostUserData updateUser(String username, String hostkey, String password, 
						int flag, Map<String,String> attrs) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostUserData removeUser(String username) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostNameData updateName(String name, String value, String hostkey, 
						int flag, Map<String,String> attrs, String oldname) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostNameData removeName(String name) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostList getHostListByName(String name) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
				@Override
				public IHostUserName getHostUserName(IUserName uname) throws ErrorException {
					throw new java.lang.UnsupportedOperationException();
				}
			};
	}
	
	@Override
	public String getAttachUserNames(String hostkey, int count) {
		return null;
	}
	
	@Override
	public IAttachUser[] getAttachUsers(String hostkey) {
		return null;
	}
	
	@Override
	public IHostInfo[] getAttachHosts() {
		return null;
	}
	
	@Override
	public final int compareTo(IHostNode o) {
		return compare(this, o);
	}
	
	static int compare(IHostNode o1, IHostNode o2) {
		if (o1 == o2) return 0;
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;
		
		boolean self1 = o1.isSelf();
		boolean self2 = o2.isSelf();
		if (self1 == true && self2 == false) return -1;
		if (self1 == false && self2 == true) return 1;
		
		int res = o1.getClusterId().compareTo(o2.getClusterId());
		if (res != 0) return res;
		
		res = o1.getHostAddress().compareTo(o2.getHostAddress());
		if (res != 0) return res;
		
		int httpPort1 = o1.getHttpPort();
		int httpPort2 = o2.getHttpPort();
		if (httpPort1 < httpPort2) return -1;
		if (httpPort1 > httpPort2) return 1;
		
		int httpsPort1 = o1.getHttpsPort();
		int httpsPort2 = o2.getHttpsPort();
		if (httpsPort1 < httpsPort2) return -1;
		if (httpsPort1 > httpsPort2) return 1;
		
		return o1.getHostKey().compareTo(o2.getHostKey());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + getHostKey() + ",hash=" + getHostHash()
				+ ",clusterId=" + getClusterId() + ",clusterDomain=" + getClusterDomain() 
				+ ",mailDomain=" + getMailDomain() + ",hostDomain=" + getHostDomain() 
				+ ",hostAddress=" + getHostAddress() + ",hostName=" + getHostName() 
				+ ",httpPort=" + getHttpPort() + ",httpsPort=" + getHttpsPort() 
				+ ",isself=" + isSelf() + ",heartbeatTime=" + getHeartbeatTime() 
				+ ",statusCode=" + getStatusCode() + ",lanAddress=" + getLanAddress() + "}";
	}
	
}
