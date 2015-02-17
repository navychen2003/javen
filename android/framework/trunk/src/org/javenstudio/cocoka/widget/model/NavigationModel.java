package org.javenstudio.cocoka.widget.model;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;

public abstract class NavigationModel extends Model {

	private final NavigationItem[] mItems; 
	
	private NavigationController mController = null; 
	private DeferredHandler mHandler = null;
	private WeakReference<NavigationCallback> mCallbackRef = null; 
	private final Object mLock = new Object();
	
	protected boolean mFailed = false; 
	protected boolean mForceReload = false; 
	protected volatile boolean mAbort = false; 
	
	public NavigationModel(final Application app) { 
		super(app); 
		
		mItems = createNavigationItems(); 
	}
	
	protected abstract NavigationItem[] createNavigationItems(); 
	protected abstract NavigationItem createDefaultNavigationItem(); 
	
	public final NavigationItem[] getNavigationItems() { 
		return mItems; 
	}
	
	protected final NavigationController getInitializedController() { 
		synchronized (mLock) {
			if (mController == null) 
				throw new RuntimeException("NavigationModel not initialized"); 
			return mController; 
		}
	}
	
	@Override 
	protected final DeferredHandler getDeferredHandler() {
		synchronized (mLock) {
			if (mHandler == null) 
				throw new RuntimeException("NavigationModel not initialized"); 
			return mHandler; 
		}
    }
	
	@Override 
	protected void onInitialized(final Controller controller) {
		synchronized (mLock) {
			mController = null; 
			mAbort = false; 
			
			if (controller != null && controller instanceof NavigationController) { 
				if (Looper.myLooper() != Looper.getMainLooper()) 
					throw new RuntimeException("NavigationModel can only initialized on main thread"); 
				
				if (mHandler == null) 
					mHandler = new DeferredHandler(Looper.myQueue());
				
				mController = (NavigationController)controller; 
			}
		}
	}
	
	@Override 
	protected void onThreadStart() { 
		super.onThreadStart(); 
		
		mFailed = false; 
	}
	
	@Override 
	public void onActivityStart(Activity activity) { 
		super.onActivityStart(activity); 
		
		//if (activity instanceof NavigationCallback)
		//	startWorker((NavigationCallback)activity); 
	}
	
	@Override 
	public void onActivityStop(Activity activity) { 
		abortWorker(); 
		super.onActivityStop(activity); 
	}
	
	@Override 
	public boolean onActivityBackPressed(Activity activity) { 
		abortWorker(); 
		return super.onActivityBackPressed(activity); 
	}
	
	public final void startLoad(NavigationCallback activity, boolean force) { 
		mAbort = false; 
		mForceReload = force; 
		
		startWorker(activity); 
	}
	
	public final void stopLoad() { 
		mAbort = true; 
		abortWorker(); 
	}
	
	public boolean isForceReload() { return mForceReload; }
	
	protected boolean shouldStartLoad(NavigationController controller, NavigationCallback callback) { 
		NavigationItem item = controller.getSelectedItem(); 
		NavigationItem current = controller.getCurrentItem(); 
		
		if (item != null && (mForceReload || item.shouldReload()) && 
			(current == null || item == current)) { 
			return true;
		}
		
		return false; 
	}
	
	private final void startWorker(NavigationCallback activity) { 
		final NavigationController controller = getInitializedController(); 
		if (controller == null || activity == null) 
			return; 
		
		if (activity instanceof NavigationCallback) { 
			NavigationController navController = (NavigationController)controller; 
			NavigationCallback navActivity = (NavigationCallback)activity; 
			
			synchronized (mLock) {
				mCallbackRef = new WeakReference<NavigationCallback>(navActivity); 
				
				if (shouldStartLoad(navController, navActivity)) 
					startLoader(true); 
			}
		}
	}
	
	private final void abortWorker() { 
		synchronized (mLock) {
			mAbort = true; 
			stopLoader(); 
		}
	}
	
	protected final NavigationCallback getModelCallback() { 
		synchronized (mLock) {
			return mCallbackRef != null ? mCallbackRef.get() : null; 
		}
	}
	
	@Override 
	protected final void startWorkOnThread() { 
		final NavigationController controller = getInitializedController(); 
		final NavigationCallback activity = getModelCallback(); 
		if (controller == null || activity == null) 
			return; 
		
		runWorkOnThread(controller, activity); 
		mForceReload = false; 
	}
	
	public final void onExceptionCatched(Throwable e) { 
		mFailed = true; 
		callbackOnException(e); 
		abortWorker(); 
	}
	
	protected abstract void runWorkOnThread(NavigationController controller, 
			NavigationCallback callback); 
	
}
