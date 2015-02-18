package org.javenstudio.cocoka.widget.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Process;

public abstract class Model extends BroadcastReceiver implements ActivityListener, ValueSavable {

	public static final int ACTION_ONLOADERSTART = 1; 
	public static final int ACTION_ONLOADERSTOP = 2; 
	public static final int ACTION_ONDATASETUPDATE = 3; 
	public static final int ACTION_ONDATASETCLEAR = 4; 
	public static final int ACTION_ONEXCEPTION = 5; 
	
	private final Object mLock = new Object();
	private final Loader mLoader = new Loader();
	
	private final Application mApp;
	private boolean mBeforeFirstLoad = true; // only access this from main thread
	private final Map<String, Object> mValues; 
	private final Map<String, BroadcastHandlers> mBroadcastHandlers;
	private final Callbacks mCallbacks; 
	
	public interface Callback {
		public void invokeByModel(int action, Object params); 
	}
	
	public interface BroadcastHandler {
		public void handleIntent(Intent intent); 
	}
	
	private class Callbacks implements Callback { 
		private final List<WeakReference<Callback>> mList; 
		
		public Callbacks() { 
			mList = new ArrayList<WeakReference<Callback>>(); 
		}
		
		@SuppressWarnings({"unused"})
		public int size() { 
			synchronized (this) { 
				return mList.size(); 
			}
		}
		
		public void add(Callback callback) { 
			if (callback == this) return; 
			
			synchronized (this) { 
				boolean found = false; 
				for (int i=0; i < mList.size(); ) { 
					WeakReference<Callback> ref = mList.get(i); 
					Callback mc = ref != null ? ref.get() : null; 
					if (mc == null) { 
						mList.remove(i); continue; 
					} else if (mc == callback) 
						found = true; 
					i ++; 
				}
				if (!found) 
					mList.add(new WeakReference<Callback>(callback)); 
			}
		}
		
		@Override 
		public void invokeByModel(int action, Object params) { 
			synchronized (this) { 
				for (int i=0; i < mList.size(); ) { 
					WeakReference<Callback> ref = mList.get(i); 
					Callback mc = ref != null ? ref.get() : null; 
					if (mc == null) { 
						mList.remove(i); continue; 
					} else
						mc.invokeByModel(action, params); 
					i ++; 
				}
			}
		}
	}
	
	private class BroadcastHandlers implements BroadcastHandler { 
		private final List<WeakReference<BroadcastHandler>> mList; 
		
		public BroadcastHandlers() { 
			mList = new ArrayList<WeakReference<BroadcastHandler>>(); 
		}
		
		public void handleIntent(Intent intent) { 
			synchronized (this) { 
				for (int i=0; i < mList.size(); ) { 
					WeakReference<BroadcastHandler> ref = mList.get(i); 
					BroadcastHandler cb = ref != null ? ref.get() : null; 
					if (cb == null) { 
						mList.remove(i); continue; 
					}
					cb.handleIntent(intent); 
					i ++; 
				}
			}
		}
		
		public int size() { 
			synchronized (this) { 
				for (int i=0; i < mList.size(); ) { 
					WeakReference<BroadcastHandler> ref = mList.get(i); 
					BroadcastHandler cb = ref != null ? ref.get() : null; 
					if (cb == null) { 
						mList.remove(i); continue; 
					}
					i ++; 
				}
				return mList.size(); 
			}
		}
		
		public void add(BroadcastHandler cb) { 
			if (cb == this) return; 
			
			synchronized (this) { 
				if (cb != null) 
					mList.add(new WeakReference<BroadcastHandler>(cb)); 
			}
		}
		
	}
	
	public Model(final Application app) { 
		mApp = app; 
		mValues = new HashMap<String, Object>(); 
		mBroadcastHandlers = new HashMap<String, BroadcastHandlers>(); 
		mCallbacks = new Callbacks(); 
	}
	
	public final Application getApplication() { 
		return mApp; 
	}
	
	protected final void registerReceiver(final IntentFilter filter) {
		if (filter != null) 
			mApp.registerReceiver(this, filter); 
	}
	
	protected final void unregisterReceiver() { 
		try { 
			mApp.unregisterReceiver(this); 
		} catch (Exception e) { 
			// ignore
		}
	}
	
