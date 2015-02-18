package org.javenstudio.android.entitydb.content;

public interface HostData extends IHostData {

	public static final int FLAG_OK = 0;
	public static final int STATUS_UNKNOWN = 0;
	public static final int STATUS_OK = 1;
	public static final int STATUS_ERROR = 2;
	public static final int STATUS_WRONG = 3;
	
	public long getId();
	public String getDisplayName();
	public String getRequestAddressPort();
	
	public String getPrefix();
	public String getClusterId();
	public String getClusterDomain();
	public String getMailDomain();
	
	public String getHostKey();
	public String getHostName();
	public String getHostAddr();
	public String getLanAddr();
	public String getDomain();
	public int getHttpPort();
	public int getHttpsPort();
	
	public String getAdmin();
	public String getTitle();
	public String getTagline();
	public String getPoster();
	public String getBackground();
	
	public int getFlag();
	public int getStatus();
	public int getFailedCode();
	public String getFailedMessage();
	
	public long getHeartbeat();
	public long getCreateTime();
	public long getUpdateTime();
	
	public HostData startUpdate();
	public long commitUpdates();
	public void commitDelete();
	
	public void setPrefix(String text);
	public void setClusterId(String text);
	public void setClusterDomain(String text);
	public void setMailDomain(String text);
	
	public void setHostKey(String text);
	public void setHostName(String text);
	public void setHostAddr(String text);
	public void setLanAddr(String text);
	public void setDomain(String text);
	public void setHttpPort(int port);
	public void setHttpsPort(int port);
	
	public void setAdmin(String text);
	public void setTitle(String text);
	public void setTagline(String text);
	public void setPoster(String text);
	public void setBackground(String text);
	
	public void setFlag(int flag);
	public void setStatus(int status);
	public void setFailedCode(int code);
	public void setFailedMessage(String text);
	
	public void setHeartbeat(long time);
	public void setCreateTime(long time);
	public void setUpdateTime(long time);
	
}
