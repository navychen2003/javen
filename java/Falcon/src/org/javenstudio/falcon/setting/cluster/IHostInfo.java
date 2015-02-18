package org.javenstudio.falcon.setting.cluster;

public interface IHostInfo {

	public HostMode getHostMode();
	public String getHostKey();
	public int getHostHash();
	
	public String getClusterId();
	public String getClusterDomain();
	public String getMailDomain();
	public int getHostCount();
	
	public String getHostDomain();
	public String getHostAddress();
	public String getHostName();
	public String getLanAddress();
	
	public int getHttpPort();
	public int getHttpsPort();
	
	public String getAttachUserNames(String hostkey, int count);
	public String getAdminUser();
	public boolean isSelf();
	
	public long getHeartbeatTime();
	public int getStatusCode();
	
}
