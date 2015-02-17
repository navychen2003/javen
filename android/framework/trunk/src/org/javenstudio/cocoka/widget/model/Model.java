package org.javenstudio.cocoka.widget.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Process;

import org.javenstudio.common.util.Logger;

public abstract class Model extends BroadcastReceiver {
	private static final Logger LOG = Logger.getLogger(Model.class);

	public static final int ACTION_ONLOADERSTART = 1; 
	public static final int ACTION_ONLOADERSTOP = 2; 
	public static final int ACTION_ONDATASETUPDATE = 3; 
	public static final int ACTION_ONDATASETCLEAR = 4; 
	public static final int ACTION_ONEXCEPTION = 5; 
	public static final int ACTION_ONDATASETCHANGED = 6; 
	
	private static final AtomicInteger sCounter = new AtomicInteger(0);
	
	public static class Callback {
		private Model mModel = null; 
		private final void initByModel(Model model) { 
			mModel = model;
			mModel.registerCallback(this);
			onInited(model);
		}
		
		public final Model getModel() { 
			if (mModel == null) 
				throw new NullPointerException("Model is null, not initialized");
			return mModel;
		}
		
		protected void invokeByModel(int action, Object params) { 
			if (LOG.isDebugEnabled())
				LOG.debug("invokeByModel: action=" + action + " params=" + params);
		}
		
		protected void onInited(Model model) { 
			if (LOG.isDebugEnabled())
				LOG.debug("onInited: model=" + model);
		}
		
		public void onCreate(Activity activity) { 
			if (LOG.isDebugEnabled())
				LOG.debug("onCreate: activity=" + activity);
			
			getModel().onActivityCreate(activity); 
		}
		
		public void onStart(Activity activity) { 
			if (LOG.isDebugEnabled())
				LOG.debug("onStart: activity=" + activity);
			
			getModel().onActivityStart(activity); 
		}
		
		public void onResume(Activity activity) { 
			if (LOG.isDebugEnabled())
				LOG.debug("onResume: activity=" + activity);
			
			getModel().onActivityResume(activity); 
		}
		
		public void onStop(Activity activity) { 
			if (LOG.isDebugEnabled())
				LOG.debug("onStop: activity=" + activity);
			
			getModel().onActivityStop(activity); 
		}
		
		public void onDestroy(Activity activity) { 
			if (LOG.isDebugEnabled())
				LOG.debug("onDestroy: activity=" + activity);
			
			getModel().onActivityDestroy(activity); 
		}
		
		public boolean onBackPressed(Activity activity) { 
			if (LOG.isDebugEnabled())
				LOG.debug("onBackPressed: activity=" + activity);
			
			return getModel().onActivityBackPressed(activity); 
		}
	}
	
	public static interface BroadcastHandler {
		public void handleIntent(Intent intent); 
	}
	
	private final Object mLock = new Object();
	private final Loader mLoader = new Loader();
	
	private final Application mApp;
	private final Map<String, Object> mValues; 
	private final Map<String, BroadcastHandlers> mBroadcastHandlers;
	private final Callbacks mCallbacks; 
	
	private boolean mBeforeFirstLoad = true; // only access this from main thread
	
	private static class Callbacks { 
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
			if (callback == null) return; 
			
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
		// do nothing
	}
	
	protected void onActivityCreate(Activity activity) {}
	protected void onActivityStart(Activity activity) {}
	protected void onActivityResume(Activity activity) {}
	protected void onActivityStop(Activity activity) {}
	protected void onActivityDestroy(Activity activity) {}
	
	protected boolean onActivityBackPressed(Activity activity) { return false; }
	
	public final void registerCallback(Callback callback) {
		if (callback == null) return; 
		
		synchronized (mLock) {
			mCallbacks.add(callback); 
		}
	}
	
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
	
	public final void initialize(Controller controller, Callback callback) { 
		synchronized (this) { 
			if (callback != null) 
				callback.initByModel(this); 
			
			onInitialized(controller); 
		}
	}
	
	protected void onInitialized(Controller controller) {} 
	
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
    
    protected final void startLoader(boolean isLaunching) {
        mLoader.startLoader(isLaunching);
    }

    protected final void stopLoader() {
        mLoader.stopLoader();
    }
    
    protected final boolean isBeforeFirstLoad() {
    	return mBeforeFirstLoad; 
    }
	
    protected final boolean isLoaderRunning() { 
    	return mLoader.isRunning(); 
    }
    
    protected boolean isReadyToStartLoader() { 
    	//synchronized (mLock) {
    	//	return mBroadcastHandlers.size() > 0 || mCallbacks.size() > 0; 
    	//}
    	return true; 
    }
    
    public void loadNextPage(Callback callback, int totalCount, int lastVisibleItem) { 
    }
    
    public final boolean isLoading() { 
    	return isLoaderRunning();
    }
    
