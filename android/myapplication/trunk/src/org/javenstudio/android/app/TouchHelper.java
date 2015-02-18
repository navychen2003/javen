package org.javenstudio.android.app;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.view.MotionEvent;
import android.widget.AbsListView;

import org.javenstudio.cocoka.graphics.BitmapCache;
import org.javenstudio.cocoka.graphics.TouchListener;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.common.util.Logger;

public class TouchHelper {
	private static final Logger LOG = Logger.getLogger(TouchHelper.class);

	public static BitmapRef loadBitmap(JobContext jc, Job<BitmapRef> job) { 
		if (job == null) return null;
		
		//JobSubmit.submit(job);
		//waitMotionUp(job);
		
		if (isMotionTouching()) { // invalidate when up
			//if (LOG.isDebugEnabled())
			//	LOG.debug("loadBitmap: touching and return null, job=" + job);
			
			return null;
		}
		
		//if (LOG.isDebugEnabled())
		//	LOG.debug("loadBitmap: start load, job=" + job);
		
		return job.run(jc);
	}
	
	private static final Object sLock = new Object();
	private static volatile int sMotionTouching = 0;
	
	public static boolean isMotionTouching() { 
		synchronized (sLock) { 
			return sMotionTouching > 0;
		}
	}
	
	public static void waitMotionUp(Object who) { 
		synchronized (sLock) { 
			int touching = sMotionTouching;
	        while (sMotionTouching > 0) {
	        	if (touching > 0 && LOG.isDebugEnabled()) { 
		        	LOG.debug("waitMotionUp: waiting, who=" + who 
		        			+ " count=" + sMotionTouching);
	        	}
	        	
	            try {
	                sLock.wait();
	            } catch (Throwable ex) {
	            	// ignore
	            }
	        }
	        
	        if (touching > 0 && LOG.isDebugEnabled())
	        	LOG.debug("waitMotionUp: done, who=" + who);
		}
	}
	
	private static void notifyTouchDown(IActivity activity) { 
		if (activity == null) return;
		synchronized (sLock) { 
			if (sMotionTouching < 0) sMotionTouching = 0;
			sMotionTouching ++;
			
			if (LOG.isDebugEnabled())
				LOG.debug("notifyMotionDown: count=" + sMotionTouching);
			
			sLock.notifyAll();
		}
	}
	
	private static void notifyTouchUp(IActivity activity) { 
		if (activity == null) return;
		synchronized (sLock) { 
			sMotionTouching --;
			if (sMotionTouching < 0) sMotionTouching = 0;
			
			if (LOG.isDebugEnabled())
				LOG.debug("notifyMotionUp: count=" + sMotionTouching);
			
			sLock.notifyAll();
			
			if (sMotionTouching == 0)
				dispatchUp(activity);
		}
	}
	
	public static void onMotionDown(IActivity activity, MotionEvent event) { 
		if (activity == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("onMotionDown: activity=" + activity + " event=" + event);
		
		notifyTouchDown(activity);
	}
	
	public static void onMotionUp(IActivity activity, MotionEvent event) { 
		if (activity == null) return;
		if (LOG.isDebugEnabled())
			LOG.debug("notifyMotionMove: activity=" + activity + " event=" + event);
		
		notifyTouchUp(activity);
	}
	
	public static void onMotionMove(IActivity activity, MotionEvent event) { 
		if (activity == null) return;
		//if (LOG.isDebugEnabled())
		//	LOG.debug("onMotionMove: activity=" + activity + " event=" + event);
		
		//dispatchMove(activity, event);
	}
	
	public static void onScrollStateChanged(IActivity activity, 
			AbsListView view, int scrollState) { 
		if (activity == null || view == null) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("notifyScrollIdle: activity=" + activity 
					+ " view=" + view + " scrollState=" + scrollState);
		}
		
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
			notifyTouchDown(activity);
		} else if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) { 
			notifyTouchUp(activity);
		}
	}
	
	private static final List<WeakReference<TouchListener>> sListeners = 
			new ArrayList<WeakReference<TouchListener>>(); 
	
	public static void addListener(Object listener) { 
		if (listener != null && listener instanceof TouchListener)
			addListener((TouchListener)listener);
	}
	
	public static void addListener(TouchListener listener) { 
    	synchronized (sListeners) { 
    		boolean found = false; 
    		for (int i=0; i < sListeners.size(); ) { 
    			WeakReference<TouchListener> ref = sListeners.get(i); 
    			TouchListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				sListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		if (!found && listener != null) { 
    			sListeners.add(new WeakReference<TouchListener>(listener)); 
    			
    			if (LOG.isDebugEnabled())
    				LOG.debug("addListener: " + listener + " count=" + sListeners.size());
    		}
    	}
    }
    
	public static void removeListener(TouchListener listener) { 
    	synchronized (sListeners) { 
    		if (LOG.isDebugEnabled())
				LOG.debug("removeListener: " + listener + " count=" + sListeners.size());
    		
    		for (int i=0; i < sListeners.size(); ) { 
    			WeakReference<TouchListener> ref = sListeners.get(i); 
    			TouchListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null || lsr == listener) { 
    				sListeners.remove(i); continue; 
    			}
    			i ++; 
    		}
    	}
    }
	
    private static void dispatchUp(IActivity activity) { 
    	if (activity == null) return;
    	synchronized (sListeners) { 
    		BitmapCache.getInstance().recycleBitmapsIfNecessary();
    		
    		for (int i=0; i < sListeners.size(); ) { 
    			WeakReference<TouchListener> ref = sListeners.get(i); 
    			TouchListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				sListeners.remove(i); continue; 
    			} else { 
    				listener.onTouchUp(activity.getActivity());
    			}
    			i ++; 
    		}
    	}
    }
    
    @SuppressWarnings("unused")
	private static void dispatchMove(IActivity activity, MotionEvent event) { 
    	if (activity == null) return;
    	synchronized (sListeners) { 
    		for (int i=0; i < sListeners.size(); ) { 
    			WeakReference<TouchListener> ref = sListeners.get(i); 
    			TouchListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				sListeners.remove(i); continue; 
    			} else { 
    				//listener.onTouchMove(activity.getActivity(), event);
    			}
    			i ++; 
    		}
    	}
    }
	
}
