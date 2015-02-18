package org.javenstudio.android.account;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.account.AccountInfoProvider;
import org.javenstudio.provider.account.dashboard.DashboardProvider;
import org.javenstudio.provider.account.notify.NotifyProvider;
import org.javenstudio.provider.account.notify.OnNotifyChangeListener;
import org.javenstudio.provider.account.space.SpacesProvider;
import org.javenstudio.provider.library.list.LibrariesProvider;
import org.javenstudio.provider.task.TaskListProvider;

public abstract class AccountUser implements AppUser {
	private static final Logger LOG = Logger.getLogger(AccountUser.class);

	public static final long SHORT_DELAY_MILLIS = 1l * 60 * 1000;
	public static final long LONG_DELAY_MILLIS = 5l * 60 * 1000;
	
	public static enum DataType {
		ACCOUNTINFO, ANNOUNCEMENT
	}
	
	public static enum DataState {
		UPDATED
	}
	
	public static interface OnDataChangeListener {
		public void onDataChanged(AccountUser user, DataType type, DataState state);
	}
	
	public abstract long getAccountId();
	public abstract String getAccountName();
	public abstract String getAccountFullname();
	public abstract String getMailAddress();
	
	public String getUserTitle() { return getAccountName(); }
	public String getUserEmail() { return null; }
	public String getNickName() { return null; }
	public String getHostDisplayName() { return null; }
	
	public long getTotalRemainingSpace() { return 0; }
	public long getTotalCapacitySpace() { return 0; }
	public float getTotalUsedPercent() { return 0; }
	
	public long getUpdateTime() { return 0; }
	public long getHeartbeatDelayMillis() { return LONG_DELAY_MILLIS; }
	public boolean hasNotification() { return false; }
	
	public AccountInfoProvider getAccountProvider() { return null; }
	public SpacesProvider getSpacesProvider() { return null; }
	public NotifyProvider getNotifyProvider() { return null; }
	public DashboardProvider getDashboardProvider() { return null; }
	public LibrariesProvider getLibrariesProvider() { return null; }
	public TaskListProvider getTaskListProvider() { return null; }
	
	private OnNotifyChangeListener mOnNotifyChangeListener = null;
	public OnNotifyChangeListener getOnNotifyChangeListener() { return mOnNotifyChangeListener; }
	public void setOnNotifyChangeListener(OnNotifyChangeListener l) { mOnNotifyChangeListener = l; }
	
	@Override
	public boolean equals(Object obj) { 
		if (obj == this) return true;
		if (obj == null || !(obj instanceof AccountUser)) 
			return false;
		
		AccountUser other = (AccountUser)obj;
		
		return AccountRegistry.isEquals(this.getAccountName(), other.getAccountName()) && 
			   AccountRegistry.isEquals(this.getUserId(), other.getUserId());
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{accountName=" + getAccountName() 
				+ ",userId=" + getUserId() + ",userTitle=" + getUserTitle() + "}";
	}
	
	private final List<WeakReference<OnDataChangeListener>> mListeners = 
			new ArrayList<WeakReference<OnDataChangeListener>>();
	
	public final void addListener(OnDataChangeListener listener) {
		if (LOG.isDebugEnabled())
			LOG.debug("addListener: listener=" + listener);
		
		synchronized (mListeners) {
			boolean found = false; 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<OnDataChangeListener> ref = mListeners.get(i); 
    			OnDataChangeListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		if (!found && listener != null) {
    			mListeners.add(new WeakReference<OnDataChangeListener>(listener)); 
    			onListenerAdded(listener);
    		}
		}
	}
	
	public final void removeListener(OnDataChangeListener listener) {
		if (LOG.isDebugEnabled())
			LOG.debug("removeListener: listener=" + listener);
		
		synchronized (mListeners) {
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<OnDataChangeListener> ref = mListeners.get(i); 
    			OnDataChangeListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null || lsr == listener) { 
    				mListeners.remove(i); continue; 
    			}
    			i ++; 
    		}
		}
	}
	
	protected final void dispatchChanged(DataType type, DataState state) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("dispatchChanged: user=" + this 
					+ " type=" + type + " state=" + state);
		}
		
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<OnDataChangeListener> ref = mListeners.get(i); 
    			OnDataChangeListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListeners.remove(i); continue; 
    			} else { 
    				try {
    					listener.onDataChanged(this, type, state);
    				} catch (Throwable e) {
    					if (LOG.isWarnEnabled())
    						LOG.warn("dispatchChanged: error: " + e, e);
    				}
    			}
    			i ++; 
    		}
    	}
    }
	
	public void onListenerAdded(OnDataChangeListener listener) {}
	public void onNotifyMenuOpened() {}
	
}
