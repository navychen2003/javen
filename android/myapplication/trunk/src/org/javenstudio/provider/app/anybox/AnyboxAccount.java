package org.javenstudio.provider.app.anybox;

import java.util.ArrayList;

import android.app.Activity;

import org.javenstudio.android.NetworkHelper;
import org.javenstudio.android.account.AccountInfo;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderManager;
import org.javenstudio.provider.account.AccountInfoProvider;
import org.javenstudio.provider.account.dashboard.DashboardProvider;
import org.javenstudio.provider.account.notify.NotifyProvider;
import org.javenstudio.provider.account.space.ITotalSpaceData;
import org.javenstudio.provider.account.space.IUserSpaceData;
import org.javenstudio.provider.account.space.SpacesProvider;
import org.javenstudio.provider.activity.ProviderListActivity;
import org.javenstudio.provider.library.list.LibrariesProvider;
import org.javenstudio.provider.library.select.SelectOperation;
import org.javenstudio.provider.task.TaskListProvider;

public abstract class AnyboxAccount extends AccountUser 
		implements IUserSpaceData, AnyboxHelper.IRequestWrapper {
	private static final Logger LOG = Logger.getLogger(AnyboxAccount.class);
	
	private final AnyboxApp mApp;
	//private final AnyboxData mData;
	//private final ActionError mError;
	
	private final HostData mHost;
	private final AccountData mAccount;
	private final long mAuthTime;
	
	private AnyboxAccountProfile mProfile = null;
	private AnyboxAccountSpace mSpaceInfo = null;
	private AnyboxAccountInfo mAccountInfo = null;
	private AnyboxDashboard mDashboard = null;
	private AnyboxHeartbeat mHeartbeat = null;
	private AnyboxLogout mLogout = null;
	
	private AnyboxSectionSearch.SearchList mSearchList = null;
	private AnyboxStorage.StorageNode[] mStorages = null;
	private AnyboxLibrary[] mLibraries = null;
	private long mLibraryRequestTime = 0;
	
	private SelectOperation mSelectOperation = null;
	
	public AnyboxAccount(AnyboxApp app, AccountData account, 
			HostData host, long authTime) {
		if (app == null || account == null || host == null) 
			throw new NullPointerException();
		mApp = app;
		mAccount = account;
		mHost = host;
		//mData = data;
		//mError = error;
		mAuthTime = authTime;
	}
	
	public AnyboxApp getApp() { return mApp; }
	public AccountData getAccountData() { return mAccount; }
	public long getAccountId() { return getAccountData().getId(); }
	public HostData getHostData() { return mHost; }
	public long getHostId() { return getHostData().getId(); }
	
	//public AnyboxData getData() { return mData; }
	//public ActionError getError() { return mError; }
	public long getAuthTime() { return mAuthTime; }
	
	public AnyboxHeartbeat getHeartbeat() { return mHeartbeat; }
	void setHeartbeat(AnyboxHeartbeat heartbeat) { mHeartbeat = heartbeat; }
	
	public AnyboxStorage.StorageNode[] getStorages() { return mStorages; }
	void setStorages(AnyboxStorage.StorageNode[] storages) { mStorages = storages; }
	
	public AnyboxLibrary[] getLibraries() { return mLibraries; }
	void setLibraries(AnyboxLibrary[] libraries) { mLibraries = libraries; }
	
	public long getLibraryRequestTime() { return mLibraryRequestTime; }
	void setLibraryRequestTime(long time) { mLibraryRequestTime = time; }
	
	public AnyboxLibrary[] getLibraryList() {
		ArrayList<AnyboxLibrary> list = new ArrayList<AnyboxLibrary>();
		
		AnyboxLibrary[] libs = getLibraries();
		if (libs != null) {
			for (AnyboxLibrary lib : libs) {
				if (lib != null) list.add(lib);
			}
		}
		
		AnyboxStorage.StorageNode[] nodes = getStorages();
		if (nodes != null) {
			for (AnyboxStorage.StorageNode node : nodes) {
				if (node == null) continue;
				
				AnyboxLibrary[] nodelibs = node.getLibraries();
				if (nodelibs != null) {
					for (AnyboxLibrary lib : nodelibs) {
						if (lib != null) list.add(lib);
					}
				}
			}
		}
		
		return list.toArray(new AnyboxLibrary[list.size()]);
	}
	
	public synchronized AnyboxSectionSearch.SearchList getSearchList() { 
		if (mSearchList == null) mSearchList = createSearchList();
		return mSearchList; 
	}
	
	protected AnyboxSectionSearch.SearchList createSearchList() {
		return new AnyboxSectionSearch.SearchList(4);
	}
	
	public synchronized SelectOperation getSelectOperation() {
		if (mSelectOperation == null) mSelectOperation = createSelectOperation();
		return mSelectOperation;
	}
	
	protected abstract SelectOperation createSelectOperation();
	
	public AnyboxDashboard getDashboard() { return mDashboard; }
	void setDashboard(AnyboxDashboard data) { 
		if (data != null && data != mDashboard) {
			mDashboard = data; 
			AnyboxAnnouncement.AnnouncementData[] ds = data.getAnnouncements();
			if (ds != null && ds.length > 0)
				dispatchChanged(DataType.ANNOUNCEMENT, DataState.UPDATED);
		}
	}
	
	public AccountInfo getAccountInfo() { 
		AnyboxAccountInfo accountInfo = mAccountInfo;
		if (accountInfo != null) {
			AnyboxAccountInfo.UserData userData = accountInfo.getUserData();
			if (userData != null) return userData;
		}
		return mAccount; 
	}
	
	public AnyboxAccountInfo getAnyboxAccountInfo() { return mAccountInfo; }
	void setAnyboxAccountInfo(AnyboxAccountInfo accountInfo) { 
		if (accountInfo != null && accountInfo != mAccountInfo) {
			mAccountInfo = accountInfo; 
			dispatchChanged(DataType.ACCOUNTINFO, DataState.UPDATED);
		}
	}
	
	public AnyboxAccountProfile getProfile() { return mProfile; }
	void setProfile(AnyboxAccountProfile profile) { mProfile = profile; }
	
	public AnyboxAccountSpace getSpaceInfo() { return mSpaceInfo; }
	void setSpaceInfo(AnyboxAccountSpace space) { mSpaceInfo = space; }

	public AnyboxLogout getLogoutInfo() { return mLogout; }
	void setLogoutInfo(AnyboxLogout info) { mLogout = info; }
	
	@Override
	public void onListenerAdded(OnDataChangeListener listener) {
		if (listener == null) return;
		
		try {
			if (getAnyboxAccountInfo() != null) {
				listener.onDataChanged(this, DataType.ACCOUNTINFO, DataState.UPDATED);
			}
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("onListenerAdded: error: " + e, e);
		}
	}
	
	@Override
	public void onNotifyMenuOpened() { 
		AnyboxAccountInfo accountInfo = getAnyboxAccountInfo();
		if (accountInfo != null) 
			accountInfo.setNewOpenedTime(System.currentTimeMillis());
	}
	
	public String getAuthToken() {
		return getHostData().getHostKey() + getAccountData().getUserKey() 
				+ getAccountData().getToken();
	}
	
	public String getAccountName() { return getAccountData().getUserName(); }
	public String getAccountFullname() { return getAccountData().getFullName(); }
	public String getHostDisplayName() { return getHostData().getDisplayName(); }
	public String getUserId() { return getAccountData().getUserKey(); }
	public String getMailAddress() { return getAccountData().getMailAddress(); }
	public String getDisplayName() { return getAccountName(); }
	
	public boolean isGroup() { 
		String type = getAccountData().getType();
		if (type != null && type.equalsIgnoreCase("group")) return true;
		return false;
	}
	
	public String getAvatarURL(int size) {
		return AnyboxHelper.getImageURL(this, getAccountInfo().getAvatar(), 
				("" + size + "t"), null);
	}
	
	public String getBackgroundURL(int width, int height) {
		return AnyboxHelper.getImageURL(this, getAccountInfo().getBackground(), 
				("" + width + "x" + height + "t"), null);
	}
	
	@Override
	public String getUserTitle() { 
		String name = getAccountInfo().getNickName(); 
		if (name != null && name.length() > 0) return name;
		name = getAccountInfo().getUserName();
		if (name != null && name.length() > 0) return name;
		name = getAccountInfo().getEmail();
		if (name != null && name.length() > 0) return name;
		return getAccountName();
	}
	
	@Override
	public String getUserEmail() { 
		return getAccountInfo().getEmail();
	}
	
	@Override
	public String getNickName() { 
		return getAccountInfo().getNickName(); 
	}
	
	@Override
	public int getStatisticCount(int type) {
		return 0;
	}
	
	@Override
	public long getUpdateTime() { 
		AnyboxAccountInfo accountInfo = getAnyboxAccountInfo();
		if (accountInfo != null) return accountInfo.getRequestTime();
		return getAuthTime(); 
	}
	
	public boolean hasNotification() {
		AnyboxAccountInfo accountInfo = getAnyboxAccountInfo();
		if (accountInfo != null) return accountInfo.hasNew();
		return false;
	}
	
	private AnyboxAccountSpace.UserInfo getMySpaceInfo() {
		AnyboxAccountSpace space = getSpaceInfo();
		if (space != null) {
			AnyboxAccountSpace.UserInfo info = space.getMe();
			if (info != null) return info;
		}
		return null;
	}
	
	@Override
	public long getUsedSpace() {
		AnyboxAccountSpace.UserInfo info = getMySpaceInfo();
		if (info != null) return info.getUsedSpace();
		return getAccountInfo().getUsedSpace();
	}
	
	@Override
	public long getRemainingSpace() {
		AnyboxHeartbeat heartbeat = getHeartbeat();
		if (heartbeat != null) {
			AnyboxHeartbeat.UserData data = heartbeat.getUserData();
			if (data != null) return data.getUsableSpace();
		}
		AnyboxAccountSpace.UserInfo info = getMySpaceInfo();
		if (info != null) return info.getRemainingSpace();
		return getAccountInfo().getUsableSpace();
	}
	
	@Override
	public long getFreeSpace() {
		AnyboxAccountSpace.UserInfo info = getMySpaceInfo();
		if (info != null) return info.getFreeSpace();
		return getAccountInfo().getFreeSpace();
	}

	@Override
	public long getTotalSpace() {
		AnyboxAccountSpace.UserInfo info = getMySpaceInfo();
		if (info != null) return info.getTotalSpace();
		return getAccountInfo().getCapacity();
	}
	
	public float getUsedPercent() {
		long used = getUsedSpace();
		long capacity = getTotalSpace();
		if (capacity > 0 && used >= 0) return 100.0f * used / capacity;
		return 0.0f;
	}
	
	@Override
	public String getCategory() {
		return getAccountInfo().getCategory();
	}
	
	@Override
	public String getHostName() {
		return getHostData().getHostName();
	}
	
	public long getTotalRemainingSpace() { 
		return getTotalSpaces().getRemainingSpace(); 
	}
	
	public float getTotalUsedPercent() { 
		return getTotalSpaces().getUsedPercent(); 
	}
	
	public long getTotalCapacitySpace() { 
		return getTotalSpaces().getTotalSpace(); 
	}
	
	public ITotalSpaceData getTotalSpaces() {
		return mTotalSpaces;
	}
	
	private ITotalSpaceData mTotalSpaces = new ITotalSpaceData() {
			@Override
			public long getRemainingSpace() {
				AnyboxStorage.StorageNode[] nodes = getStorages();
				long total = AnyboxAccount.this.getRemainingSpace();
				if (nodes != null) {
					for (AnyboxStorage.StorageNode node : nodes) {
						if (node != null) total += node.getRemainingSpace();
					}
				}
				return total;
			}
	
			@Override
			public long getFreeSpace() {
				AnyboxStorage.StorageNode[] nodes = getStorages();
				long total = AnyboxAccount.this.getFreeSpace();
				if (nodes != null) {
					for (AnyboxStorage.StorageNode node : nodes) {
						if (node != null) total += node.getFreeSpace();
					}
				}
				return total;
			}
	
			@Override
			public long getTotalSpace() {
				AnyboxStorage.StorageNode[] nodes = getStorages();
				long total = AnyboxAccount.this.getTotalSpace();
				if (nodes != null) {
					for (AnyboxStorage.StorageNode node : nodes) {
						if (node != null) total += node.getTotalSpace();
					}
				}
				return total;
			}
	
			@Override
			public long getUsedSpace() {
				AnyboxStorage.StorageNode[] nodes = getStorages();
				long total = AnyboxAccount.this.getUsedSpace();
				if (nodes != null) {
					for (AnyboxStorage.StorageNode node : nodes) {
						if (node != null) total += node.getUsedSpace();
					}
				}
				return total;
			}
	
			@Override
			public float getUsedPercent() {
				long used = getUsedSpace();
				long total = getTotalSpace();
				if (total > 0 && used >= total) return 100.0f;
				if (total > 0 && used >= 0) return 100.0f * (float)used / (float)total;
				return 0;
			}
		};
	
	private AccountInfoProvider mAccountProvider = null;
	private SpacesProvider mSpacesProvider = null;
	private NotifyProvider mNotifyProvider = null;
	private DashboardProvider mDashboardProvider = null;
	private LibrariesProvider mLibrariesProvider = null;
	private TaskListProvider mTaskListProvider = null;
	
	@Override
	public synchronized TaskListProvider getTaskListProvider() { 
		if (mTaskListProvider == null) 
			mTaskListProvider = getApp().createTaskListProvider(this);
		return mTaskListProvider; 
	}
	
	@Override
	public synchronized AccountInfoProvider getAccountProvider() { 
		if (mAccountProvider == null) 
			mAccountProvider = getApp().createAccountProvider(this);
		return mAccountProvider; 
	}
	
	@Override
	public synchronized SpacesProvider getSpacesProvider() { 
		if (mSpacesProvider == null) 
			mSpacesProvider = getApp().createSpacesProvider(this);
		return mSpacesProvider; 
	}
	
	@Override
	public synchronized NotifyProvider getNotifyProvider() {
		if (mNotifyProvider == null) {
			mNotifyProvider = getApp().createNotifyProvider(this);
			addListener(mNotifyProvider);
		}
		return mNotifyProvider;
	}
	
	@Override
	public synchronized DashboardProvider getDashboardProvider() {
		if (mDashboardProvider == null) 
			mDashboardProvider = getApp().createDashboardProvider(this);
		return mDashboardProvider;
	}
	
	@Override
	public synchronized LibrariesProvider getLibrariesProvider() {
		if (mLibrariesProvider == null) 
			mLibrariesProvider = getApp().createLibrariesProvider(this);
		return mLibrariesProvider;
	}
	
	@Override
	public long getHeartbeatDelayMillis() {
		if (mHeartbeat == null) return 0;
		
		if (NetworkHelper.getInstance().isWifiAvailable() && getApp().isActivityRunning())
			return SHORT_DELAY_MILLIS;
		else
			return LONG_DELAY_MILLIS;
	}
	
	public final ActionItem[] getNavigationItems(Activity activity) {
		initNavigationProviders(false);
		return ProviderManager.initNavigationItems((ProviderListActivity)activity);
	}
	
	private boolean mProviderInited = false;
	private final Object mLock = new Object();
	
	public final void initNavigationProviders(boolean force) {
		synchronized (mLock) {
			if (mProviderInited == false || force) setNavigationProviders();
			mProviderInited = true;
		}
	}
	
	protected void setNavigationProviders() {
		if (LOG.isDebugEnabled()) LOG.debug("setNavigationProviders: account=" + this);
		
		ProviderManager.clearProviders();
		ProviderManager.addProvider(getDashboardProvider());
		ProviderManager.addProvider(getLibrariesProvider());
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{account=" + mAccount 
				+ ",host=" + mHost + "}";
	}
	
}