    protected String newLoaderThreadName() { 
    	return "WorkThread-" + sCounter.incrementAndGet() + "[" + getClass().getSimpleName() + "]";
    }
    
	private class Loader {
    	private LoaderThread mLoaderThread = null;
    	
    	/**
         * Call this from the ui thread so the handler is initialized on the correct thread.
         */
        public Loader() {}
        
        public void startLoader(boolean isLaunching) {
            synchronized (mLock) {
                // Don't bother to start the thread if we know it's not going to do anything
                if (isReadyToStartLoader()) {
                    LoaderThread oldThread = mLoaderThread;
                    if (oldThread != null) 
                        oldThread.stopLocked();
                    
                    notifyThreadStart(); 
                    mLoaderThread = new LoaderThread(oldThread, isLaunching);
                    mLoaderThread.start();
                }
            }
        }
        
		public void stopLoader() {
            //synchronized (mLock) {
				LoaderThread thread = mLoaderThread;
			
				if (LOG.isDebugEnabled())
					LOG.debug("stopLoader: thread=" + thread);
				
                if (thread != null) 
                	thread.stopLocked();
            //}
        }
    	
		public boolean isRunning() {
            //synchronized (mLock) {
            	LoaderThread thread = mLoaderThread;
                if (thread != null) 
                    return thread.isRunning();
                return false; 
            //}
        }
		
        @SuppressWarnings("unused")
		public boolean isRequestStopped() {
            //synchronized (mLock) {
            	LoaderThread thread = mLoaderThread;
                if (thread != null) 
                    return thread.isRequestStopped();
                return false; 
            //}
        }
        
        @SuppressWarnings({"unused"})
        private class LoaderThread extends Thread {
        	
	        private Thread mWaitThread;
	        private boolean mIsLaunching = false;
	        private boolean mIsRunning = false;
            private boolean mRequestStopped = false;
	        
            LoaderThread(Thread waitThread, boolean isLaunching) {
            	super(newLoaderThreadName());
                this.mWaitThread = waitThread;
                this.mIsLaunching = isLaunching;
            }

            boolean isLaunching() { return mIsLaunching; }
	        boolean isRunning() { return mIsRunning; }
            boolean isRequestStopped() { return mRequestStopped; }
            
            @Override
	        public void run() {
	            waitForOtherThread(); 
	            notifyLoaderStart(); 
	            
	            long startTime = System.currentTimeMillis();
	            try { 
	            	if (LOG.isDebugEnabled())
	        			LOG.debug(getName() + " starting ...");
	            	
	            	mIsRunning = true;
	            	doRun(); 
	            } finally { 
	            	mIsRunning = false;
	            	
	        		long elapsed = System.currentTimeMillis() - startTime; 
	        		if (LOG.isInfoEnabled())
	        			LOG.info(getName() + " finished in " + elapsed + " ms");
	        		
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
                //mContext = null;
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
                    mRequestStopped = true;
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
		callbackOnLoaderStart(null); 
	}
	
	protected void notifyLoaderStop() { 
		onLoaderStop(); 
		callbackOnLoaderStop(null); 
	}
	
	public final void callbackInvoke(int action, Object param) { 
		mCallbacks.invokeByModel(action, param); 
		onCallbackInvoke(action, param);
	}
	
	public void callbackOnLoaderStart(Object data) { 
		callbackInvoke(Model.ACTION_ONLOADERSTART, data); 
	}
	
	public void callbackOnLoaderStop(Object data) { 
		callbackInvoke(Model.ACTION_ONLOADERSTOP, data); 
	}
	
	public void callbackOnException(Throwable e) { 
		callbackInvoke(Model.ACTION_ONEXCEPTION, e); 
	}
	
	public void callbackOnDataSetUpdate(Object data) { 
		callbackInvoke(Model.ACTION_ONDATASETUPDATE, data); 
	}
	
	public void callbackOnDataSetClear(Object data) { 
		callbackInvoke(Model.ACTION_ONDATASETCLEAR, data); 
	}
	
	public void callbackOnDataSetChanged(Object data) { 
		callbackInvoke(Model.ACTION_ONDATASETCHANGED, data); 
	}
	
	protected void onThreadStart() {} 
	protected void onLoaderStart() {} 
	protected void onLoaderStop() {} 
	
	protected void onCallbackInvoke(int action, Object param) {}
	
	protected void startWorkOnThread() {} 
	protected void startHardWorkOnThread() {} 
	
	public void removeValue(String key) {
		if (key == null) return; 
		
		synchronized (mValues) {
			mValues.remove(key); 
		}
	}
	
	public void putValue(String key, Object value) {
		if (key == null || value == null) 
			return; 
		
		synchronized (mValues) {
			mValues.put(key, value); 
		}
	}
	
	public Object getValue(String key) {
		if (key == null) return null; 
		
		synchronized (mValues) {
			return mValues.get(key); 
		}
	}
	
}
