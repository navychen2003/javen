package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.user.INameData;
import org.javenstudio.falcon.user.IUserData;

final class AnyboxUser {
	private static final Logger LOG = Logger.getLogger(AnyboxUser.class);

	static class GetUserData implements IUserData {
		private final String mKey;
		private String mName;
		private String mHostKey;
		private int mFlag;
		private int mType;
		
		public GetUserData(String key) {
			if (key == null) throw new NullPointerException();
			mKey = key;
		}
		
		public String getUserName() { return mName; }
		public String getUserKey() { return mKey; }
		public String getHostKey() { return mHostKey; }
		
		public int getUserFlag() { return mFlag; }
		public int getUserType() { return mType; }
		
		public String getAttr(String name) { return null; }
		public String[] getAttrNames() { return null; }
		
		@Override
		public String toString() {
			return "GetUserData{key=" + mKey + ",name=" + mName 
					+ ",hostkey=" + mHostKey + ",flag=" + mFlag 
					+ ",type=" + mType + "}";
		}
	}
	
	static class GetNameData implements INameData {
		private final String mKey;
		private String mValue;
		private String mHostKey;
		private int mFlag;
		
		public GetNameData(String key) {
			if (key == null) throw new NullPointerException();
			mKey = key;
		}
		
		public String getNameKey() { return mKey; }
		public String getNameValue() { return mValue; }
		public String getHostKey() { return mHostKey; }
		
		public int getNameFlag() { return mFlag; }
		public String getAttr(String name) { return null; }
		public String[] getAttrNames() { return null; }
		
		@Override
		public String toString() {
			return "GetNameData{key=" + mKey + ",value=" + mValue 
					+ ",hostkey=" + mHostKey + ",flag=" + mFlag 
					+ "}";
		}
	}
	
	static class UserData implements IHostUser {
		private final String mKey;
		private String mName;
		private String mMailAddr;
		private String mNick;
		private String mCategory;
		private String mType;
		private String mFlag;
		private String mIdle;
		private long mUsed;
		private long mCapacity;
		private long mUsable;
		private long mFree;
		private long mPurchased;
		private long mUtime;
		
		private AnyboxLibrary.LibraryData[] mLibraries;
		
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
		public long getUsedSpace() { return mUsed; }
		public long getCapacitySpace() { return mCapacity; }
		public long getUsableSpace() { return mUsable; }
		public long getFreeSpace() { return mFree; }
		public long getPurchasedSpace() { return mPurchased; }
		public long getModifiedTime() { return mUtime; }
		
		public AnyboxLibrary.LibraryData[] getLibraries() { return mLibraries; }
		
		@Override
		public String toString() {
			return getClass().getSimpleName() + "{key=" + mKey 
					+ ",name=" + mName + ",mailAddr=" + mMailAddr + "}";
		}
	}
	
	static UserData[] loadUsers(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		String[] names = data.getNames();
		ArrayList<UserData> list = new ArrayList<UserData>();
		
		if (names != null) {
			for (String name : names) {
				UserData user = loadUser(data.get(name));
				if (user != null) list.add(user);
			}
		}
		
		return list.toArray(new UserData[list.size()]);
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
		user.mUsed = data.getLong("used", 0);
		user.mUsable = data.getLong("usable", 0);
		user.mCapacity = data.getLong("capacity", 0);
		user.mFree = data.getLong("free", 0);
		user.mPurchased = data.getLong("purchased", 0);
		user.mUtime = data.getLong("utime", 0);
		
		user.mLibraries = AnyboxLibrary.loadLibraries(data.get("libraries"));
		
		return user;
	}
	
	static GetUserData loadGetUser(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadGetUser: data=" + data);
		
		String key = data.getString("key");
		if (key == null) return null;
		
		GetUserData user = new GetUserData(key);
		user.mName = data.getString("name");
		user.mHostKey = data.getString("hostkey");
		user.mFlag = data.getInt("flag", 0);
		user.mType = data.getInt("type", 0);
		
		return user;
	}
	
	static GetNameData loadGetName(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadGetName: data=" + data);
		
		String key = data.getString("key");
		if (key == null) return null;
		
		GetNameData name = new GetNameData(key);
		name.mValue = data.getString("value");
		name.mHostKey = data.getString("hostkey");
		name.mFlag = data.getInt("flag", 0);
		
		return name;
	}
	
}
