package org.javenstudio.cocoka.widget.model;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;

import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.activity.NavigationActivity;

public abstract class NavigationModel extends Model {

	private final NavigationItem[] mItems; 
	
	private NavigationController mController = null; 
	private DeferredHandler mHandler = null;
	private WeakReference<NavigationActivity> mActivityRef = null; 
	
	protected boolean mFailed = false; 
	protected boolean mReload = false; 
	protected volatile boolean mAbort = false; 
	
	public NavigationModel(final Application app) { 
		super(app); 
		
		mItems = createNavigationItems(); 
	}
	
	public ResourceContext getResourceContext() { 
		return ResourceHelper.getResourceContext(); 
	}
	
	protected abstract NavigationItem[] createNavigationItems(); 
	protected abstract NavigationItem createDefaultNavigationItem(); 
	
	public final NavigationItem[] getNavigationItems() { 
		return mItems; 
	}
	
	protected final synchronized NavigationController getInitializedController() { 
		if (mController == null) 
			throw new RuntimeException("NavigationModel not initialized"); 
		return mController; 
	}
	
	@Override 
	protected final synchronized DeferredHandler getDeferredHandler() {
		if (mHandler == null) 
			throw new RuntimeException("NavigationModel not initialized"); 
		return mHandler; 
    }
	
	@Override 
	protected synchronized void onInitialized(final Controller controller) {
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
	
	@Override 
	protected void onThreadStart() { 
		super.onThreadStart(); 
		
		mFailed = false; 
	}
	
	@Override 
	public void onActivityStart(Activity activity) { 
		super.onActivityStart(activity); 
		
		startWorker(activity); 
	}
	
	@Override 
	public void onActivityStop(Activity activity) { 
		super.onActivityStop(activity); 
		
		abortWorker(); 
	}
	
	@Override 
	public boolean onActivityBackPressed(Activity activity) { 
		abortWorker(); 
		
		return super.onActivityBackPressed(activity); 
	}
	
	public final void reload(Activity activity, boolean force) { 
		mAbort = false; 
		mReload = force ? true : false; 
		
		startWorker(activity); 
	}
	
	public final void stop() { 
		mAbort = true; 
		
		abortWorker(); 
	}
	
	public synchronized final void startWorker(Activity activity) { 
		final NavigationController controller = getInitializedController(); 
		if (controller == null || activity == null) 
			return; 
		
		if (activity instanceof NavigationActivity) { 
			NavigationController navigationController = (NavigationController)controller; 
			NavigationActivity navigationActivity = (NavigationActivity)activity; 
			
			mActivityRef = new WeakReference<NavigationActivity>(navigationActivity); 
			
			NavigationItem item = navigationController.getSelectedItem(); 
			NavigationItem current = navigationController.getCurrentItem(); 
			
			if (item != null && (mReload || item.shouldReload()) && (current == null || item == current)) { 
				mReload = false; 
				startLoader(controller.getContext(), true); 
			}
		}
	}
	
	public synchronized final void abortWorker() { 
		//final Controller controller = mController; 
		
		mAbort = true; 
		stopLoader(); 
	}
	
	public synchronized NavigationActivity getNavigationActivity() { 
		return mActivityRef != null ? mActivityRef.get() : null; 
	}
	
	@Override 
	protected final synchronized void startWorkOnThread() { 
		final NavigationController controller = getInitializedController(); 
		final NavigationActivity activity = getNavigationActivity(); 
		if (controller == null || activity == null) 
			return; 
		
		startNavigationWorkOnThread(controller, activity); 
	}
	
	protected void onExceptionCached(Throwable e) { 
		mFailed = true; 
		callbackOnException(e); 
		abortWorker(); 
	}
	
	protected abstract void startNavigationWorkOnThread(NavigationController controller, NavigationActivity activity); 
	
}
