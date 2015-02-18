package org.javenstudio.provider.app.anybox;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountAuth;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.android.entitydb.content.HostData;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.util.IdentityUtils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.mail.util.Base64Util;
import org.javenstudio.util.StringUtils;

final class AnyboxAuth {
	private static final Logger LOG = Logger.getLogger(AnyboxAuth.class);
	
	static void doInit(final AnyboxApp app) throws IOException {
		if (app == null) throw new NullPointerException();
		if (LOG.isDebugEnabled()) LOG.debug("doInit");
		
		final String url = app.getHostSiteUrl() + "/android.txt";
		
		AnyboxApi.request(url, new AnyboxApi.ResponseListener() {
				@Override
				public void onContentFetched(String content, Throwable e) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("onContentFetched: url=" + url 
								+ " content=" + content);
					}
					if (content != null && content.length() > 0) {
						try {
							Properties props = new Properties();
							props.load(new StringReader(content));
							//app.init(props);
						} catch (Throwable ex) {
							if (LOG.isWarnEnabled())
								LOG.warn("doInit: load " + url + " error: " + ex, ex);
						}
					}
				}
			});
	}
	
	static AccountAuth doAuth(AnyboxApp app, AccountAuth.Callback cb, 
			String accountEmail, boolean checkOnly) throws IOException {
		if (app == null) throw new NullPointerException();
		if (LOG.isDebugEnabled()) { 
			LOG.debug("checkAuth: account=" + accountEmail 
					+ " checkOnly=" + checkOnly);
		}
		
		if (app.getAccount() != null) {
			return new AccountAuth(AccountAuth.Result.AUTHENTICATED, null);
		}
		
		AccountData[] accounts = loadAccounts(app, false);
		if (accounts == null || accounts.length == 0) {
			//ActionError error = new ActionError(-2, "no accounts", null);
			return new AccountAuth(AccountAuth.Result.NO_ACCOUNT, null);
		}
		
		if (checkOnly) return null;
		//if (!app.isInited()) app.doInit();
		
		AccountData account = null;
		for (AccountData data : accounts) {
			if (data == null) continue;
			if (account == null || data.getUpdateTime() > account.getUpdateTime()) {
				String clientkey = data.getClientKey();
				String devicekey = data.getDeviceKey();
				String authkey = data.getAuthKey();
				if (authkey != null && authkey.length() > 0 && 
					devicekey != null && devicekey.length() > 0 &&
					clientkey != null && clientkey.length() > 0) {
					if (data.getStatus() == AccountData.STATUS_LOGIN)
						account = data;
				}
			}
			if (accountEmail != null && accountEmail.equals(data.getMailAddress())) {
				account = data;
				break;
			}
		}
		
		if (account != null) 
			return doAuth(app, account, cb);
		
		return new AccountAuth(AccountAuth.Result.SELECT_ACCOUNT, null);
	}
	
	private static AccountData[] loadAccounts(AnyboxApp app, boolean force) {
		AccountData[] accounts = app.getAccounts();
		if (accounts == null || force) {
			accounts = ContentHelper.getInstance().queryAccounts();
			if (accounts == null) accounts = new AccountData[0];
			app.setAccounts(accounts);
		}
		return accounts;
	}
	
	public static AccountData loadAccount(AnyboxApp app, long accountId, boolean force) {
		AccountData[] accounts = loadAccounts(app, force);
		if (accounts != null) {
			for (AccountData account : accounts) {
				if (account != null && account.getId() == accountId)
					return account;
			}
		}
		return null;
	}
	
	public static String getAccountName(AnyboxApp app, long accountId) {
		AccountData account = loadAccount(app, accountId, false);
		if (account != null) return account.getUserName();
		return null;
	}
	
	private static AccountAuth onAuthenticated(AnyboxApp app, 
			AuthListener listener) throws IOException {
		if (app == null || listener == null) throw new NullPointerException();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onAuthenticated: name=" + listener.mAuthName 
					+ " accountId=" + listener.mAccountId 
					+ " action=" + listener.mAction);
		}
		
		AnyboxData data = listener.mData;
		if (data != null) {
			String hostkey = null;
			String hostaddr = null;
			String hostname = null;
			String domain = null;
			String clusterId = null;
			String clusterDomain = null;
			String mailDomain = null;
			
			int httpport = 0;
			int httpsport = 0;
			
			AnyboxData systemData = data.get("system");
			if (systemData != null) {
				hostkey = systemData.getString("hostkey");
				hostaddr = systemData.getString("hostaddr");
				hostname = systemData.getString("hostname");
				domain = systemData.getString("domain");
				clusterId = systemData.getString("clusterid");
				clusterDomain = systemData.getString("clusterdomain");
				mailDomain = systemData.getString("maildomain");
				
				httpport = systemData.getInt("httpport", 0);
				httpsport = systemData.getInt("httpsport", 0);
			}
			
			String userkey = null;
			String username = null;
			String nickname = null;
			String mailaddr = null;
			String devicekey = null;
			String authkey = null;
			String token = null;
			String type = null;
			String category = null;
			String email = null;
			String avatar = null;
			String background = null;
			
			long usedSpace = 0;
			long usableSpace = 0;
			long freeSpace = 0;
			long purchased = 0;
			long capacity = 0;
			
			AnyboxData userData = data.get("user");
			if (userData != null) {
				userkey = userData.getString("key");
				username = userData.getString("name");
				nickname = userData.getString("nick");
				mailaddr = userData.getString("mailaddr");
				devicekey = userData.getString("devicekey");
				authkey = userData.getString("authkey");
				token = userData.getString("token");
				type = userData.getString("type");
				category = userData.getString("category");
				email = userData.getString("email");
				avatar = userData.getString("avatar");
				background = userData.getString("background");
				
				usedSpace = userData.getLong("used", 0);
				usableSpace = userData.getLong("usable", 0);
				freeSpace = userData.getLong("free", 0);
				purchased = userData.getLong("purchased", 0);
				capacity = userData.getLong("capacity", 0);
			}
			
			if (isEmpty(hostkey, hostaddr, username, userkey, 
					devicekey, authkey, token) == false) {
				long current = System.currentTimeMillis();
				
				AccountData account = listener.mAccountId > 0 ? 
						ContentHelper.getInstance().getAccount(listener.mAccountId) : null;
				
				HostData hostUpdate = null;
				boolean hostUpdateAll = true;
				if (account != null) 
					hostUpdate = ContentHelper.getInstance().getHost(account.getHostId());
				if (hostUpdate == null) 
					hostUpdate = ContentHelper.getInstance().queryHostByKey(hostkey);
				if (hostUpdate == null) 
					hostUpdate = ContentHelper.getInstance().queryHostByAddress(domain, hostaddr, true);
				if (hostUpdate == null) {
					hostUpdate = ContentHelper.getInstance().newHost();
					hostUpdate.setCreateTime(current);
					hostUpdateAll = true;
				} else {
					//String cid = hostUpdate.getClusterId();
					//String hkey = hostUpdate.getHostKey();
					//if (hkey != null && hkey.length() > 0) hostUpdateAll = false;
					//else hostUpdateAll = true;
					hostUpdate = hostUpdate.startUpdate();
					hostUpdateAll = true;
				}
				
				if (hostUpdateAll) {
					hostUpdate.setClusterId(clusterId);
					hostUpdate.setClusterDomain(clusterDomain);
					hostUpdate.setMailDomain(mailDomain);
					hostUpdate.setHostKey(hostkey);
					hostUpdate.setHostName(hostname);
					hostUpdate.setHostAddr(hostaddr);
					hostUpdate.setDomain(domain);
					hostUpdate.setHttpPort(httpport);
					hostUpdate.setHttpsPort(httpsport);
				}
				
				hostUpdate.setPrefix(AnyboxApp.PREFIX);
				hostUpdate.setFlag(HostData.FLAG_OK);
				hostUpdate.setStatus(HostData.STATUS_OK);
				hostUpdate.setFailedCode(0);
				hostUpdate.setFailedMessage("");
				hostUpdate.setUpdateTime(current);
				long hostId = hostUpdate.commitUpdates();
				
				if (account == null)
					account = ContentHelper.getInstance().newAccount();
				else
					account = account.startUpdate();
				
				account.setHostId(hostId);
				account.setUserKey(userkey);
				account.setUserName(username);
				account.setNickName(nickname);
				account.setMailAddress(mailaddr);
				account.setType(type);
				account.setCategory(category);
				account.setAvatar(avatar);
				account.setBackground(background);
				account.setEmail(email);
				account.setToken(token);
				account.setAuthKey(authkey);
				account.setDeviceKey(devicekey);
				account.setClientKey(listener.mClientKey);
				
				account.setUsedSpace(usedSpace);
				account.setUsableSpace(usableSpace);
				account.setFreeSpace(freeSpace);
				account.setPurchased(purchased);
				account.setCapacity(capacity);
				
				if (listener.mAccountId <= 0) {
					account.setCreateTime(current);
					account.setKeygenTime(current);
				}
				
				account.setUpdateTime(current);
				account.setPrefix(AnyboxApp.PREFIX);
				account.setFlag(AccountData.FLAG_OK);
				account.setStatus(AccountData.STATUS_LOGIN);
				account.setFailedCode(0);
				account.setFailedMessage("");
				
				long accountId = account.commitUpdates();
				
				HostData hostSaved = ContentHelper.getInstance().getHost(hostId);
				if (hostSaved != null) {
					app.setHostDisplayName(hostSaved.getDisplayName());
					app.setHostAddressPort(hostSaved.getRequestAddressPort());
				}
				
				AccountData[] accounts = loadAccounts(app, true);
				if (accounts != null) {
					for (AccountData ad : accounts) {
						if (ad != null && ad.getId() == accountId) { 
							HostData host = ContentHelper.getInstance().getHost(ad.getHostId());
							if (host == null) continue;
							
							AnyboxAccount user = app.createAccount(ad, host, listener.mRequestTime);
									//new AnyboxAccount(app, ad, 
									//listener.mData, listener.mError, listener.mRequestTime);
							app.setAccount(user);
							
							if (LOG.isDebugEnabled())
								LOG.debug("onAuthenticated: user=" + user);
							
							AccountAuth.Result res = AccountAuth.Result.AUTH_SUCCESS;
							if (listener.mAction == AccountAuth.Action.REGISTER)
								res = AccountAuth.Result.REGISTER_SUCCESS;
							else if (listener.mAction == AccountAuth.Action.LOGIN)
								res = AccountAuth.Result.LOGIN_SUCCESS;
							
							AccountAuth result = new AccountAuth(res, listener.mError);
							result.setAccountId(accountId);
							return result;
						}
					}
				}
			}
		}
		
		AccountAuth.Result res = AccountAuth.Result.AUTH_FAILED;
		if (listener.mAction == AccountAuth.Action.REGISTER)
			res = AccountAuth.Result.REGISTER_FAILED;
		else if (listener.mAction == AccountAuth.Action.LOGIN)
			res = AccountAuth.Result.LOGIN_FAILED;
		
		AccountAuth result = new AccountAuth(res, listener.mError);
		result.setAccountId(listener.mAccountId);
		return result;
	}
	
	static AccountAuth doLogin(AnyboxApp app, AccountAuth.Callback cb, 
			String username, String password, String email, 
			boolean registerMode) throws IOException { 
		if (app == null) throw new NullPointerException();
		//if (!app.isInited()) app.doInit();
		
		if (username == null || username.length() == 0)
			throw new IllegalArgumentException("Username is empty");
		
		if (password == null || password.length() == 0)
			throw new IllegalArgumentException("Password is empty");
		
		if (registerMode) {
			if (email == null || email.length() == 0)
				throw new IllegalArgumentException("Email is empty");
			
			AuthListener listener = new AuthListener(app, username, 
					AccountAuth.Action.REGISTER, -1, ActionError.Action.ACCOUNT_REGISTER);
			
			String url = app.getRequestAddr(null, false) 
					+ "/user/login?wt=secretjson&action=registerlogin" 
					+ "&secret.email=" + StringUtils.URLEncode(Base64Util.encodeSecret(email)) 
					+ "&secret.username=" + StringUtils.URLEncode(Base64Util.encodeSecret(username)) 
					+ "&secret.password=" + StringUtils.URLEncode(Base64Util.encodeSecret(password))
					+ getAppParams(app, listener.mClientKey);
			
			
			if (cb != null) cb.onRequestStart(app, listener.getAuthAction());
			AnyboxApi.request(url, listener);
			
			if (listener.mData != null) 
				return onAuthenticated(app, listener);
			
			AccountAuth result = new AccountAuth(AccountAuth.Result.REGISTER_FAILED, listener.mError);
			result.setHostName(app.getRequestAddr(null, false));
			return result;
			
		} else {
			AuthListener listener = new AuthListener(app, username, 
					AccountAuth.Action.LOGIN, -1, ActionError.Action.ACCOUNT_LOGIN);
			
			String url = app.getRequestAddr(null, false) 
					+ "/user/login?wt=secretjson&action=login" 
					+ "&secret.username=" + StringUtils.URLEncode(Base64Util.encodeSecret(username)) 
					+ "&secret.password=" + StringUtils.URLEncode(Base64Util.encodeSecret(password))
					+ getAppParams(app, listener.mClientKey);
			
			if (cb != null) cb.onRequestStart(app, listener.getAuthAction());
			AnyboxApi.request(url, listener);
			
			if (listener.mData != null) 
				return onAuthenticated(app, listener);
			
			AccountAuth result = new AccountAuth(AccountAuth.Result.LOGIN_FAILED, listener.mError);
			result.setHostName(app.getRequestAddr(null, false));
			return result;
		}
	}
	
	private static AccountAuth doAuth(AnyboxApp app, 
			AccountData account, AccountAuth.Callback cb) throws IOException { 
		if (app == null || account == null) throw new NullPointerException();
		
		final long accountId = account.getId();
		final String username = account.getUserName();
		final String devicekey = account.getDeviceKey();
		final String clientkey = account.getClientKey();
		final String authkey = account.getAuthKey();
		
		if (username == null || username.length() == 0)
			throw new IllegalArgumentException("Username is empty");
		
		if (devicekey == null || devicekey.length() == 0)
			throw new IllegalArgumentException("Device key is empty");
		
		if (clientkey == null || clientkey.length() == 0)
			throw new IllegalArgumentException("Client key is empty");
		
		if (authkey == null || authkey.length() == 0)
			throw new IllegalArgumentException("Auth key is empty");
		
		AuthListener listener = new AuthListener(app, username, 
				AccountAuth.Action.AUTH, accountId, clientkey, 
				ActionError.Action.ACCOUNT_AUTH);
	
		HostData host = ContentHelper.getInstance().getHost(account.getHostId());
		
		String hostaddr = app.getRequestAddr(host, true) ;
		String url = hostaddr
				+ "/user/login?wt=secretjson&action=authlogin"
				+ "&secret.username=" + StringUtils.URLEncode(Base64Util.encodeSecret(username)) 
				+ "&secret.devicekey=" + StringUtils.URLEncode(Base64Util.encodeSecret(devicekey))
				+ "&secret.authkey=" + StringUtils.URLEncode(Base64Util.encodeSecret(authkey))
				+ getAppParams(app, listener.mClientKey);
		
		if (cb != null) cb.onRequestStart(app, listener.getAuthAction());
		AnyboxApi.request(url, listener);
		
		if (listener.mData != null) 
			return onAuthenticated(app, listener);
		
		ActionError error = listener.mError;
		try {
			AccountData updateData = account.startUpdate();
			if (updateData != null) {
				updateData.setStatus(AccountData.STATUS_ERROR);
				if (error != null) {
					updateData.setFailedCode(error.getCode());
					updateData.setFailedMessage(error.getMessage());
				} else {
					updateData.setFailedCode(-1);
					updateData.setFailedMessage("auth failed");
				}
				updateData.commitUpdates();
			}
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("doAuth: update error: " + e, e);
		}
		
		AccountAuth result = new AccountAuth(AccountAuth.Result.AUTH_FAILED, error);
		result.setAccountId(accountId);
		result.setHostName(hostaddr);
		return result;
	}
	
	static class AuthListener extends AnyboxApi.SecretJSONListener {
		@SuppressWarnings("unused")
		private final AnyboxApp mApp;
		private final AccountAuth.Action mAction;
		private final ActionError.Action mErrorAction;
		private final String mClientKey;
		private final String mAuthName;
		private final long mAccountId;
		
		private AnyboxData mData = null;
		private ActionError mError = null;
		private long mRequestTime = 0;
		
		public AuthListener(AnyboxApp app, String username, 
				AccountAuth.Action action, long accountId, ActionError.Action ea) {
			this(app, username, action, accountId, 
					Utilities.toMD5(username + "@" + System.currentTimeMillis()), 
					ea);
		}
		
		public AuthListener(AnyboxApp app, String username, 
				AccountAuth.Action action, long accountId, String clientkey, 
				ActionError.Action ea) {
			if (app == null || username == null || clientkey == null || ea == null) 
				throw new NullPointerException();
			mApp = app;
			mAction = action;
			mErrorAction = ea;
			mAccountId = accountId;
			mClientKey = clientkey;
			mAuthName = username;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mRequestTime = System.currentTimeMillis();
			mData = data;
			mError = error;
			
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
			return mErrorAction; //ActionError.Action.ACCOUNT_AUTH;
		}
		
		public AccountAuth.Action getAuthAction() {
			return mAction;
		}
	}
	
	static String getAppParams(AnyboxApp app, String clientkey) throws IOException {
		String lang = app.getLocalString();
		String apptype = AnyboxApp.DEVICE_TYPE;
		
		PackageInfo packageInfo;
        try {
            packageInfo = app.getContext().getPackageManager().getPackageInfo(
            		app.getContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("getPackageInfo failed");
        }
        String applang = lang;
        String appname = packageInfo.packageName;
        String appversion = String.format("%s (%s/%s; %s/%s/%s/%s; %s/%s/%s)",
        		Integer.toString(packageInfo.versionCode),
                packageInfo.packageName,
                packageInfo.versionName,
                Build.BRAND,
                Build.DEVICE,
                Build.MODEL,
                Build.ID,
                Build.VERSION.SDK_INT,
                Build.VERSION.RELEASE,
                Build.VERSION.INCREMENTAL);
		
		return "&secret.lang=" + StringUtils.URLEncode(Base64Util.encodeSecret(lang))
				+ "&secret.clientkey=" + StringUtils.URLEncode(Base64Util.encodeSecret(clientkey))
				+ "&secret.apptype=" + StringUtils.URLEncode(Base64Util.encodeSecret(apptype))
				+ "&secret.applang=" + StringUtils.URLEncode(Base64Util.encodeSecret(applang))
				+ "&secret.appname=" + StringUtils.URLEncode(Base64Util.encodeSecret(appname))
				+ "&secret.appversion=" + StringUtils.URLEncode(Base64Util.encodeSecret(appversion));
	}
	
	static String getInitParams(AnyboxApp app) throws IOException {
		//String lang = app.getLocalString();
		String apptype = AnyboxApp.DEVICE_TYPE;
		
		PackageInfo packageInfo;
        try {
            packageInfo = app.getContext().getPackageManager().getPackageInfo(
            		app.getContext().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("getPackageInfo failed");
        }
        //String applang = lang;
        //String appname = packageInfo.packageName;
        String appversion = String.format("%s (%s/%s; %s/%s/%s/%s; %s/%s/%s)",
        		Integer.toString(packageInfo.versionCode),
                packageInfo.packageName,
                packageInfo.versionName,
                Build.BRAND,
                Build.DEVICE,
                Build.MODEL,
                Build.ID,
                Build.VERSION.SDK_INT,
                Build.VERSION.RELEASE,
                Build.VERSION.INCREMENTAL);
		
        String devicekey = IdentityUtils.newKey(appversion, 8);
        
		return "&secret.appkey=" + StringUtils.URLEncode(Base64Util.encodeSecret(devicekey))
				+ "&secret.apptype=" + StringUtils.URLEncode(Base64Util.encodeSecret(apptype));
	}
	
	static boolean isEmpty(String... vals) {
		if (vals != null && vals.length > 0) {
			for (String val : vals) {
				if (val == null || val.length() == 0)
					return true;
			}
			return false;
		}
		return true;
	}
	
}
