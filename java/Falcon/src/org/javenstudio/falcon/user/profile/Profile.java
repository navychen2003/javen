package org.javenstudio.falcon.user.profile;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingAttr;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.User;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;

public abstract class Profile extends SettingAttr {
	private static final Logger LOG = Logger.getLogger(Profile.class);
	
	private final IProfileStore mStore;
	private volatile boolean mClosed = false;
	
	public Profile(String key, IProfileStore store) { 
		super(key);
		if (store == null) throw new NullPointerException();
		mStore = store;
	}
	
	public abstract IUser getUser();
	public abstract ILockable.Lock getLock();
	
	public final IProfileStore getStore() { return mStore; }
	
	public void setUserName(String value, boolean ignoreIfExists) { addKeyIfNotNull(USERNAME_KEY, value, ignoreIfExists); }
	public String getUserName() { return getItemString(USERNAME_KEY); }
	
	public void setAttachHostName(String value, boolean ignoreIfExists) { addKeyIfNotNull(ATTACH_HOSTNAME_KEY, value, ignoreIfExists); }
	public String getAttachHostName() { return getItemString(ATTACH_HOSTNAME_KEY); }
	
	public void setAttachHostKey(String value, boolean ignoreIfExists) { addKeyIfNotNull(ATTACH_HOSTKEY_KEY, value, ignoreIfExists); }
	public String getAttachHostKey() { return getItemString(ATTACH_HOSTKEY_KEY); }
	
	public void setAttachUserKey(String value, boolean ignoreIfExists) { addKeyIfNotNull(ATTACH_USERKEY_KEY, value, ignoreIfExists); }
	public String getAttachUserKey() { return getItemString(ATTACH_USERKEY_KEY); }
	
	public void setAttachUserName(String value, boolean ignoreIfExists) { addKeyIfNotNull(ATTACH_USERNAME_KEY, value, ignoreIfExists); }
	public String getAttachUserName() { return getItemString(ATTACH_USERNAME_KEY); }
	
	public void setAttachUserEmail(String value, boolean ignoreIfExists) { addKeyIfNotNull(ATTACH_MAILADDR_KEY, value, ignoreIfExists); }
	public String getAttachUserEmail() { return getItemString(ATTACH_MAILADDR_KEY); }
	
	public void setEmail(String value, boolean ignoreIfExists) { addInternetAddressIfNotNull(EMAIL_ADDR, value, ignoreIfExists); }
	public String getEmail() { return getItemString(EMAIL_ADDR); }
	
	public void setNickName(String value, boolean ignoreIfExists) { addNameIfNotNull(NICK_NAME, value, ignoreIfExists); }
	public String getNickName() { return getItemString(NICK_NAME); }
	
	public void setFirstName(String value, boolean ignoreIfExists) { addNameIfNotNull(FIRST_NAME, value, ignoreIfExists); }
	public String getFirstName() { return getItemString(FIRST_NAME); }
	
	public void setLastName(String value, boolean ignoreIfExists) { addNameIfNotNull(LAST_NAME, value, ignoreIfExists); }
	public String getLastName() { return getItemString(LAST_NAME); }
	
	public void setSex(String value, boolean ignoreIfExists) { addAttributeIfNotNull(SEX_ATTR, value, ignoreIfExists); }
	public String getSex() { return getItemString(SEX_ATTR); }
	
	public void setBirthday(String value, boolean ignoreIfExists) { addAttributeIfNotNull(BIRTHDAY_ATTR, value, ignoreIfExists); }
	public String getBirthday() { return getItemString(BIRTHDAY_ATTR); }
	
	public void setTimezone(String value, boolean ignoreIfExists) { addAttributeIfNotNull(TIMEZONE_ATTR, value, ignoreIfExists); }
	public String getTimezone() { return getItemString(TIMEZONE_ATTR); }
	
	public void setTags(String value, boolean ignoreIfExists) { addAttributeIfNotNull(TAGS_ATTR, value, ignoreIfExists); }
	public String getTags() { return getItemString(TAGS_ATTR); }
	
	public void setRegion(String value, boolean ignoreIfExists) { addAddressIfNotNull(REGION_ADDR, value, ignoreIfExists); }
	public String getRegion() { return getItemString(REGION_ADDR); }
	
	//public void setTitle(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(TITLE_DESC, value, ignoreIfExists); }
	//public String getTitle() { return getItemString(TITLE_DESC); }
	
	public void setBrief(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(BRIEF_DESC, value, ignoreIfExists); }
	public String getBrief() { return getItemString(BRIEF_DESC); }
	
	public void setIntroduction(String value, boolean ignoreIfExists) { addDescriptionIfNotNull(INTRO_DESC, value, ignoreIfExists); }
	public String getIntroduction() { return getItemString(INTRO_DESC); }
	
	public void setAvatar(String value, boolean ignoreIfExists) { addResourceIfNotNull(AVATAR_RES, value, ignoreIfExists); }
	public String getAvatar() { return getItemString(AVATAR_RES); }
	
	public void setBackground(String value, boolean ignoreIfExists) { addResourceIfNotNull(BACKGROUND_RES, value, ignoreIfExists); }
	public String getBackground() { return getItemString(BACKGROUND_RES); }
	
	public synchronized void saveProfile() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("saveProfile: user=" + getUser());
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (getUser()) {
				getStore().saveProfile(this, toNamedList(this));
				
				Preference preference = getUser().getPreference();
				preference.setStatus(getBrief());
				preference.setEmail(getEmail());
				preference.setAvatar(getAvatar());
				preference.setBackground(getBackground());
				preference.setNickName(getNickName());
				preference.setTimezone(getTimezone());
				
				preference.setAttachHostKey(getAttachHostKey());
				preference.setAttachHostName(getAttachHostName());
				preference.setAttachUserKey(getAttachUserKey());
				preference.setAttachUserName(getAttachUserName());
				preference.setAttachUserEmail(getAttachUserEmail());
				
				preference.savePreference();
				
				((User)getUser()).setModifiedTime(System.currentTimeMillis());
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadProfile(boolean force) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadProfile: user=" + getUser());
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (getUser()) {
				if (size() > 0 && force == false) return;
				clear();
				
				NamedList<Object> items = getStore().loadProfile(this);
				loadProfile0(items, this);
			}
		} finally { 
			getLock().unlock(ILockable.Type.READ);
		}
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
	}
	
	public boolean isClosed() { return mClosed; }
	
	private static void loadProfile0(NamedList<Object> item, Profile p) 
			throws ErrorException { 
		SettingAttr.loadSettingAttr(item, p);
	}
	
	private static NamedList<Object> toNamedList(Profile item) 
			throws ErrorException { 
		return SettingAttr.toNamedList((SettingAttr)item);
	}
	
}
