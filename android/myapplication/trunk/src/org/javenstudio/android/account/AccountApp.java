package org.javenstudio.android.account;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.worker.work.Work;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderController;

public abstract class AccountApp {
	private static final Logger LOG = Logger.getLogger(AccountApp.class);
	
	public static enum LoginAction {
		ADD_ACCOUNT, SWITCH_ACCOUNT, SELECT_ACCOUNT
	}
	
	public abstract Context getContext();
	public abstract DataApp getDataApp();
	public abstract AccountWork getWork();
	public abstract AccountUser getAccount();
	public abstract AccountData[] getAccounts();
	public abstract void resetAccounts();
	public abstract void onAccountRemoved(AccountData account, boolean success);
	
	public abstract String getAccountName(long accountId);
	public abstract String getAccountAvatarURL(AccountData account, int size);
	public abstract String getWelcomeImageUrl();
	public abstract String getWelcomeTitle();
	
	//public abstract boolean isInited();
	protected abstract void doInit(AccountAuth.Callback cb) throws IOException;
	
	protected abstract AccountAuth doAuth(AccountAuth.Callback cb, 
			String accountEmail, boolean checkOnly) throws IOException;
	protected abstract AccountAuth doLogin(AccountAuth.Callback cb, 
			String username, String password, String email, 
			boolean registerMode) throws IOException;
	
	public abstract boolean logout(Activity activity, 
			LoginAction action, String accountEmail);
	
	public abstract ProviderController getNavigationController(Activity activity);
	public abstract ActionItem[] getNavigationItems(Activity activity);
	public abstract Provider getCurrentProvider(Activity activity);
	public abstract Provider setCurrentProvider(Activity activity, Provider provider);
	
	private volatile boolean mActivityStarted = false;
	private volatile boolean mTerminated = false;
	
	public void onTerminate() { mTerminated = true; }
	public void onActivityStart() { mActivityStarted = true; }
	public void onActivityDestroy() { mActivityStarted = false; }
	
	public boolean isActivityRunning() { 
		return mActivityStarted && !mTerminated;
	}
	
	public int getAccountCount() {
		AccountData[] accounts = getAccounts();
		return accounts != null ? accounts.length : 0;
	}
	
	private Work mInitWork = null;
	
	void doInitWork(Work work, AccountAuth.Callback cb) 
			throws IOException {
		if (work == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("doInitWork: work=" + work);
		
		mInitWork = work;
		try {
			doInit(cb);
			dispatchEvent(AppEvent.INITED);
		} finally {
			mInitWork = null;
		}
	}
	
	AccountAuth doAuthWork(Work work, AccountAuth.Callback cb, 
			String accountEmail, boolean checkOnly) throws IOException {
		if (work == null) return null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("doAuthWork: work=" + work 
					+ " checkOnly=" + checkOnly);
		}
		
		AccountAuth result = null;
		try {
			waitForInited();
			result = doAuth(cb, accountEmail, checkOnly);
			dispatchEvent(AppEvent.AUTHED);
		} finally {
		}
		return result;
	}
	
	AccountAuth doLoginWork(Work work, AccountAuth.Callback cb, 
			String username, String password, String email, 
			boolean registerMode) throws IOException {
		if (work == null) return null;
		if (LOG.isDebugEnabled()) {
			LOG.debug("doAuthWork: work=" + work 
					+ " registerMode=" + registerMode);
		}
		
		AccountAuth result = null;
		try {
			waitForInited();
			result = doLogin(cb, username, password, email, registerMode);
			dispatchEvent(AppEvent.LOGGEDIN);
		} finally {
		}
		return result;
	}
	
	private final void waitForInited() {
		if (LOG.isDebugEnabled()) LOG.debug("waitForInited");
		//if (isInited()) return;
		try {
			Work work = mInitWork;
			if (work != null) work.waitForFinished();
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("waitForInited: error: " + e, e);
		}
	}
	
	public enum AppEvent {
		INITED, AUTHED, LOGGEDIN
	}
	
	public interface AppListener {
		public void onAppInited(AccountApp app);
	}
	
	private final List<WeakReference<AppListener>> mListeners = 
			new ArrayList<WeakReference<AppListener>>(); 
	
	public final void addListener(AppListener listener) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("addListener: listener=" + listener);
		
    	synchronized (mListeners) { 
    		boolean found = false; 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<AppListener> ref = mListeners.get(i); 
    			AppListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		if (!found && listener != null) 
    			mListeners.add(new WeakReference<AppListener>(listener)); 
    	}
    }
    
	public final void removeListener(AppListener listener) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("removeListener: listener=" + listener);
		
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<AppListener> ref = mListeners.get(i); 
    			AppListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null || lsr == listener) { 
    				mListeners.remove(i); continue; 
    			}
    			i ++; 
    		}
    	}
    }
	
    protected final void dispatchEvent(AppEvent event) { 
    	if (LOG.isDebugEnabled()) 
    		LOG.debug("dispatchEvent: event=" + event);
    	
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<AppListener> ref = mListeners.get(i); 
    			AppListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListeners.remove(i); continue; 
    			} else { 
    				if (event == AppEvent.INITED)
    					listener.onAppInited(this);
    			}
    			i ++; 
    		}
    	}
    }
	
}
