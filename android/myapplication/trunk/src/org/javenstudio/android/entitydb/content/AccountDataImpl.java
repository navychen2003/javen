package org.javenstudio.android.entitydb.content;

import org.javenstudio.android.entitydb.TAccount;
import org.javenstudio.android.entitydb.TAccountUpdater;

final class AccountDataImpl extends AbstractContentImpl 
		implements AccountData {

	private final TAccount mEntity; 
	
	AccountDataImpl(TAccount data) { 
		this(data, false); 
	}
	
	AccountDataImpl(TAccount data, boolean updateable) { 
		super(updateable); 
		mEntity = data; 
	}
	
	TAccount getEntity() { return mEntity; }
	
	@Override
	public long getId() { 
		return mEntity.getId(); 
	}
	
	@Override
	public String getPrefix() { 
		return mEntity.prefix; 
	}
	
	@Override
	public long getHostId() { 
		return toLong(mEntity.hostid); 
	}
	
	@Override
	public String getUserKey() { 
		return mEntity.userkey;
	}
	
	@Override
	public String getUserName() { 
		return mEntity.username;
	}
	
	@Override
	public String getNickName() { 
		return mEntity.nickname;
	}
	
	@Override
	public String getMailAddress() { 
		return mEntity.mailaddress;
	}
	
	@Override
	public String getFullName() {
		HostData host = ContentHelper.getInstance().getHost(getHostId());
		if (host != null) {
			String maildomain = host.getMailDomain();
			if (maildomain != null && maildomain.length() > 0)
				return getUserName() + "@" + maildomain;
			
			String domain = host.getClusterDomain();
			if (domain != null && domain.length() > 0)
				return getUserName() + "@" + domain;
			
			String hostname = host.getHostName();
			if (hostname != null && hostname.length() > 0)
				return getUserName() + "@" + hostname;
		}
		
		return getMailAddress();
	}
	
	@Override
	public String getAvatar() { 
		return mEntity.avatar;
	}
	
	@Override
	public String getBackground() { 
		return mEntity.background;
	}
	
	@Override
	public String getEmail() { 
		return mEntity.email;
	}
	
	@Override
	public String getType() { 
		return mEntity.type;
	}
	
	@Override
	public String getCategory() { 
		return mEntity.category;
	}
	
	@Override
	public String getToken() { 
		return mEntity.token;
	}
	
	@Override
	public String getAuthKey() { 
		return mEntity.authkey;
	}
	
	@Override
	public String getDeviceKey() { 
		return mEntity.devicekey;
	}
	
	@Override
	public String getClientKey() { 
		return mEntity.clientkey;
	}
	
	@Override
	public long getUsedSpace() { 
		return toLong(mEntity.usedspace);
	}
	
	@Override
	public long getUsableSpace() { 
		return toLong(mEntity.usablespace);
	}
	
	@Override
	public long getFreeSpace() { 
		return toLong(mEntity.freespace);
	}
	
	@Override
	public long getPurchased() { 
		return toLong(mEntity.purchased);
	}
	
	@Override
	public long getCapacity() { 
		return toLong(mEntity.capacity);
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
	public long getCreateTime() { 
		return toLong(mEntity.createTime);
	}
	
	@Override
	public long getUpdateTime() { 
		return toLong(mEntity.updateTime);
	}
	
	@Override
	public long getKeygenTime() { 
		return toLong(mEntity.keygenTime);
	}
	
	@Override 
	public AccountData startUpdate() { 
		TAccount data = new TAccount(mEntity.getId()); 
		
		return new AccountDataImpl(data, true); 
	}
	
	@Override 
    public synchronized long commitUpdates() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TAccountUpdater provider = new TAccountUpdater(mEntity); 
			provider.commitUpdate();
			return provider.getAccountKey();
		}
    }
	
	@Override 
    public synchronized void commitDelete() { 
		checkUpdateable(); 
		
		synchronized (sContentLock) { 
			TAccountUpdater provider = new TAccountUpdater(mEntity); 
			provider.commitDelete();
		}
    }
	
	@Override 
	public void setPrefix(String text) { 
		checkUpdateable(); 
		mEntity.prefix = text; 
	}
	
	@Override 
	public void setHostId(long val) { 
		checkUpdateable(); 
		mEntity.hostid = val;
	}
	
	@Override 
	public void setUserKey(String text) { 
		checkUpdateable(); 
		mEntity.userkey = text;
	}
	
	@Override 
	public void setUserName(String text) { 
		checkUpdateable(); 
		mEntity.username = text;
	}
	
	@Override 
	public void setNickName(String text) { 
		checkUpdateable(); 
		mEntity.nickname = text;
	}
	
	@Override 
	public void setMailAddress(String text) { 
		checkUpdateable(); 
		mEntity.mailaddress = text;
	}
	
	@Override 
	public void setAvatar(String text) { 
		checkUpdateable(); 
		mEntity.avatar = text;
	}
	
	@Override 
	public void setBackground(String text) { 
		checkUpdateable(); 
		mEntity.background = text;
	}
	
	@Override 
	public void setEmail(String text) { 
		checkUpdateable(); 
		mEntity.email = text;
	}
	
	@Override 
	public void setType(String text) { 
		checkUpdateable(); 
		mEntity.type = text;
	}
	
	@Override 
	public void setCategory(String text) { 
		checkUpdateable(); 
		mEntity.category = text;
	}
	
	@Override
	public void setToken(String text) { 
		checkUpdateable(); 
		mEntity.token = text;
	}
	
	@Override
	public void setAuthKey(String text) { 
		checkUpdateable(); 
		mEntity.authkey = text;
	}
	
	@Override
	public void setDeviceKey(String text) { 
		checkUpdateable(); 
		mEntity.devicekey = text;
	}
	
	@Override
	public void setClientKey(String text) { 
		checkUpdateable(); 
		mEntity.clientkey = text;
	}
	
	@Override 
	public void setUsedSpace(long val) { 
		checkUpdateable(); 
		mEntity.usedspace = val;
	}
	
	@Override 
	public void setUsableSpace(long val) { 
		checkUpdateable(); 
		mEntity.usablespace = val;
	}
	
	@Override 
	public void setFreeSpace(long val) { 
		checkUpdateable(); 
		mEntity.freespace = val;
	}
	
	@Override 
	public void setPurchased(long val) { 
		checkUpdateable(); 
		mEntity.purchased = val;
	}
	
	@Override 
	public void setCapacity(long val) { 
		checkUpdateable(); 
		mEntity.capacity = val;
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
	public void setKeygenTime(long time) { 
		checkUpdateable(); 
		mEntity.keygenTime = time;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id=" + getId() + ",prefix=" + getPrefix()
				+ ",hostid=" + getHostId() + ",mailaddr=" + getMailAddress() 
				+ ",userkey=" + getUserKey() + ",username=" + getUserName() 
				+ "}";
	}
	
}
