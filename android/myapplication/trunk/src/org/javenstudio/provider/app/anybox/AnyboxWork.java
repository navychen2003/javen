package org.javenstudio.provider.app.anybox;

import java.util.concurrent.atomic.AtomicLong;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.account.AccountWork;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.worker.work.Work;
import org.javenstudio.common.util.Logger;

public class AnyboxWork extends AccountWork {
	private static final Logger LOG = Logger.getLogger(AnyboxWork.class);

	private static final AtomicLong sHeartbeatCounter = new AtomicLong(0);
	
	private final Object mHeartbeatLock = new Object();
	private HeartbeatWork mHeartbeatWork = null;
	
	private final Object mAccountInfoLock = new Object();
	private AccountInfoWork mAccountInfoWork = null;
	
	private final Object mLogoutLock = new Object();
	private LogoutWork mLogoutWork = null;
	
	private volatile boolean mHeartbeatRunning = false;
	private volatile boolean mAccountInfoRunning = false;
	private volatile boolean mLogoutRunning = false;
	
	AnyboxWork() {}
	
	@Override
	public void stopAll() {
		mHeartbeatWork = null;
		mAccountInfoWork = null;
	}
	
	@Override
	protected void onListenerAdded(AccountUser user, OnStateChangeListener listener) {
		if (user == null || listener == null) return;
		
		if (mAccountInfoRunning) {
			try {
				listener.onStateChanged(user, WorkType.ACCOUNTINFO, WorkState.RUNNING);
			} catch (Throwable e) {
				if (LOG.isWarnEnabled())
					LOG.warn("onListenerAdded: error: " + e, e);
			}
		}
		
		if (mHeartbeatRunning) {
			try {
				listener.onStateChanged(user, WorkType.HEARTBEAT, WorkState.RUNNING);
			} catch (Throwable e) {
				if (LOG.isWarnEnabled())
					LOG.warn("onListenerAdded: error: " + e, e);
			}
		}
		
		if (mLogoutRunning) {
			try {
				listener.onStateChanged(user, WorkType.LOGOUT, WorkState.RUNNING);
			} catch (Throwable e) {
				if (LOG.isWarnEnabled())
					LOG.warn("onListenerAdded: error: " + e, e);
			}
		}
	}
	
	@Override
	public boolean isWorkRunning(AccountUser user, WorkType type) {
		if (type == WorkType.ACCOUNTINFO) {
			return mAccountInfoRunning;
			
		} else if (type == WorkType.HEARTBEAT) {
			return mHeartbeatRunning;
			
		} else if (type == WorkType.LOGOUT) {
			return mLogoutRunning;
			
		}
		
		return false;
	}
	
	@Override
	public void scheduleWork(AccountUser user, WorkType type, long delayMillis) {
		if (user == null || !(user instanceof AnyboxAccount)) 
			return;
		
		if (type == WorkType.ACCOUNTINFO) {
			scheduleAccountInfo((AnyboxAccount)user, delayMillis);
			
		} else if (type == WorkType.HEARTBEAT) {
			scheduleHeartbeat((AnyboxAccount)user, delayMillis);
			
		} else if (type == WorkType.LOGOUT) {
			//scheduleLogout((AnyboxAccount)user, delayMillis);
			
		} else {
			if (LOG.isWarnEnabled())
				LOG.warn("scheduleWork: unsupported work type: " + type);
		}
	}
	
	public void scheduleLogout(AnyboxAccount user, 
			AnyboxLogout.LogoutTaskCallback callback, long delayMillis) {
		if (user == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("scheduleLogout: user=" + user 
					+ ", delayMillis=" + delayMillis);
		}
		
		try {
			LogoutWork work = new LogoutWork(user, callback);
			mLogoutWork = work;
			
			ResourceHelper.getScheduler().postDelayed(work, delayMillis);
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("scheduleLogout: error: " + e, e);
		}
	}
	
	private void doLogoutOnThread(LogoutWork work, 
			AnyboxLogout.LogoutTaskCallback callback) {
		synchronized (mHeartbeatLock) {
			synchronized (mAccountInfoLock) {
				doLogoutOnThread0(work, callback);
			}
		}
	}
	
	private void doLogoutOnThread0(LogoutWork work, 
			AnyboxLogout.LogoutTaskCallback callback) {
		synchronized (mLogoutLock) {
			if (work == null || work != mLogoutWork) return;
			mLogoutWork = null;
			
			if (LOG.isDebugEnabled())
				LOG.debug("doLogoutOnThread: work=" + work);
			
			AnyboxAccount user = work.mUser;
			try {
				if (user != null) {
					mLogoutRunning = true;
					dispatchChanged(user, WorkType.LOGOUT, WorkState.RUNNING);
					
					ActionError error = AnyboxLogout.doLogout(user, callback);
					if (error == null || error.getCode() == 0) 
						scheduleHeartbeat(user, user.getHeartbeatDelayMillis());
				}
			} finally {
				boolean running = mLogoutRunning;
				mLogoutRunning = false;
				
				if (user != null && running) 
					dispatchChanged(user, WorkType.LOGOUT, WorkState.STOPPED);
			}
		}
	}
	