	@Override 
	public final void onReceive(Context context, final Intent intent) { 
		// Use the app as the context.
        context = getApplication();

        if (isBeforeFirstLoad()) {
            // If we haven't even loaded yet, don't bother, since we'll just pick
            // up the changes.
            return;
        }
        
        synchronized (this) { 
        	final String action = intent.getAction();
        	onReceiveBroadcast(intent); 
        	
        	final BroadcastHandler callback = lookupBroadcastHandler(action); 
        	if (callback == null) {
                //Log.w(TAG, "Nobody to tell about the new update.");
                return;
            }
        	
        	getDeferredHandler().post(new Runnable() {
                public void run() {
                    callback.handleIntent(intent); 
                }
            });
        }
	}
	
	protected void onReceiveBroadcast(Intent intent) { 
		
	}
	
	public final void initialize(final Controller controller, final ActivityInitializer initializer) { 
		synchronized (this) { 
			if (initializer != null) 
				initializer.initializeActivityListener(this); 
			
			onInitialized(controller); 
		}
	}
	
	protected void onInitialized(final Controller controller) {} 
	
	@Override 
	public void onActivityCreate(Activity activity) { 
		// do nothing
	}
	
	@Override 
	public void onActivityStart(Activity activity) { 
		// do nothing
	}
	
	@Override 
	public void onActivityResume(Activity activity) { 
		// do nothing
	}
	
	@Override 
	public boolean onActivityBackPressed(Activity activity) { 
		return false; 
	}
	
	@Override 
	public void onActivityStop(Activity activity) { 
		// do nothing
	}
	
	@Override 
	public void onActivityDestroy(Activity activity) { 
		// do nothing
	}
	
	@Override 
	public final void registerCallback(Callback callback) {
		if (callback == null) return; 
		
		synchronized (mLock) {
			mCallbacks.add(callback); 
		}
	}
	
	@Override 
	public final void registerBroadcast(String action, BroadcastHandler handler) {
		if (action == null || action.length() == 0 || handler == null) 
			return; 
		
        synchronized (mLock) {
    		BroadcastHandlers cbs = mBroadcastHandlers.get(action); 
    		if (cbs == null) { 
    			cbs = new BroadcastHandlers(); 
    			mBroadcastHandlers.put(action, cbs); 
    		}
    		cbs.add(handler); 
        }
    }
    
    protected BroadcastHandler lookupBroadcastHandler(String action) {
    	synchronized (mLock) {
    		if (action != null && action.length() > 0) { 
    			BroadcastHandlers cbs = mBroadcastHandlers.get(action); 
    			if (cbs != null && cbs.size() > 0) 
    				return cbs; 
    		}
    		return null; 
    	}
    }
    
    protected abstract DeferredHandler getDeferredHandler(); 
    
    protected void startLoader(Context context, boolean isLaunching) {
        mLoader.startLoader(context, isLaunching);
    }

    protected void stopLoader() {
        mLoader.stopLoader();
    }
    
    protected boolean isBeforeFirstLoad() {
    	return mBeforeFirstLoad; 
    }
	
    protected boolean isLoaderStopped() { 
    	return mLoader.isStopped(); 
    }
    
    protected boolean isReadyToStartLoader() { 
    	//synchronized (mLock) {
    	//	return mBroadcastHandlers.size() > 0 || mCallbacks.size() > 0; 
    	//}
    	return true; 
    }
    
	private class Loader {
    	private LoaderThread mLoaderThread = null;
    	
    	/**
         * Call this from the ui thread so the handler is initialized on the correct thread.
         */
        public Loader() {
        }
        
        public void startLoader(Context context, boolean isLaunching) {
            synchronized (mLock) {
                // Don't bother to start the thread if we know it's not going to do anything
                if (isReadyToStartLoader()) {
                    LoaderThread oldThread = mLoaderThread;
                    if (oldThread != null) {
                        oldThread.stopLocked();
                    }
                    notifyThreadStart(); 
                    mLoaderThread = new LoaderThread(context, oldThread, isLaunching);
                    mLoaderThread.start();
                }
            }
        }
        
        public void stopLoader() {
            synchronized (mLock) {
                if (mLoaderThread != null) {
                    mLoaderThread.stopLocked();
                }
            }
        }
    	
        public boolean isStopped() {
            synchronized (mLock) {
                if (mLoaderThread != null) {
                    return mLoaderThread.isStopped();
                }
                return false; 
            }
        }
        
        @SuppressWarnings({"unused"})
        private class LoaderThread extends Thread {
	        private Context mContext;
	        private Thread mWaitThread;
	        private boolean mIsLaunching;
            private boolean mStopped;
	        
