package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.user.IMember;

final class AnyboxAuth {
	private static final Logger LOG = Logger.getLogger(AnyboxAuth.class);

	static class UserData implements IAttachUserInfo {
		private final String mKey;
		private String mName;
		private String mMailAddr;
		private String mNick;
		private String mCategory;
		private String mType;
		private String mFlag;
		private String mIdle;
		private String mToken;
		private String mClient;
		private String mDeviceKey;
		private String mAuthKey;
		private long mUsed;
		private long mCapacity;
		private long mUsable;
		private long mFree;
		private long mPurchased;
		private long mUtime;
		
		public UserData(String key) {
			if (key == null) throw new NullPointerException();
			mKey = key;
		}
		
		public String getUserKey() { return mKey; }
		public String getUserName() { return mName; }
		public String getUserEmail() { return mMailAddr; }
		public String getNickName() { return mNick; }
		public String getCategory() { return mCategory; }
		public String getType() { return mType; }
		public String getFlag() { return mFlag; }
		public String getIdle() { return mIdle; }
		public String getToken() { return mToken; }
		public String getClient() { return mClient; }
		public String getDeviceKey() { return mDeviceKey; }
		public String getAuthKey() { return mAuthKey; }
		public long getUsedSpace() { return mUsed; }
		public long getCapacitySpace() { return mCapacity; }
		public long getUsableSpace() { return mUsable; }
		public long getFreeSpace() { return mFree; }
		public long getPurchasedSpace() { return mPurchased; }
		public long getModifiedTime() { return mUtime; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{key=" + mKey 
					+ ",name=" + mName + ",mailAddr=" + mMailAddr + "}";
		}
	}
	
	static class AuthData implements IAuthInfo {
		private final IMember mUser;
		private final IHostInfo mHost;
		private final UserData mUserData;
		
		public AuthData(IMember user, IHostInfo host, UserData data) {
			if (user == null || host == null || data == null) 
				throw new NullPointerException();
			mUser = user;
			mUserData = data;
			mHost = host;
		}
		
		public IMember getUser() { return mUser; }
		public UserData getAttachUser() { return mUserData; }
		public IHostInfo getAttachHost() { return mHost; }
	}
	
	static AuthData loadAuth(IMember user, IHostInfo host, 
			AnyboxData data) throws IOException {
		if (user == null || host == null || data == null) 
			return null;
		
		UserData userData = loadUser(data.get("user"));
		
		return new AuthData(user, host, userData);
	}
	
	static UserData loadUser(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadUser: data=" + data);
		
		String key = data.getString("key");
		if (key == null) return null;
		
		UserData user = new UserData(key);
		user.mName = data.getString("name");
		user.mMailAddr = data.getString("mailaddr");
		user.mNick = data.getString("nick");
		user.mCategory = data.getString("category");
		user.mType = data.getString("type");
		user.mFlag = data.getString("flag");
		user.mIdle = data.getString("idle");
		user.mToken = data.getString("token");
		user.mClient = data.getString("client");
		user.mDeviceKey = data.getString("devicekey");
		user.mAuthKey = data.getString("authkey");
		user.mUsed = data.getLong("used", 0);
		user.mUsable = data.getLong("usable", 0);
		user.mCapacity = data.getLong("capacity", 0);
		user.mFree = data.getLong("free", 0);
		user.mPurchased = data.getLong("purchased", 0);
		user.mUtime = data.getLong("utime", 0);
		
		return user;
	}
	
}
