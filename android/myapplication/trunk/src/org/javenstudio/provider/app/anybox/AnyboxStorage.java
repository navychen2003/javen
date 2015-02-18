package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.ArrayList;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.entitydb.content.IHostData;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.space.IUserSpaceData;
import org.javenstudio.util.StringUtils;

public class AnyboxStorage {
	private static final Logger LOG = Logger.getLogger(AnyboxStorage.class);

	static class HostData implements IHostData {
		private final String mKey;
		private String mMode;
		private String mClusterId;
		private String mClusterDomain;
		private String mMailDomain;
		private String mAdmin;
		private String mDomain;
		private String mHostAddr;
		private String mHostName;
		private String mLanAddr;
		private int mHttpPort;
		private int mHttpsPort;
		private long mHeartbeat;
		private int mStatus;
		
		public HostData(String key) {
			if (key == null) throw new NullPointerException();
			mKey = key;
		}
		
		public String getKey() { return mKey; }
		public String getMode() { return mMode; }
		public String getClusterId() { return mClusterId; }
		public String getClusterDomain() { return mClusterDomain; }
		public String getMailDomain() { return mMailDomain; }
		public String getAdmin() { return mAdmin; }
		public String getDomain() { return mDomain; }
		public String getHostAddr() { return mHostAddr; }
		public String getHostName() { return mHostName; }
		public String getLanAddr() { return mLanAddr; }
		public int getHttpPort() { return mHttpPort; }
		public int getHttpsPort() { return mHttpsPort; }
		public long getHeartbeat() { return mHeartbeat; }
		public int getStatus() { return mStatus; }

		@Override
		public String getDisplayName() {
			String name = getClusterDomain();
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
	}
	
	public static class UserData {
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
		
		public UserData(String key) {
			if (key == null) throw new NullPointerException();
			mKey = key;
		}
		
		public String getKey() { return mKey; }
		public String getName() { return mName; }
		public String getMailAddr() { return mMailAddr; }
		public String getNick() { return mNick; }
		public String getCategory() { return mCategory; }
		public String getType() { return mType; }
		public String getFlag() { return mFlag; }
		public String getIdle() { return mIdle; }
		public long getUsedSpace() { return mUsed; }
		public long getTotalSpace() { return mCapacity; }
		public long getRemainingSpace() { return mUsable; }
		public long getFreeSpace() { return mFree; }
		public long getPurchasedSpace() { return mPurchased; }
		public long getModifiedTime() { return mUtime; }
	}
	
	public static class StorageNode implements IUserSpaceData {
		private final AnyboxAccount mAccount;
		private final HostData mHost;
		private final UserData mUser;
		
		private AnyboxLibrary[] mLibraries = null;
		
		public StorageNode(AnyboxAccount account, HostData host, UserData user) {
			if (account == null || host == null || user == null) 
				throw new NullPointerException();
			mAccount = account;
			mHost = host;
			mUser = user;
		}
		
		public AnyboxAccount getAccount() { return mAccount; }
		public HostData getHost() { return mHost; }
		public UserData getUser() { return mUser; }
		
		public AnyboxLibrary[] getLibraries() { return mLibraries; }
		
		public long getRemainingSpace() { return getUser().getRemainingSpace(); }
		public long getFreeSpace() { return getUser().getFreeSpace(); }
		public long getTotalSpace() { return getUser().getTotalSpace(); }
		public long getUsedSpace() { return getUser().getUsedSpace(); }
		
		public String getCategory() { return getUser().getCategory(); }
		public String getHostName() { return getHost().getHostName(); }
		public String getUserName() { return getUser().getName(); }
		public String getNickName() { return getUser().getNick(); }
		public String getUserEmail() { return getUser().getMailAddr(); }
		public String getDisplayName() { return getUserName(); }
		
		public boolean isGroup() { 
			String type = getUser().getType();
			if (type != null && type.equalsIgnoreCase("group")) return true;
			return false; 
		}
		
		public float getUsedPercent() { 
			long used = getUsedSpace();
			long total = getTotalSpace();
			if (total > 0 && used >= total) return 100.0f;
			if (total > 0) return 100.0f * (float)used / (float)total;
			return 0;
		}
		
		public String getRequestAddr() {
			return getAccount().getApp().getRequestAddr(getHost(), false);
		}
		
		public String getRequestToken() {
			return getHost().getKey() + getUser().getKey() + 
					getAccount().getAccountData().getToken();
		}
	}
	
