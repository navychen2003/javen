package org.javenstudio.provider.app.anybox;

import java.io.IOException;

import android.app.Activity;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.common.util.Logger;
import org.javenstudio.util.StringUtils;

public class AnyboxLogout {
	private static final Logger LOG = Logger.getLogger(AnyboxLogout.class);
	
	public static interface LogoutCallback {
		public void onLogout(AnyboxAccount user, boolean success);
	}
	
	public static interface LogoutTaskCallback {
		public void onLogoutFailed(AnyboxAccount user, ActionError error);
		public void onLogoutDone(AnyboxAccount user);
		public void onLogoutStart(AnyboxAccount user);
	}
	
	private final AnyboxAccount mUser;
	private final AnyboxData mData;
	private final ActionError mError;
	private final long mRequestTime;
	
	private AnyboxLogout(AnyboxAccount user, 
			AnyboxData data, ActionError error, long requestTime) {
		if (user == null) throw new NullPointerException();
		mUser = user;
		mData = data;
		mError = error;
		mRequestTime = requestTime;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	public AnyboxData getData() { return mData; }
	public ActionError getError() { return mError; }
	public long getRequestTime() { return mRequestTime; }
	
	private void onLogoutDone(AnyboxData data) throws IOException {
		if (data == null) return;
		
		AccountData accountData = getUser().getAccountData();
		if (accountData != null) {
			AccountData updateData = accountData.startUpdate();
			if (updateData != null) {
				updateData.setStatus(AccountData.STATUS_LOGOUT);
				updateData.setFailedCode(0);
				updateData.setFailedMessage("");
				updateData.commitUpdates();
			}
		}
	}
	
	private void onLogoutFailed(ActionError error) throws IOException {
		AccountData accountData = getUser().getAccountData();
		if (accountData != null) {
			AccountData updateData = accountData.startUpdate();
			if (updateData != null) {
				updateData.setStatus(AccountData.STATUS_ERROR);
				if (error != null) {
					updateData.setFailedCode(error.getCode());
					updateData.setFailedMessage(error.getMessage());
				} else {
					updateData.setFailedCode(-1);
					updateData.setFailedMessage("logout failed");
				}
				updateData.commitUpdates();
			}
		}
	}
	
	static void scheduleLogout(AnyboxAccount user, final Activity activity, 
			final LogoutCallback callback) {
		if (user == null || activity == null) return;
		
		AnyboxLogout.LogoutTaskCallback taskCallback = new AnyboxLogout.LogoutTaskCallback() {
				@Override
				public void onLogoutFailed(AnyboxAccount user, ActionError error) {
					if (LOG.isDebugEnabled())
						LOG.debug("onLogoutFailed: user=" + user + " error=" + error);
					
					if (activity != null && activity instanceof IActivity) {
						IActivity iactivity = (IActivity)activity;
						iactivity.getActivityHelper().postHideProgressAlert();
					}
					//AppResources.getInstance().startLoginActivity(activity, action, accountEmail);
					if (callback != null) callback.onLogout(user, false);
				}
				@Override
				public void onLogoutDone(AnyboxAccount user) {
					if (LOG.isDebugEnabled())
						LOG.debug("onLogoutDone: user=" + user);
					
					if (activity != null && activity instanceof IActivity) {
						IActivity iactivity = (IActivity)activity;
						iactivity.getActivityHelper().postHideProgressAlert();
					}
					//AppResources.getInstance().startLoginActivity(activity, action, accountEmail);
					if (callback != null) callback.onLogout(user, true);
				}
				@Override
				public void onLogoutStart(AnyboxAccount user) {
					if (LOG.isDebugEnabled())
						LOG.debug("onLogoutStart: user=" + user);
					
					if (activity != null && activity instanceof IActivity) {
						IActivity iactivity = (IActivity)activity;
						iactivity.getActivityHelper().postShowProgressAlert(
								AppResources.getInstance().getStringText(AppResources.string.login_signingout_message));
					}
				}
			};
		
		user.getApp().getWork().scheduleLogout(user, taskCallback, 0);
	}
	
	static ActionError doLogout(AnyboxAccount user, LogoutTaskCallback callback) {
		if (user == null) return null;
		
		String url = user.getApp().getRequestAddr(user.getHostData(), false) 
				+ "/user/login?wt=secretjson&action=logout" 
				+ "&token=" + StringUtils.URLEncode(user.getAuthToken());
		
		if (callback != null)
			callback.onLogoutStart(user);
		
		LogoutListener listener = new LogoutListener(user);
		AnyboxApi.request(url, listener);
		
		AnyboxLogout logout = listener.mLogout;
		ActionError error = null;
		boolean success = false;
		
		try {
			if (logout != null) {
				error = logout.getError();
				if (error == null || error.getCode() == 0) {
					error = null;
					
					logout.onLogoutDone(logout.getData());
					user.setLogoutInfo(logout);
					user.getApp().setAccount(null);
					
					success = true;
					if (callback != null)
						callback.onLogoutDone(user);
				}
			}
		} catch (IOException e) {
			if (error == null) {
				error = new ActionError(listener.getErrorAction(), 
						-1, e.getMessage(), null, e);
			}
			if (LOG.isErrorEnabled())
				LOG.error("doLogout: error: " + e, e);
		}
		
		if (callback != null && !success) {
			try {
				if (logout != null) 
					logout.onLogoutFailed(error);
			} catch (Throwable e) {
				if (LOG.isWarnEnabled())
					LOG.warn("doLogout: onLogoutFailed error: " + e, e);
			}
			callback.onLogoutFailed(user, error);
		}
		
		return error;
	}
	
	static class LogoutListener extends AnyboxApi.SecretJSONListener {
		private final AnyboxAccount mUser;
		private AnyboxLogout mLogout = null;
		
		public LogoutListener(AnyboxAccount user) {
			if (user == null) throw new NullPointerException();
			mUser = user;
		}
		
		@Override
		public void handleData(AnyboxData data, ActionError error) {
			mLogout = new AnyboxLogout(mUser, data, error, System.currentTimeMillis());
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
			return ActionError.Action.ACCOUNT_LOGOUT;
		}
	}
	
}
