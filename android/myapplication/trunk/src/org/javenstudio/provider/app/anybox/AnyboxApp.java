package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountAuth;
import org.javenstudio.android.account.AccountWork;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.android.entitydb.content.IHostData;
import org.javenstudio.cocoka.android.PluginManager;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.AccountInfoProvider;
import org.javenstudio.provider.account.dashboard.DashboardProvider;
import org.javenstudio.provider.account.notify.NotifyProvider;
import org.javenstudio.provider.account.space.SpacesProvider;
import org.javenstudio.provider.library.ILibraryData;
import org.javenstudio.provider.library.ISectionInfoData;
import org.javenstudio.provider.library.list.LibrariesProvider;
import org.javenstudio.provider.library.list.LibraryProvider;
import org.javenstudio.provider.task.TaskListProvider;

public abstract class AnyboxApp extends AccountApp {
	private static final Logger LOG = Logger.getLogger(AnyboxApp.class);

	public static final String PREFIX = "anybox";
	public static final String DEVICE_TYPE = "android";
	
	private final AnyboxWork mWork = new AnyboxWork();
	private final Object mInitLock = new Object();
	
	private AnyboxAccount mAccount = null;
	private AccountData[] mAccounts = null;
	
	private final Map<String,AnyboxProperty> mProperties = 
			new HashMap<String,AnyboxProperty>();
	
	private String mHostName = null;
	private String mHostAddressPort = null; //AnyboxApi.LIGHTNING_ADDRESS;
	private String mWelcomeTitle = null;
	private String mWelcomeImageUrl = null;
	
	//private volatile boolean mInited = false;
	private long mInitedTime = 0;
	
	public AnyboxApp(String hostAddress) { 
		if (hostAddress == null || hostAddress.length() == 0)
			throw new IllegalArgumentException("Host address is empty");
		mHostAddressPort = hostAddress;
		//if (autoInit) AccountAuth.startInitWork(this, 0);
	}
	
	public abstract DataApp getDataApp();
	public abstract PluginManager.PackageInfo getPluginInfo(Context context);
	public abstract AnyboxAccount createAccount(AccountData account, HostData host, long authTime);
	
	public abstract AccountInfoProvider createAccountProvider(AnyboxAccount user);
	public abstract SpacesProvider createSpacesProvider(AnyboxAccount user);
	public abstract NotifyProvider createNotifyProvider(AnyboxAccount user);
	public abstract DashboardProvider createDashboardProvider(AnyboxAccount user);
	public abstract LibrariesProvider createLibrariesProvider(AnyboxAccount user);
	public abstract LibraryProvider createLibraryProvider(AnyboxAccount user, ILibraryData data);
	public abstract TaskListProvider createTaskListProvider(AnyboxAccount user);
	
	public abstract boolean openSectionDetails(Activity activity, 
			AnyboxAccount user, ISectionInfoData data);
	
	@Override
	public AnyboxWork getWork() {
		return mWork;
	}
	
	public String getHostAddressPort() { 
		return mHostAddressPort; 
	}
	
	public String getHostDisplayName() { 
		String name = mHostName;
		if (name == null || name.length() == 0)
			name = mHostAddressPort;
		return name;
	}
	
	public String getHostSiteUrl() {
		return "http://" + getHostAddressPort(); //AnyboxApi.SITE_ADDRESS;
	}
	
	public abstract String getAnyboxSiteUrl();
	
	String getRequestAddr(IHostData host, boolean setAddr) {
		String hostAddress = getHostAddressPort();
		
		if (host != null) {
			String hostName = host.getDisplayName();
			String hostAddr = host.getRequestAddressPort();
			
			if (hostAddr != null && hostAddr.length() > 0) {
				if (hostName == null || hostName.length() == 0)
					hostName = hostAddr;
				
				if (setAddr) {
					setHostAddressPort(hostAddr);
					setHostDisplayName(hostName);
				}
				
				hostAddress = hostAddr;
			}
		}
		
		return "http://" + hostAddress + "/lightning";
	}
	
	String getRequestUrl(IHostData host) {
		String hostAddress = getHostAddressPort();
		
		if (host != null) {
			//String hostName = host.getDisplayName();
			String hostAddr = host.getRequestAddressPort();
			
			if (hostAddr != null && hostAddr.length() > 0) {
				//if (hostName == null || hostName.length() == 0)
				//	hostName = hostAddr;
				
				hostAddress = hostAddr;
			}
		}
		
		return "http://" + hostAddress + "/lightning";
	}
	
	@Override
	public String getWelcomeImageUrl() {
		return mWelcomeImageUrl;
	}
	
	void setWelcomeImageUrl(String url) {
		if (LOG.isDebugEnabled()) LOG.debug("setWelcomeImageUrl: url=" + url);
		mWelcomeImageUrl = url;
	}
	
	@Override
	public String getWelcomeTitle() {
		return mWelcomeTitle;
	}
	
	public void setWelcomeTitle(String title) {
		if (LOG.isDebugEnabled()) LOG.debug("setWelcomeTitle: title=" + title);
		mWelcomeTitle = title;
	}
	
	public void setHostAddressPort(String addr) {
		if (LOG.isDebugEnabled()) LOG.debug("setHostAddressPort: addr=" + addr);
		if (addr != null && addr.length() > 0)
			mHostAddressPort = addr;
	}
	