	static class StorageLibrary extends AnyboxLibrary 
			implements AnyboxHelper.IRequestWrapper {
		private final StorageNode mStorage;
		
		public StorageLibrary(AnyboxAccount user, 
				AnyboxData data, StorageNode node, String id, long requestTime) {
			super(user, data, id, requestTime);
			if (node == null) throw new NullPointerException();
			mStorage = node;
		}
		
		public StorageNode getStorage() { return mStorage; }
		
		@Override
		public String getRequestAddr() {
			return getStorage().getRequestAddr();
		}
		
		@Override
		public String getRequestToken() {
			return getStorage().getRequestToken();
		}
		
		@Override
		public AnyboxHelper.IRequestWrapper getRequestWrapper() {
			return this;
		}

		@Override
		public AnyboxApp getApp() {
			return getUser().getApp();
		}

		@Override
		public IHostData getHostData() {
			return getStorage().getHost();
		}

		@Override
		public String getAuthToken() {
			return getStorage().getRequestToken();
		}
	}
	
	static void loadStorages(AnyboxAccount account, 
			AnyboxData data) throws IOException {
		if (account == null || data == null) return;
		
		StorageNode[] storages = loadStorages0(account, data.get("storages"));
		if (storages != null) {
			for (StorageNode node : storages) {
				signinStorage(account, node);
			}
		}
		
		account.setStorages(storages);
		account.setLibraryRequestTime(System.currentTimeMillis());
	}
	
	static void signinStorage(AnyboxAccount account, 
			StorageNode node) throws IOException {
		if (account == null || node == null) return;
		
		String url = node.getRequestAddr() + "/user/login?action=check&wt=secretjson"
				+ "&token=" + StringUtils.URLEncode(node.getRequestToken())
				+ AnyboxAuth.getAppParams(account.getApp(), account.getAccountData().getClientKey());
		
		CheckListener listener = new CheckListener();
		AnyboxApi.request(url, listener);
	}
	
	static class CheckListener extends AnyboxApi.SecretJSONListener {
		@SuppressWarnings("unused")
		private AnyboxData mData = null;
		@SuppressWarnings("unused")
		private ActionError mError = null;
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mData = data; mError = error;
			if (LOG.isDebugEnabled())
				LOG.debug("handleData: data=" + data);
			
			if (error != null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("handleData: response error: " + error, 
							error.getException());
				}
			}
		}

		@Override
		public ActionError.Action getErrorAction() {
			return ActionError.Action.ACCOUNT_CHECK;
		}
	}
	
	static StorageNode[] loadStorages0(AnyboxAccount account, 
			AnyboxData data) throws IOException {
		if (account == null || data == null) return null;
		
		String[] names = data.getNames();
		ArrayList<StorageNode> list = new ArrayList<StorageNode>();
		
		if (names != null) {
			for (String name : names) {
				StorageNode node = loadStorage(account, data.get(name));
				if (node != null) list.add(node);
			}
		}
		
		return list.toArray(new StorageNode[list.size()]);
	}
	
	private static StorageNode loadStorage(AnyboxAccount account, 
			AnyboxData data) throws IOException {
		if (account == null || data == null) return null;
		
		HostData host = loadHost(data.get("host"));
		UserData user = loadUser(data.get("user"));
		
		final StorageNode node = new StorageNode(account, host, user);
		
		node.mLibraries = AnyboxLibrary.loadLibraries0(account, data.get("libraries"), 
			new AnyboxLibrary.ILibraryFactory() {
				@Override
				public AnyboxLibrary create(AnyboxAccount user, AnyboxData data, 
						String id, long requestTime) {
					return new StorageLibrary(user, data, node, id, requestTime);
				}
			});
		
		return node;
	}
	
	private static HostData loadHost(AnyboxData data) 
			throws IOException {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadHost: data=" + data);
		
		String key = data.getString("key");
		if (key == null) return null;
		
		HostData host = new HostData(key);
		host.mClusterId = data.getString("clusterid");
		host.mClusterDomain = data.getString("clusterdomain");
		host.mMailDomain = data.getString("maildomain");
		host.mAdmin = data.getString("admin");
		host.mDomain = data.getString("domain");
		host.mHostAddr = data.getString("hostaddr");
		host.mHostName = data.getString("hostname");
		host.mLanAddr = data.getString("lanaddr");
		host.mHttpPort = data.getInt("httpport", 80);
		host.mHttpsPort = data.getInt("httpsport", 443);
		host.mHeartbeat = data.getLong("heartbeat", 0);
		host.mStatus = data.getInt("status", 0);
		
		return host;
	}
	
	private static UserData loadUser(AnyboxData data) 
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
		
		return user;
	}
	
}
