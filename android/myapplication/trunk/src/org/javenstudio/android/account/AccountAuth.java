package org.javenstudio.android.account;

import org.javenstudio.android.ActionError;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.work.Work;
import org.javenstudio.common.util.Logger;

public class AccountAuth {
	private static final Logger LOG = Logger.getLogger(AccountAuth.class);

	public static enum Action {
		REGISTER, LOGIN, AUTH, INIT
	}
	
	public static enum Result {
		REGISTER_FAILED, LOGIN_FAILED, AUTH_FAILED, 
		REGISTER_SUCCESS, LOGIN_SUCCESS, AUTH_SUCCESS, 
		NO_ACCOUNT, SELECT_ACCOUNT, AUTHENTICATED
	}
	
	public static interface Callback {
		public void onWorkStart(AccountApp app, Action action);
		public void onRequestStart(AccountApp app, Action action);
		public void onWorkDone(AccountApp app, AccountAuth result);
		public void onWorkError(AccountApp app, AccountAuth result);
	}
	
	private final Result mResult;
	private final ActionError mError;
	
	private String mHostName = null;
	private long mAccountId = 0;
	
	public AccountAuth(Result res, ActionError error) {
		if (res == null) throw new NullPointerException();
		mResult = res;
		mError = error;
	}
	
	public Result getResult() { return mResult; }
	public ActionError getError() { return mError; }
	
	public Throwable getException() {
		if (mError != null) return mError.getException();
		return null;
	}
	
	public String getHostName() { return mHostName; }
	public void setHostName(String hostname) { mHostName = hostname; }
	
	public long getAccountId() { return mAccountId; }
	public void setAccountId(long id) { mAccountId = id; }
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{result=" + mResult 
				+ ",error=" + mError + ",accountId=" + mAccountId 
				+ ",hostname=" + mHostName + "}";
	}
	
	public static boolean startInitWork(final AccountApp app, 
			final AccountAuth.Callback cb, final long delayedMillis) {
		if (app == null/* || app.isInited()*/) return false;
		try {
			ResourceHelper.getScheduler().postDelayed(new Work("init") {
					@Override
					public void onRun() {
						try {
							if (cb != null) cb.onWorkStart(app, Action.INIT);
							app.doInitWork(this, cb);
							if (cb != null) cb.onWorkDone(app, null);
						} catch (Throwable e) {
							if (cb != null) cb.onWorkError(app, null);
							if (LOG.isWarnEnabled())
								LOG.warn("startInitWork: do Init error: " + e, e);
						}
					}
				}, delayedMillis);
			return true;
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("startInitWork: error: " + e, e);
		}
		return false;
	}
	
	public static boolean startAuthWork(final AccountApp app, 
			final AccountAuth.Callback cb, final String accountEmail, 
			final long delayedMillis) {
		if (app == null) return false;
		try {
			ResourceHelper.getScheduler().postDelayed(new Work("auth") {
					@Override
					public void onRun() {
						try {
							AccountAuth result = app.doAuth(cb, accountEmail, true);
							if (result == null) {
								if (cb != null) cb.onWorkStart(app, Action.AUTH);
								result = app.doAuthWork(this, cb, accountEmail, false);
							}
							if (cb != null) cb.onWorkDone(app, result);
							
						} catch (Throwable e) {
							if (LOG.isWarnEnabled())
								LOG.warn("startAuthWork: doAuth error: " + e, e);
							
							ActionError error = new ActionError(ActionError.Action.ACCOUNT_AUTH, 
									-1, "auth exception", null, e);
							
							AccountAuth result = new AccountAuth(AccountAuth.Result.AUTH_FAILED, error);
							if (cb != null) cb.onWorkError(app, result);
						}
					}
				}, delayedMillis);
			return true;
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("startAuthWork: error: " + e, e);
		}
		return false;
	}
	
	public static boolean startLoginWork(final AccountApp app, 
			final String username, final String password, final String email, 
			final boolean registerMode, final AccountAuth.Callback cb) {
		if (app == null) return false;
		try {
			ResourceHelper.getScheduler().post(new Work("login") {
					@Override
					public void onRun() {
						try {
							if (cb != null) cb.onWorkStart(app, Action.LOGIN);
							AccountAuth result = app.doLoginWork(this, cb, 
									username, password, email, registerMode);
							if (cb != null) cb.onWorkDone(app, result);
							
						} catch (Throwable e) {
							if (LOG.isWarnEnabled())
								LOG.warn("startAuthWork: doLogin error: " + e, e);
							
							ActionError error = new ActionError(ActionError.Action.ACCOUNT_LOGIN, 
									-1, "login exception", null, e);
							
							AccountAuth result = new AccountAuth(registerMode ? 
									AccountAuth.Result.REGISTER_FAILED : AccountAuth.Result.LOGIN_FAILED, 
									error);
							if (cb != null) cb.onWorkError(app, result);
						}
					}
				});
			return true;
		} catch (Throwable e) {
			if (LOG.isErrorEnabled())
				LOG.error("startLoginWork: error: " + e, e);
		}
		return false;
	}
	
}
