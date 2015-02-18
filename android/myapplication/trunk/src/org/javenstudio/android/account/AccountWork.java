package org.javenstudio.android.account;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.util.Logger;

public abstract class AccountWork {
	private static final Logger LOG = Logger.getLogger(AccountWork.class);

	public static enum WorkType {
		HEARTBEAT, ACCOUNTINFO, LOGOUT
	}
	
	public static enum WorkState {
		RUNNING, STOPPED
	}
	
	public static interface OnStateChangeListener {
		public void onStateChanged(AccountUser user, WorkType type, WorkState state);
	}
	
	public abstract void scheduleWork(AccountUser user, WorkType type, long delayMillis);
	
	public abstract boolean isWorkRunning(AccountUser user, WorkType type);
	public abstract void stopAll();
	
	private final List<WeakReference<OnStateChangeListener>> mListeners = 
			new ArrayList<WeakReference<OnStateChangeListener>>();
	
	public final void addListener(AccountUser user, OnStateChangeListener listener) {
		if (LOG.isDebugEnabled())
			LOG.debug("addListener: user=" + user + " listener=" + listener);
		
		synchronized (mListeners) {
			boolean found = false; 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<OnStateChangeListener> ref = mListeners.get(i); 
    			OnStateChangeListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		if (!found && listener != null) {
    			mListeners.add(new WeakReference<OnStateChangeListener>(listener)); 
    			onListenerAdded(user, listener);
    		}
		}
	}
	
	public final void removeListener(OnStateChangeListener listener) {
		if (LOG.isDebugEnabled())
			LOG.debug("removeListener: listener=" + listener);
		
		synchronized (mListeners) {
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<OnStateChangeListener> ref = mListeners.get(i); 
    			OnStateChangeListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null || lsr == listener) { 
    				mListeners.remove(i); continue; 
    			}
    			i ++; 
    		}
		}
	}
	
	protected final void dispatchChanged(AccountUser user, 
			WorkType type, WorkState state) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("dispatchChanged: user=" + user 
					+ " type=" + type + " state=" + state);
		}
		
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<OnStateChangeListener> ref = mListeners.get(i); 
    			OnStateChangeListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListeners.remove(i); continue; 
    			} else { 
    				try {
    					listener.onStateChanged(user, type, state);
    				} catch (Throwable e) {
    					if (LOG.isWarnEnabled())
    						LOG.warn("dispatchChanged: error: " + e, e);
    				}
    			}
    			i ++; 
    		}
    	}
    }
	
	protected void onListenerAdded(AccountUser user, OnStateChangeListener listener) {
	}
	
}
