package org.javenstudio.cocoka.widget.model;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;

import org.javenstudio.cocoka.android.ResourceContext;
import org.javenstudio.cocoka.android.ResourceHelper;

public abstract class BaseModel extends Model {

	private Controller mController = null; 
	private DeferredHandler mHandler = null;
	
	protected boolean mFailed = false; 
	protected boolean mReload = false; 
	protected volatile boolean mAbort = false; 
	
	public BaseModel(final Application app) { 
		super(app); 
	}
	
	public ResourceContext getResourceContext() { 
		return ResourceHelper.getResourceContext(); 
	}
	
	protected final synchronized Controller getInitializedController() { 
		if (mController == null) 
			throw new RuntimeException("Model not initialized"); 
		return mController; 
	}
	
	@Override 
	protected final synchronized DeferredHandler getDeferredHandler() {
		if (mHandler == null) 
			throw new RuntimeException("Model not initialized"); 
		return mHandler; 
    }
	
	@Override 
	protected synchronized void onInitialized(final Controller controller) {
		if (Looper.myLooper() != Looper.getMainLooper()) 
			throw new RuntimeException("Model can only initialized on main thread"); 
		
		if (mHandler == null) 
			mHandler = new DeferredHandler(Looper.myQueue());
		
		mController = controller; 
		mAbort = false; 
	}
	
	@Override 
	protected void onThreadStart() { 
		super.onThreadStart(); 
		
		mFailed = false; 
	}
	
	@Override 
	public void onActivityStart(Activity activity) { 
		super.onActivityStart(activity); 
		
		startWorker(); 
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
	
	public boolean shouldStartWorker() { 
		return false; 
	}
	
	public final void reload(boolean force) { 
		mAbort = false; 
		mReload = force ? true : false; 
		
		startWorker(); 
	}
	
	public final void stop() { 
		mAbort = true; 
		
		abortWorker(); 
	}
	
	public final void startWorker() { 
		final Controller controller = getInitializedController(); 
		if (controller == null) return; 
		
		if (shouldStartWorker()) 
			startLoader(controller.getContext(), true); 
	}
	
	public final void abortWorker() { 
		//final Controller controller = mController; 
		
		mAbort = true; 
		stopLoader(); 
	}
	
	protected void onExceptionCatched(Throwable e) { 
		mFailed = true; 
		callbackOnException(e); 
		abortWorker(); 
	}
	
}
