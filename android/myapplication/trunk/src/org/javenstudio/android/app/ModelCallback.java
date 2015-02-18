package org.javenstudio.android.app;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.Activity;

import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.common.util.Logger;

public class ModelCallback extends Model.Callback {
	private static final Logger LOG = Logger.getLogger(ModelCallback.class);
	
	private final Set<IActivityListener> mListeners = 
			new HashSet<IActivityListener>();
	
	public final void addListener(IActivityListener listener) { 
		if (listener == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("addListener: listener=" + listener);
		
		synchronized (mListeners) {
			mListeners.add(listener);
		}
	}
	
	public final void removeListener(IActivityListener listener) { 
		if (listener == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("removeListener: listener=" + listener);
		
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}
	
	@Override
	public void onCreate(Activity activity) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onCreate: activity=" + activity);
		
		super.onCreate(activity);
	}
	
	@Override
	public void onStart(Activity activity) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onStart: activity=" + activity);
		
		super.onStart(activity);
	}
	
	@Override
	public void onStop(Activity activity) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onStop: activity=" + activity);
		
		super.onStop(activity);
		
		final IActivityListener[] listeners;
		synchronized (mListeners) {
			listeners = mListeners.toArray(new IActivityListener[mListeners.size()]);
		}
		
		if (listeners != null) {
			for (IActivityListener listener : listeners) { 
				if (LOG.isDebugEnabled())
					LOG.debug("onStop: IActivityListener.onActivityStop: " + listener);
				
				listener.onActivityStop(activity);
			}
		}
	}
	
	@Override
	public void onDestroy(Activity activity) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onDestroy: activity=" + activity);
		
		super.onDestroy(activity);
	}
	
	private static final Map<String, ActivityInfo> sActivityInfos = 
			new HashMap<String, ActivityInfo>();
	
	static ActivityInfo getActivityInfo(Object activity) { 
		if (activity == null) return null;
		
		final String activityKey = activity.toString();
		
		return getActivityInfo(activityKey);
	}
	
	static ActivityInfo getActivityInfo(String activityKey) { 
		synchronized (sActivityInfos) { 
			ActivityInfo info = sActivityInfos.get(activityKey);
			if (info == null) { 
				info = new ActivityInfo(activityKey);
				sActivityInfos.put(activityKey, info);
				
				if (LOG.isDebugEnabled()) 
					LOG.debug("create ActivityInfo: " + activityKey);
			}
			
			return info;
		}
	}
	
	static class ActivityInfo { 
		private final String mActivityKey;
		//private int mProgressCount = 0;
		
		public ActivityInfo(String activityKey) { 
			mActivityKey = activityKey;
		}
		
		public String getActivityKey() { return mActivityKey; }
		//public int getProgressCount() { return mProgressCount; }
	}
	
}
