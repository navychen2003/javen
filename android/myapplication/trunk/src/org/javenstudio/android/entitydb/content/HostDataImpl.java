package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.THost;
import org.javenstudio.android.entitydb.THostUpdater;

final class HostDataImpl extends AbstractContentImpl 
		implements HostData {

	private final THost mEntity; 
	
	HostDataImpl(THost data) { 
		this(data, false); 
	}
	
	HostDataImpl(THost data, boolean updateable) { 
		super(updateable); 
		mEntity = data; 
	}
	
	THost getEntity() { return mEntity; }
	
	@Override
	public String getDisplayName() {
		String name = getDomain();
		if (name == null || name.length() == 0) {
			String addr = getLanAddr();
			if (addr == null || addr.length() == 0)
				addr = getHostAddr();
			name = getHostName() + "/" + addr;
		}
		return name;
	}
	
	@Override
	public String getRequestAddressPort() {
		String addr = getHostAddr();
		int port = getHttpPort();
		if (port > 0 && port != 80)
			addr = addr + ":" + port;
		return addr;
	}
	
	@Override
	public long getId() { 
		return mEntity.getId(); 
	}
	
	@Override
	public String getPrefix() { 
		return mEntity.prefix; 
	}
	
	@Override
	public String getClusterId() { 
		return mEntity.clusterid; 
	}
	
	@Override
	public String getClusterDomain() { 
		return mEntity.clusterdomain; 
	}
	
	@Override
	public String getMailDomain() { 
		return mEntity.maildomain; 
	}
	
	@Override
	public String getHostKey() { 
		return mEntity.hostkey; 
	}
	
	@Override
	public String getHostAddr() { 
		return mEntity.hostaddr; 
	}
	
	@Override
	public String getHostName() { 
		return mEntity.hostname; 
	}
	
	@Override
	public String getLanAddr() { 
		return mEntity.lanaddr; 
	}
	
	@Override
	public String getDomain() { 
		return mEntity.domain; 
	}
	
	@Override
	public int getHttpPort() { 
		return toInt(mEntity.httpport);
	}
	
	@Override
	public int getHttpsPort() { 
		return toInt(mEntity.httpsport);
	}
	
	@Override
	public String getAdmin() { 
		return mEntity.admin;
	}
	
	@Override
	public String getTitle() { 
		return mEntity.title;
	}
	
	@Override
	public String getTagline() { 
		return mEntity.tagline;
	}
	
	@Override
	public String getPoster() { 
		return mEntity.poster;
	}
	
	@Override
	public String getBackground() { 
		return mEntity.background;
	}
	
	@Override
	public int getFlag() { 
		return toInt(mEntity.flag);
	}
	
	@Override
	public int getStatus() { 
		return toInt(mEntity.status);
	}
	
	@Override
	public int getFailedCode() { 
		return toInt(mEntity.failedCode);
	}
	
	@Override
	public String getFailedMessage() { 
		return mEntity.failedMessage;
	}
	
	@Override
	public long getHeartbeat() { 
		return toLong(mEntity.heartbeat);
	}
	
	@Override
	public long getCreateTime() { 
		return toLong(mEntity.createTime);
	}
	
	@Override
	public long getUpdateTime() { 
		return toLong(mEntity.updateTime);
	}
	
	@Override 
	public HostData startUpdate() { 
		THost data = new THost(mEntity.getId()); 
		
		return new HostDataImpl(data, true); 
	}
	
	@Override 
    public synchronized long commitUpdates() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			THostUpdater provider = new THostUpdater(mEntity); 
			provider.commitUpdate();
			return provider.getHostKey();
		}
    }
	
	@Override 
    public synchronized void commitDelete() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			THostUpdater provider = new THostUpdater(mEntity); 
			provider.commitDelete();
		}
    }
	
	@Override 
	public void setPrefix(String text) { 
		checkUpdateable(); 
		mEntity.prefix = text; 
	}
	
	@Override 
	public void setClusterId(String text) { 
		checkUpdateable(); 
		mEntity.clusterid = text; 
	}
	
	@Override 
	public void setClusterDomain(String text) { 
		checkUpdateable(); 
		mEntity.clusterdomain = text; 
	}
	
	@Override 
	public void setMailDomain(String text) { 
		checkUpdateable(); 
		mEntity.maildomain = text; 
	}
	
	@Override 
	public void setHostKey(String text) { 
		checkUpdateable(); 
		mEntity.hostkey = text; 
	}
	
	@Override 
	public void setHostAddr(String text) { 
		checkUpdateable(); 
		mEntity.hostaddr = text; 
	}
	
	@Override 
	public void setHostName(String text) { 
		checkUpdateable(); 
		mEntity.hostname = text; 
	}
	
	@Override 
	public void setLanAddr(String text) { 
		checkUpdateable(); 
		mEntity.lanaddr = text; 
	}
	
	@Override 
	public void setDomain(String text) { 
		checkUpdateable(); 
		mEntity.domain = text; 
	}
	
	@Override 
	public void setHttpPort(int port) { 
		checkUpdateable(); 
		mEntity.httpport = port;
	}
	
	@Override 
	public void setHttpsPort(int port) { 
		checkUpdateable(); 
		mEntity.httpsport = port;
	}
	
	@Override 
	public void setAdmin(String text) { 
		checkUpdateable(); 
		mEntity.admin = text;
	}
	
	@Override 
	public void setTitle(String text) { 
		checkUpdateable(); 
		mEntity.title = text;
	}
	
	@Override 
	public void setTagline(String text) { 
		checkUpdateable(); 
		mEntity.tagline = text;
	}
	
	@Override 
	public void setPoster(String text) { 
		checkUpdateable(); 
		mEntity.poster = text;
	}
	
	@Override 
	public void setBackground(String text) { 
		checkUpdateable(); 
		mEntity.background = text;
	}
	
	@Override 
	public void setFlag(int flag) { 
		checkUpdateable(); 
		mEntity.flag = flag;
	}
	
	@Override 
	public void setStatus(int status) { 
		checkUpdateable(); 
		mEntity.status = status;
	}
	
	@Override 
	public void setFailedCode(int code) { 
		checkUpdateable(); 
		mEntity.failedCode = code;
	}
	
	@Override
	public void setFailedMessage(String text) { 
		checkUpdateable(); 
		mEntity.failedMessage = text;
	}
	
	@Override
	public void setHeartbeat(long time) { 
		checkUpdateable(); 
		mEntity.heartbeat = time;
	}
	
	@Override
	public void setCreateTime(long time) { 
		checkUpdateable(); 
		mEntity.createTime = time;
	}
	
	@Override
	public void setUpdateTime(long time) { 
		checkUpdateable(); 
		mEntity.updateTime = time;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + getId() + ",prefix=" + getPrefix()
				+ ",clusterid=" + getClusterId() + ",clusterdomain=" + getClusterDomain() 
				+ ",hostaddr=" + getHostAddr() + ",hostkey=" + getHostKey() 
				+ "}";
	}
	
}
