package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.android.ActionError;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.account.space.IUserSpaceData;
import org.javenstudio.util.StringUtils;

public class AnyboxAccountSpace {
	private static final Logger LOG = Logger.getLogger(AnyboxAccountSpace.class);

	public class UserInfo implements IUserSpaceData {
		private String mKey = null;
		private String mName = null;
		private String mMailAddr = null;
		private String mNickName = null;
		private String mCategory = null;
		private String mType = null;
		private String mFlag = null;
		private long mUsedSpace = 0;
		private long mCapacity = 0;
		private long mUsableSpace = 0;
		private long mFreeSpace = 0;
		private long mPurchasedSpace = 0;
		private LibraryInfo[] mLibraries = null;
		
		private UserInfo() {}
		
		public String getKey() { return mKey; }
		public String getName() { return mName; }
		public String getMailAddr() { return mMailAddr; }
		public String getNickName() { return mNickName; }
		public String getCategory() { return mCategory; }
		public String getType() { return mType; }
		public String getFlag() { return mFlag; }
		public long getUsedSpace() { return mUsedSpace; }
		public long getTotalSpace() { return mCapacity; }
		public long getRemainingSpace() { return mUsableSpace; }
		public long getFreeSpace() { return mFreeSpace; }
		public long getPurchasedSpace() { return mPurchasedSpace; }
		
		public LibraryInfo[] getLibraries() { return mLibraries; }
		
		public String getDisplayName() { return getName(); }
		public String getHostName() { return getUser().getHostName(); }
		
		@Override
		public boolean isGroup() {
			String type = getType();
			if (type != null && type.equalsIgnoreCase("group")) return true;
			return false;
		}
	}
	
	public static class LibraryInfo {
		private String mId = null;
		private String mName = null;
		private String mHostname = null;
		private String mType = null;
		private long mCreatedTime = 0;
		private long mModifiedTime = 0;
		private long mIndexTime = 0;
		private int mTotalFiles = 0;
		private int mTotalFolders = 0;
		private long mTotalLength = 0;
		
		private LibraryInfo() {}
		
		public String getId() { return mId; }
		public String getName() { return mName; }
		public String getHostname() { return mHostname; }
		public String getType() { return mType; }
		public long getCreatedTime() { return mCreatedTime; }
		public long getModifiedTime() { return mModifiedTime; }
		public long getIndexTime() { return mIndexTime; }
		public int getTotalFiles() { return mTotalFiles; }
		public int getTotalFolders() { return mTotalFolders; }
		public long getTotalLength() { return mTotalLength; }
	}
	
	private final AnyboxAccount mUser;
	//private final AnyboxData mData;
	//private final ActionError mError;
	private final long mRequestTime;
	private final long mReloadId;

	private UserInfo[] mUsers = null;
	private UserInfo mMe = null;
	
