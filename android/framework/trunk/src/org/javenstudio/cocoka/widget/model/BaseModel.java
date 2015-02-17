package org.javenstudio.cocoka.widget.model;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;

public abstract class BaseModel extends Model {

	private Controller mController = null; 
	private DeferredHandler mHandler = null;
	private WeakReference<Model.Callback> mCallbackRef = null; 
	private final Object mLock = new Object();
	
	protected boolean mFailed = false; 
	protected boolean mForceReload = false; 
	protected volatile boolean mAbort = false; 
	
	public BaseModel(final Application app) { 
		super(app); 
	}
	
	protected final Controller getInitializedController() { 
		synchronized (mLock) {
			if (mController == null) 
				throw new RuntimeException("Model not initialized"); 
		
			return mController; 
		}
	}
	
	@Override 
	protected final DeferredHandler getDeferredHandler() {
		synchronized (mLock) {
			if (mHandler == null) 
				throw new RuntimeException("Model not initialized"); 
			
			return mHandler; 
		}
    }
	
	@Override 
	protected void onInitialized(final Controller controller) {
		synchronized (mLock) {
			if (Looper.myLooper() != Looper.getMainLooper()) 
				throw new RuntimeException("Model can only initialized on main thread"); 
			
			if (mHandler == null) 
				mHandler = new DeferredHandler(Looper.myQueue());
			
			mController = controller; 
			mAbort = false; 
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
		
		//startWorker(activity); 
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
	
	public final void startLoad(Model.Callback activity, boolean force) { 
		mAbort = false; 
		mForceReload = force; 
		
		startWorker(activity); 
	}
	
	public final void stopLoad() { 
		mAbort = true; 
		abortWorker(); 
	}
	
	public boolean isForceReload() { return mForceReload; }
	
	protected boolean shouldStartLoad(Controller controller, Model.Callback callback) { 
		return false; 
	}
	
	private final void startWorker(Model.Callback callback) { 
		final Controller controller = getInitializedController(); 
		if (controller == null || callback == null) 
			return; 
		
		synchronized (mLock) {
			mCallbackRef = new WeakReference<Model.Callback>(callback); 
			
			if (shouldStartLoad(controller, callback)) 
				startLoader(true); 
		}
	}
	
	private final void abortWorker() { 
		synchronized (mLock) {
			mAbort = true; 
			stopLoader(); 
		}
	}
	
	protected final Model.Callback getModelCallback() { 
		synchronized (mLock) {
			return mCallbackRef != null ? mCallbackRef.get() : null; 
		}
	}
	
	@Override 
	protected final void startWorkOnThread() { 
		final Controller controller = getInitializedController(); 
		final Model.Callback activity = getModelCallback(); 
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
	
	protected abstract void runWorkOnThread(Controller controller, 
			Model.Callback callback); 
	
}
