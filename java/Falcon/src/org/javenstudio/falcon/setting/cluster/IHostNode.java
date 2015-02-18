package org.javenstudio.falcon.setting.cluster;

public interface IHostNode extends IHostInfo, Comparable<IHostNode> {

	public static final int STATUS_UNKNOWN = 0;
	public static final int STATUS_OK = 1;
	public static final int STATUS_ERROR = 2;
	public static final int STATUS_WRONG = 3;
	public static final int STATUS_JOINERROR = 4;
	public static final int STATUS_ATTACHERROR = 5;
	
	public HostMode getHostMode();
	
	public IHostCluster createCluster(HostManager manager, String clusterId);
	public IHostCluster getCluster();
	public void setCluster(IHostCluster cluster);
	
	public String getHostKey();
	public int getHostHash();
	
	public String getClusterId();
	public String getClusterDomain();
	public String getClusterSecret();
	public String getMailDomain();
	public int getHostCount();
	
	public String getHostDomain();
	public String getHostAddress();
	public String getHostName();
	public String getLanAddress();
	
	public int getHttpPort();
	public int getHttpsPort();
	
	public IHostInfo[] getAttachHosts();
	public IAttachUser[] getAttachUsers(String hostkey);
	public String getAttachUserNames(String hostkey, int count);
	
	public String getAdminUser();
	public boolean isSelf();
	
	public long getHeartbeatTime();
	public void setHeartbeatTime(long time);
	
	public int getStatusCode();
	public void setStatusCode(int code);
	
	public void onRemoved();
	
}