	private AnyboxAccountSpace(AnyboxAccount user, AnyboxData data, 
			ActionError error, long requestTime, long reloadId) {
		if (user == null) throw new NullPointerException();
		mUser = user;
		//mData = data;
		//mError = error;
		mRequestTime = requestTime;
		mReloadId = reloadId;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	//public AnyboxData getData() { return mData; }
	//public ActionError getError() { return mError; }
	
	public long getRequestTime() { return mRequestTime; }
	public long getReloadId() { return mReloadId; }
	
	public UserInfo getMe() { return mMe; }
	public UserInfo[] getUsers() { return mUsers; }
	
	public void loadData(AnyboxData data) throws IOException {
		if (data == null) return;
		
		AnyboxData spaces = data.get("spaceinfo");
		ArrayList<UserInfo> list = new ArrayList<UserInfo>();
		
		if (spaces != null) {
			String[] names = spaces.getNames();
			if (names != null) {
				for (String name : names) {
					AnyboxData ad = spaces.get(name);
					if (LOG.isDebugEnabled())
						LOG.debug("loadData: name= " + name + " user=" + ad);
					
					UserInfo user = loadUserInfo(ad);
					if (user != null) { 
						String key = user.getKey();
						if (key != null && key.equals(getUser().getAccountData().getUserKey())) 
							mMe = user;
						else 
							list.add(user);
					}
				}
			}
		}
		
		mUsers = list.toArray(new UserInfo[list.size()]);
	}
	
	private UserInfo loadUserInfo(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		UserInfo user = new UserInfo();
		user.mKey = data.getString("key");
		user.mName = data.getString("name");
		user.mNickName = data.getString("nick");
		user.mMailAddr = data.getString("mailaddr");
		user.mCategory = data.getString("category");
		user.mType = data.getString("type");
		user.mFlag = data.getString("flag");
		user.mUsedSpace = data.getLong("used", 0);
		user.mCapacity = data.getLong("capacity", 0);
		user.mUsableSpace = data.getLong("usable", 0);
		user.mFreeSpace = data.getLong("free", 0);
		user.mPurchasedSpace = data.getLong("purchased", 0);
		
		AnyboxData libs = data.get("libraries");
		ArrayList<LibraryInfo> list = new ArrayList<LibraryInfo>();
		
		if (libs != null) {
			String[] names = libs.getNames();
			if (names != null) {
				for (String name : names) {
					AnyboxData ad = libs.get(name);
					if (LOG.isDebugEnabled())
						LOG.debug("loadUserInfo: name=" + name + " library=" + ad);
					
					LibraryInfo lib = loadLibraryInfo(ad);
					if (lib != null) list.add(lib);
				}
			}
		}
		user.mLibraries = list.toArray(new LibraryInfo[list.size()]);
		
		return user;
	}
	
	private LibraryInfo loadLibraryInfo(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		LibraryInfo lib = new LibraryInfo();
		lib.mId = data.getString("id");
		lib.mName = data.getString("name");
		lib.mHostname = data.getString("hostname");
		lib.mType = data.getString("type");
		lib.mCreatedTime = data.getLong("ctime", 0);
		lib.mModifiedTime = data.getLong("mtime", 0);
		lib.mIndexTime = data.getLong("itime", 0);
		lib.mTotalFiles = data.getInt("totalfiles", 0);
		lib.mTotalFolders = data.getInt("totalfolders", 0);
		lib.mTotalLength = data.getLong("totallength", 0);
		
		return lib;
	}
	
	public static void reloadSpaceInfo(AnyboxAccount user, 
			ProviderCallback callback, long reloadId) {
		if (user == null) return;
		AnyboxAccountSpace space = user.getSpaceInfo();
		if (space != null && space.getReloadId() == reloadId) {
			if (LOG.isDebugEnabled())
				LOG.debug("reloadSpaceInfo: already loaded for same reloadId: " + reloadId);
			return;
		}
		getSpaceInfo(user, callback, reloadId);
	}
	
	static void getSpaceInfo(AnyboxAccount user, 
			ProviderCallback callback, long reloadId) {
		if (user == null) return;
		
		String url = user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/user/space?wt=secretjson&action=info" 
				+ "&token=" + StringUtils.URLEncode(user.getAuthToken());
		
		SpaceListener listener = new SpaceListener(user, reloadId);
		AnyboxApi.request(url, listener);
		
		ActionError error = null;
		try {
			AnyboxAccountSpace space = listener.mSpace;
			if (space != null) {
				error = listener.mError;
				if (error == null || error.getCode() == 0) {
					error = null;
					space.loadData(listener.mData);
					user.setSpaceInfo(space);
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("getSpaceInfo: error: " + e, e);
		}
		
		if (callback != null && error != null) 
			callback.onActionError(error);
	}
	
	static class SpaceListener extends AnyboxApi.SecretJSONListener {
		private final AnyboxAccount mUser;
		private final long mReloadId;
		private AnyboxAccountSpace mSpace = null;
		private AnyboxData mData = null;
		private ActionError mError = null;
		
		public SpaceListener(AnyboxAccount user, long reloadId) {
			if (user == null) throw new NullPointerException();
			mUser = user;
			mReloadId = reloadId;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			mSpace = new AnyboxAccountSpace(mUser, data, error, 
					System.currentTimeMillis(), mReloadId);
			
			if (LOG.isDebugEnabled())
				LOG.debug("handleData: reloadId=" + mReloadId + " data=" + data);
			
			if (error != null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("handleData: response error: " + error, 
							error.getException());
				}
			}
		}

		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.ACCOUNT_SPACE;
		}
	}
	
}
