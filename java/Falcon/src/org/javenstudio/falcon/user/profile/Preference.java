package org.javenstudio.falcon.user.profile;

import java.util.StringTokenizer;
import java.util.TimeZone;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.SettingVal;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.User;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.NamedList;

public class Preference extends SettingVal {
	private static final Logger LOG = Logger.getLogger(Preference.class);

	private static final String USERKEY_KEY = "user.userkey";
	private static final String USERNAME_KEY = "user.username";
	private static final String USERTYPE_KEY = "user.usertype";
	private static final String CATEGORY_KEY = "user.category";
	private static final String STATUS_KEY = "user.status";
	private static final String EMAIL_KEY = "user.email";
	private static final String NICKNAME_KEY = "user.nickname";
	private static final String AVATAR_KEY = "user.avatar";
	private static final String BACKGROUND_KEY = "user.background";
	private static final String LANGUAGE_KEY = "user.language";
	private static final String TIMEZONE_KEY = "user.timezone";
	
	private static final String ATTACH_HOSTKEY_KEY = "user.attach.hostkey";
	private static final String ATTACH_HOSTNAME_KEY = "user.attach.hostname";
	private static final String ATTACH_USERKEY_KEY = "user.attach.userkey";
	private static final String ATTACH_USERNAME_KEY = "user.attach.username";
	private static final String ATTACH_MAILADDR_KEY = "user.attach.mailaddr";
	
	private final IUser mUser;
	private final IPreferenceStore mStore;
	private volatile boolean mLoaded = false;
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return Preference.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "Preference(" + Preference.this.getUser().getUserName() + ")";
			}
		};
	
	public Preference(IUser user, IPreferenceStore store) throws ErrorException { 
		if (user == null || store == null) throw new NullPointerException();
		mUser = user;
		mStore = store;
		loadPreference(false);
	}
	
	public IUser getUser() { return mUser; }
	public IPreferenceStore getStore() { return mStore; }
	public ILockable.Lock getLock() { return mLock; }
	
	public String getUserKey() { return getString(USERKEY_KEY, null); }
	public void setUserKey(String val) { setString(USERKEY_KEY, val); }
	
	public String getUserName() { return getString(USERNAME_KEY, null); }
	public void setUserName(String val) { setString(USERNAME_KEY, val); }
	
	public String getUserType() { return getString(USERTYPE_KEY, null); }
	public void setUserType(String val) { setString(USERTYPE_KEY, val); }
	
	public String getCategory() { return getString(CATEGORY_KEY, null); }
	public void setCategory(String val) { setString(CATEGORY_KEY, val); }
	
	public String getStatus() { return getString(STATUS_KEY, null); }
	public void setStatus(String val) { setString(STATUS_KEY, val); }
	
	public String getEmail() { return getString(EMAIL_KEY, null); }
	public void setEmail(String val) { setString(EMAIL_KEY, val); }
	
	public String getNickName() { return getString(NICKNAME_KEY, null); }
	public void setNickName(String val) { setString(NICKNAME_KEY, val); }
	
	public String getAvatar() { return getString(AVATAR_KEY, null); }
	public void setAvatar(String val) { setString(AVATAR_KEY, val); }
	
	public String getBackground() { return getString(BACKGROUND_KEY, null); }
	public void setBackground(String val) { setString(BACKGROUND_KEY, val); }
	
	public String getLanguage() { return getString(LANGUAGE_KEY, null); }
	public void setLanguage(String val) { setString(LANGUAGE_KEY, val); }
	
	public String getAttachHostKey() { return getString(ATTACH_HOSTKEY_KEY, null); }
	public void setAttachHostKey(String val) { setString(ATTACH_HOSTKEY_KEY, val); }
	
	public String getAttachHostName() { return getString(ATTACH_HOSTNAME_KEY, null); }
	public void setAttachHostName(String val) { setString(ATTACH_HOSTNAME_KEY, val); }
	
	public String getAttachUserKey() { return getString(ATTACH_USERKEY_KEY, null); }
	public void setAttachUserKey(String val) { setString(ATTACH_USERKEY_KEY, val); }
	
	public String getAttachUserName() { return getString(ATTACH_USERNAME_KEY, null); }
	public void setAttachUserName(String val) { setString(ATTACH_USERNAME_KEY, val); }
	
	public String getAttachUserEmail() { return getString(ATTACH_MAILADDR_KEY, null); }
	public void setAttachUserEmail(String val) { setString(ATTACH_MAILADDR_KEY, val); }
	
	public String getTimezone() { return getString(TIMEZONE_KEY, null); }
	
	public void setTimezone(String val) { 
		if (val != null) { 
			String timezone = null;
			StringTokenizer st = new StringTokenizer(val, " \t\r\n(),");
			while (st.hasMoreElements()) { 
				String token = st.nextToken();
				if (token == null || token.length() == 0)
					continue;
				
				if (LOG.isDebugEnabled())
					LOG.debug("setTimezone: token=" + token);
				
				token = token.toUpperCase();
				if (token.startsWith("GMT")) { 
					try {
						timezone = TimeZone.getTimeZone(token).getID();
						break;
					} catch (Throwable e) { 
						// ignore
					}
				}
			}
			
			if (LOG.isDebugEnabled())
				LOG.debug("setTimezone: tz=" + timezone +" val=" + val);
			
			val = timezone;
		}
		setString(TIMEZONE_KEY, val); 
	}
	
	public boolean isClosed() { return mClosed; }
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
		
		//try { 
		//	savePreference();
		//} catch (Throwable e) { 
		//	if (LOG.isDebugEnabled())
		//		LOG.debug("close: error: " + e, e);
		//}
	}
	
	public synchronized void savePreference() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("savePreference: user=" + getUser());
		
		getLock().lock(ILockable.Type.WRITE, ILockable.Check.ALL);
		try {
			synchronized (getUser()) {
				getStore().savePreference(this, toNamedList(this));
				mLoaded = true;
				
				((User)getUser()).setModifiedTime(System.currentTimeMillis());
			}
		} finally { 
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	public synchronized void loadPreference(boolean force) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("loadPreference: user=" + getUser());
		
		getLock().lock(ILockable.Type.READ, null);
		try {
			synchronized (getUser()) {
				if (mLoaded && force == false) return;
				clear();
				
				NamedList<Object> items = getStore().loadPreference(this);
				loadPrefernce0(items, this);
				mLoaded = true;
			}
		} finally { 
			getLock().unlock(ILockable.Type.READ);
		}
	}
	
	static void loadPrefernce0(NamedList<Object> listItem, Preference item) { 
		loadSettingVal(item, listItem);
	}
	
}
