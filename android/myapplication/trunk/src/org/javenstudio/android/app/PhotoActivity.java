package org.javenstudio.android.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.IntentHelper;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.view.GLPhotoActivity;
import org.javenstudio.common.util.Logger;

public class PhotoActivity extends GLPhotoActivity 
		implements ReloadCallback, ActivityHelper.HelperApp {
	private static final Logger LOG = Logger.getLogger(PhotoActivity.class);
	
	private final ActivityHelper mActivityHelper = createActivityHelper();
	
	protected boolean mProgressRunning = false;
	protected boolean mStarted = false;
	protected boolean mDestroyed = false;
	
	protected final ActivityHelper createActivityHelper() { 
		return new ActivityHelper(this) {
				protected boolean isStarted() { return mStarted; }
				protected boolean isDestroyed() { return mDestroyed; }
				protected boolean isProgressRunning() { return mProgressRunning; }
				protected boolean isLockOrientationDisabled(int orientation) { 
					return PhotoActivity.this.isLockOrientationDisabled(orientation); 
				}
				protected boolean isUnlockOrientationDisabled() { 
					return PhotoActivity.this.isUnlockOrientationDisabled(); 
				}
			};
	}
	
	protected Object getPhotoObject() { return null; }
	
	protected boolean isLockOrientationDisabled(int orientation) { return false; }
	protected boolean isUnlockOrientationDisabled() { return false; }
	
	public boolean isContentProgressEnabled() { return false; }
	
	@Override
	public final ActivityHelper getActivityHelper() {
		return mActivityHelper;
	}
	
	@Override
	protected boolean isRotationLockEnable() { 
		return !mActivityHelper.isOrientationSensorEnable();
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityHelper.onActivityCreate();
	}
	
    @Override
    protected void onStart() {
        super.onStart();
        mStarted = true;
        mActivityHelper.onActivityStart();
    }
	
    @Override
    protected void onResume() {
        super.onResume();
        mActivityHelper.onActivityResume();
    }
    
    @Override
    protected void onStop() {
    	mStarted = false;
    	mActivityHelper.onActivityStop();
        super.onStop();
    }
    
    @Override
    public void finish() {
    	super.finish();
		overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_right_exit); 
	}
    
	@Override
	protected void onDestroy() {
		mDestroyed = true;
		mActivityHelper.onActivityDestroy();
	    super.onDestroy();
	}
    
	@Override    
	public boolean dispatchTouchEvent(MotionEvent event) {
		//mActivityHelper.onActivityDispatchTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}
	
    //@Override
	//public void onExceptionCatched(Throwable e) { 
    //	mActivityHelper.onExceptionCatched(e);
	//	postHideProgress(true);
	//}
    
    @Override
    public void showContentMessage(CharSequence message) { 
    	if (mProgressRunning) {
    		mActivityHelper.postShowContentMessage(message, 0, null);
    	}
    }
    
    @Override
    public void onActionError(ActionError error) {
    	if (error == null) return;
    	mActivityHelper.onActionError(error);
    	postHideProgress(true);
    }
    
    @Override
	public void showProgressDialog(CharSequence message) {}
    
    @Override
	public void hideProgressDialog() {}
    
    @Override
	public boolean isActionProcessing() { return false; }
    
    @Override
	public final void postShowProgress(final boolean force) { 
		if (mActivityHelper.incrementProgress(force) == false)
			return;
		
		mProgressRunning = true;
		if (mDestroyed) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				public void run() { 
					if (LOG.isDebugEnabled()) LOG.debug("postShowProgress: force=" + force);
					showProgressView(); 
				}
			});
	}
	
    @Override
	public final void postHideProgress(final boolean force) { 
		if (mActivityHelper.decrementProgress(force) == false) 
			return;
		
		mProgressRunning = false;
		if (mDestroyed) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				public void run() { 
					if (LOG.isDebugEnabled()) LOG.debug("postHideProgress: force=" + force);
					hideProgressView(); 
				}
			});
	}
	
    private void showProgressView() { 
		IMenuItem item = getGLActionBar().getProgressMenuItem();
		mActivityHelper.showProgressActionView(item);
	}
	
	private void hideProgressView() { 
		IMenuItem item = getGLActionBar().getProgressMenuItem();
		mActivityHelper.hideProgressActionView(item);
	}
    
	public CharSequence getLastUpdatedLabel() { return null; }
	
    public String getParam(String name) { return null; }
	public void setParam(String name, String value) {}
	public void clearParams() {}
    
	@Override
    protected void initializeByIntent() {
		Object data = getPhotoObject();
		if (data != null) initializePhotoPage();
	}
	
	protected void initializePhotoPage() {
		Bundle data = new Bundle();
		//data.putInt(PhotoPage.KEY_INDEX_HINT, activity.getPhotoIndexHint());
		getGLStateManager().startState(PhotoHelper.PhotoPageImpl.class, data);
	}
	
	@Override
	public void startChooser(Intent intent, CharSequence title) {
		if (intent == null) return;
		try {
			//startActivity(Intent.createChooser(intent, title));
			IntentHelper.showChooserDialog(this, intent, title);
		} catch (Throwable e) {
			getActivityHelper().onActionError(
					new ActionError(ActionError.Action.START_ACTIVITY, e));
		}
	}
	
}