            LoaderThread(Context context, Thread waitThread, boolean isLaunching) {
                this.mContext = context;
                this.mWaitThread = waitThread;
                this.mIsLaunching = isLaunching;
            }

            boolean isLaunching() {
                return mIsLaunching;
            }
	        
            boolean isStopped() { 
            	return mStopped; 
            }
            
	        public void run() {
	            waitForOtherThread(); 
	            notifyLoaderStart(); 
	            try { 
	            	doRun(); 
	            } finally { 
	            	notifyLoaderStop(); 
	            }
	        }
	        
	        private void doRun() {
	            final boolean isLaunching = mIsLaunching; 
	            
	            // Elevate priority when Home launches for the first time to avoid
                // starving at boot time. Staring at a blank home is not cool.
                synchronized (mLock) {
                    android.os.Process.setThreadPriority(isLaunching
                            ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }
                
                loadHardWork(); 
                
                // Whew! Hard work done.
                synchronized (mLock) {
                    if (isLaunching) {
                        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                    }
                }
                
                loadWork(); 
                
                // Clear out this reference, otherwise we end up holding it until all of the
                // callback runnables are done.
                mContext = null;
                
                mBeforeFirstLoad = false;

                synchronized (mLock) {
                    // Setting the reference is atomic, but we can't do it inside the other critical
                    // sections.
                    mLoaderThread = null;
                }
                
                // Trigger a gc to try to clean up after the stuff is done, since the
                // renderscript allocations aren't charged to the java heap.
                getDeferredHandler().post(new Runnable() {
                        public void run() {
                            System.gc();
                        }
                    });
	        }
	        
	        public void stopLocked() {
                synchronized (LoaderThread.this) {
                    mStopped = true;
                    this.notify();
                }
            }
	        
	        /**
             * If another LoaderThread was supplied, we need to wait for that to finish before
             * we start our processing.  This keeps the ordering of the setting and clearing
             * of the dirty flags correct by making sure we don't start processing stuff until
             * they've had a chance to re-set them.  We do this waiting the worker thread, not
             * the ui thread to avoid ANRs.
             */
            private void waitForOtherThread() {
                if (mWaitThread != null) {
                    boolean done = false;
                    while (!done) {
                        try {
                            mWaitThread.join();
                            done = true;
                        } catch (InterruptedException ex) {
                            // Ignore
                        }
                    }
                    mWaitThread = null;
                }
            }
            
            private void loadWork() {
            	startWorkOnThread(); 
            }
            
            private void loadHardWork() {
            	startHardWorkOnThread(); 
            }
		}
    }
	
	private final void notifyThreadStart() { 
		onThreadStart(); 
		
	}
	
	protected void notifyLoaderStart() { 
		onLoaderStart(); 
		callbackOnLoaderStart(); 
	}
	
	protected void notifyLoaderStop() { 
		onLoaderStop(); 
		callbackOnLoaderStop(); 
	}
	
	protected void callbackInvoke(int action, Object param) { 
		mCallbacks.invokeByModel(action, param); 
	}
	
	protected void callbackOnLoaderStart() { 
		callbackInvoke(Model.ACTION_ONLOADERSTART, null); 
	}
	
	protected void callbackOnLoaderStop() { 
		callbackInvoke(Model.ACTION_ONLOADERSTOP, null); 
	}
	
	protected void callbackOnException(Throwable e) { 
		callbackInvoke(Model.ACTION_ONEXCEPTION, e); 
	}
	
	protected void callbackOnDataSetUpdate(Object data) { 
		callbackInvoke(Model.ACTION_ONDATASETUPDATE, data); 
	}
	
	protected void callbackOnDataSetClear() { 
		callbackInvoke(Model.ACTION_ONDATASETCLEAR, null); 
	}
	
	protected void onThreadStart() {} 
	protected void onLoaderStart() {} 
	protected void onLoaderStop() {} 
	
	protected void startWorkOnThread() {} 
	protected void startHardWorkOnThread() {} 
	
	@Override 
	public void removeValue(String key) {
		if (key == null) return; 
		
		synchronized (mValues) {
			mValues.remove(key); 
		}
	}
	
	@Override 
	public void putValue(String key, Object value) {
		if (key == null || value == null) 
			return; 
		
		synchronized (mValues) {
			mValues.put(key, value); 
		}
	}
	
	@Override 
	public Object getValue(String key) {
		if (key == null) return null; 
		
		synchronized (mValues) {
			return mValues.get(key); 
		}
	}
	
}