	public void setHostDisplayName(String name) {
		if (LOG.isDebugEnabled()) LOG.debug("setHostDisplayName: name=" + name);
		if (name != null && name.length() > 0)
			mHostName = name;
	}
	
	@Override
	public void onAccountRemoved(AccountData account, boolean success) {
		if (LOG.isDebugEnabled()) { 
			LOG.debug("onAccountRemoved: account=" + account 
					+ " success=" + success);
		}
		setAccounts(null);
	}
	
	void setAccounts(AccountData[] accounts) {
		if (LOG.isDebugEnabled()) LOG.debug("setAccounts: accounts=" + accounts);
		synchronized (mInitLock) {
			mAccounts = accounts;
		}
	}
	
	void setAccount(AnyboxAccount user) {
		if (LOG.isDebugEnabled()) LOG.debug("setUser: user=" + user);
		synchronized (mInitLock) {
			mAccounts = null;
			if (user != null && user != mAccount) {
				AnyboxAccount old = mAccount;
				mAccount = user;
				onAccountChanged(user, old);
				mWork.scheduleWork(user, AccountWork.WorkType.ACCOUNTINFO, 0);
			} else if (user == null) {
				AnyboxAccount old = mAccount;
				mAccount = user;
				onAccountChanged(user, old);
			}
		}
	}
	
	protected void onAccountChanged(AnyboxAccount user, AnyboxAccount old) {
		if (LOG.isDebugEnabled())
			LOG.debug("onAccountChanged: user=" + user + " old=" + old);
	}
	
	@Override
	public void resetAccounts() {
		if (LOG.isDebugEnabled()) LOG.debug("resetAccounts");
		setAccount(null);
	}
	
	@Override
	public AccountData[] getAccounts() {
		synchronized (mInitLock) {
			if (mAccounts == null) {
				if (LOG.isDebugEnabled()) LOG.debug("getAccounts: requery");
				AccountData[] accounts = ContentHelper.getInstance().queryAccounts();
				if (accounts == null) accounts = new AccountData[0];
				mAccounts = accounts;
			}
			return mAccounts;
		}
	}
	
	@Override
	public AnyboxAccount getAccount() {
		return mAccount;
	}
	
	@Override
	public String getAccountName(long accountId) {
		return AnyboxAuth.getAccountName(this, accountId);
	}
	
	@Override
	public String getAccountAvatarURL(AccountData account, int size) {
		if (account != null) {
			AnyboxAccount user = getAccount();
			HostData host = ContentHelper.getInstance().getHost(account.getHostId());
			String authToken = user != null ? user.getAuthToken() : null;
			return AnyboxHelper.getImageURL(this, host, authToken, 
					account.getAvatar(), ("" + size + "t"), null);
		}
		return null;
	}
	
	public AnyboxProperty getSectionProperty(String sectionId) {
		if (sectionId == null || sectionId.length() == 0)
			return null;
		
		synchronized (mProperties) {
			return mProperties.get(sectionId);
		}
	}
	
	public void putSectionProperty(AnyboxProperty p) {
		if (p == null) return;
		
		synchronized (mProperties) {
			mProperties.put(p.getSectionId(), p);
		}
	}
	
	//@Override
	//public boolean isInited() {
	//	return mInited;
	//}
	
	@Override
	protected void doInit(AccountAuth.Callback cb) throws IOException {
		//if (mInited) return; 
		if (System.currentTimeMillis() - mInitedTime < 60 * 1000l)
			return;
		
		AnyboxHost.doInit(this, cb); 
		
		//mInited = true;
		mInitedTime = System.currentTimeMillis();
	}
	
	@Override
	protected AccountAuth doAuth(AccountAuth.Callback cb, 
			String accountEmail, boolean checkOnly) throws IOException {
		return AnyboxAuth.doAuth(this, cb, accountEmail, checkOnly);
	}
	
	@Override
	protected AccountAuth doLogin(AccountAuth.Callback cb, 
			String username, String password, String email, 
			boolean registerMode) throws IOException {
		return AnyboxAuth.doLogin(this, cb, 
				username, password, email, registerMode);
	}
	
	@Override
	public void onTerminate() { 
		super.onTerminate();
		mWork.stopAll();
	}
	
	@Override
	public boolean logout(final Activity activity, final LoginAction action, 
			final String accountEmail) {
		AnyboxAccount account = getAccount();
		if (LOG.isDebugEnabled()) { 
			LOG.debug("logout: user=" + account + " activity=" + activity 
					+ " action=" + action + " account=" + accountEmail);
		}
		
		if (account != null) {
			AnyboxLogout.scheduleLogout(account, activity, new AnyboxLogout.LogoutCallback() {
					@Override
					public void onLogout(AnyboxAccount user, boolean success) {
						resetAccounts();
						AppResources.getInstance().startLoginActivity(activity, action, accountEmail);
					}
				});
		} else {
			AppResources.getInstance().startLoginActivity(activity, action, accountEmail);
		}
		
		return true;
	}
	
	public String getLocalString() {
		String lang = getContext().getResources().getConfiguration().locale.toString();
		if (lang == null || lang.length() == 0)
			lang = "en";
		return lang;
	}
	
}