	private void scheduleAccountInfo(AnyboxAccount user, long delayMillis) {
		if (user == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("scheduleAccountInfo: user=" + user 
					+ ", delayMillis=" + delayMillis);
		}
		
		try {
			AccountInfoWork work = new AccountInfoWork(user);
			mAccountInfoWork = work;
			
			ResourceHelper.getScheduler().postDelayed(work, delayMillis);
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("scheduleAccountInfo: error: " + e, e);
		}
	}
	
	private void doAccountInfoOnThread(AccountInfoWork work) {
		synchronized (mAccountInfoLock) {
			if (work == null || work != mAccountInfoWork) return;
			mAccountInfoWork = null;
			
			if (LOG.isDebugEnabled())
				LOG.debug("doAccountInfoOnThread: work=" + work);
			
			AnyboxAccount user = work.mUser;
			try {
				if (user != null) {
					mAccountInfoRunning = true;
					dispatchChanged(user, WorkType.ACCOUNTINFO, WorkState.RUNNING);
					
					ActionError error = AnyboxAccountInfo.getAccountInfo(user, null);
					if (error == null || error.getCode() == 0) 
						scheduleHeartbeat(user, user.getHeartbeatDelayMillis());
				}
			} finally {
				boolean running = mAccountInfoRunning;
				mAccountInfoRunning = false;
				
				if (user != null && running) 
					dispatchChanged(user, WorkType.ACCOUNTINFO, WorkState.STOPPED);
			}
		}
	}
	
	private void scheduleHeartbeat(AnyboxAccount user, long delayMillis) {
		if (user == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("scheduleHeartbeat: user=" + user 
					+ ", delayMillis=" + delayMillis);
		}
		
		try {
			HeartbeatWork work = new HeartbeatWork(user);
			mHeartbeatWork = work;
			
			ResourceHelper.getScheduler().postDelayed(work, delayMillis);
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("scheduleHeartbeat: error: " + e, e);
		}
	}
	
	private void doHeartbeatOnThread(HeartbeatWork work) {
		synchronized (mHeartbeatLock) {
			if (work == null || work != mHeartbeatWork) return;
			mHeartbeatWork = null;
			
			if (LOG.isDebugEnabled())
				LOG.debug("doHeartbeatOnThread: work=" + work);
			
			AnyboxAccount user = work.mUser;
			try {
				if (user != null) {
					if (user.getApp().getAccount() != user)
						return;
					
					mHeartbeatRunning = true;
					dispatchChanged(user, WorkType.HEARTBEAT, WorkState.RUNNING);
					
					ActionError error = AnyboxHeartbeat.getHeartbeat(user, null);
					if (error == null || error.getCode() == 0) {
						if (user.getApp().getAccount() == user)
							scheduleHeartbeat(user, user.getHeartbeatDelayMillis());
					}
				}
			} finally {
				boolean running = mHeartbeatRunning;
				mHeartbeatRunning = false;
				
				if (user != null && running) 
					dispatchChanged(user, WorkType.HEARTBEAT, WorkState.STOPPED);
			}
		}
	}
	
	private class HeartbeatWork extends Work {
		private final AnyboxAccount mUser;
		
		public HeartbeatWork(AnyboxAccount user) {
			super("heartbeat-" + user.getAccountName() 
					+ "-" + sHeartbeatCounter.incrementAndGet());
			mUser = user;
		}

		@Override
		public void onRun() {
			doHeartbeatOnThread(this);
		}
	}
	
	private class AccountInfoWork extends Work {
		private final AnyboxAccount mUser;
		
		public AccountInfoWork(AnyboxAccount user) {
			super("accountinfo-" + user.getAccountName() 
					+ "-" + sHeartbeatCounter.incrementAndGet());
			mUser = user;
		}

		@Override
		public void onRun() {
			doAccountInfoOnThread(this);
		}
	}
	
	private class LogoutWork extends Work {
		private final AnyboxAccount mUser;
		private final AnyboxLogout.LogoutTaskCallback mCallback;
		
		public LogoutWork(AnyboxAccount user, AnyboxLogout.LogoutTaskCallback callback) {
			super("logout-" + user.getAccountName() 
					+ "-" + sHeartbeatCounter.incrementAndGet());
			mUser = user;
			mCallback = callback;
		}

		@Override
		public void onRun() {
			doLogoutOnThread(this, mCallback);
		}
	}
	
}
