package org.javenstudio.android.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.NetworkHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.SimpleFragmentActivity;
import org.javenstudio.cocoka.app.SupportActionBar;
import org.javenstudio.cocoka.widget.model.Controller;
import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.common.util.Logger;

public abstract class SimpleActivity extends SimpleFragmentActivity 
		implements IActivity {
	private static final Logger LOG = Logger.getLogger(SimpleActivity.class);

	private final ModelCallback mCallback = createCallback();
	
	protected ModelCallback createCallback() { 
		return new ModelCallback() { 
				@Override
				protected void invokeByModel(int action, Object params) { 
					onInvokeByModel(action, params);
				}
			};
	}
	
	private final ActivityHelper mActivityHelper = createActivityHelper();
	private ActionHelper mActionHelper = null;
	
	protected final ActivityHelper createActivityHelper() { 
		return new ActivityHelper(this) { 
				protected boolean isStarted() { return mStarted; }
				protected boolean isDestroyed() { return mDestroyed; }
				protected boolean isProgressRunning() { return mProgressRunning; }
				protected boolean isLockOrientationDisabled(int orientation) { 
					return SimpleActivity.this.isLockOrientationDisabled(orientation); 
				}
				protected boolean isUnlockOrientationDisabled() { 
					return SimpleActivity.this.isUnlockOrientationDisabled(); 
				}
			};
	}
	
	protected boolean mProgressRunning = false;
	protected boolean mStarted = false;
	protected boolean mDestroyed = false;
	
	public abstract Controller getController();
	
	protected boolean isLockOrientationDisabled(int orientation) { return false; }
	protected boolean isUnlockOrientationDisabled() { return false; }
	
	public boolean isContentProgressEnabled() { return false; }
	
	@Override
	public final Activity getActivity() { 
		return this;
	}
	
	@Override
	public final ActivityHelper getActivityHelper() { 
		return mActivityHelper;
	}
	
	@Override
	public final ActionHelper getActionHelper() { 
		return mActionHelper;
	}
	
	@Override
	public final SupportActionBar getSupportActionBarOrNull() { 
		return mActivityHelper.getSupportActionBarOrNull();
	}
	
	@Override
	public final IActionBar getSupportActionBar() { 
		return mActivityHelper.getActionBarAdapter();
	}
	
	@Override
	public final ModelCallback getCallback() { 
		return mCallback;
	}
	
	@Override
	public IMenu getActivityMenu() { 
		return null;
	}
	
	@Override
	public IMenuOperation getMenuOperation() { 
		return null;
	}
	
	protected void onInvokeByModel(int action, Object params) { 
		switch (action) { 
		case Model.ACTION_ONDATASETCLEAR:
			//onDataSetClear(params);
			break;
		case Model.ACTION_ONDATASETUPDATE: 
			//onDataSetUpdated(params); 
			break; 
		case Model.ACTION_ONDATASETCHANGED:
			//onDataSetChanged(params);
			break;
		case Model.ACTION_ONEXCEPTION: 
			onExceptionCatched(params); 
			break; 
		}
	}
	
	protected ActionHelper createActionHelper() { 
		return new ActionHelper(this, getSupportActionBar());
	}
	
	@Override
	protected void doOnCreateDone(Bundle savedInstanceState) { 
		super.doOnCreateDone(savedInstanceState);
		mActionHelper = createActionHelper();
		mCallback.onCreate(this); 
		mActivityHelper.onActivityCreate();
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mActivityHelper.onActivityConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onStart() {
	    super.onStart();
	    mCallback.onStart(this); 
	    mActivityHelper.onActivityStart();
	    mStarted = true;
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    mCallback.onResume(this); 
	    mActivityHelper.onActivityResume();
	}
	
	@Override
	public void onBackPressed() {
	    if (mCallback.onBackPressed(this)) 
	    	return; 
	    
		super.onBackPressed(); 
	}
	
	@Override
	protected void onStop() {
		mStarted = false;
	    mCallback.onStop(this); 
	    mActivityHelper.onActivityStop();
	    super.onStop();
	}
	
	@Override
	public void finish() {
		mDestroyed = true;
		super.finish();
		overridePendingTransition();
	}
	
	@Override
	protected void onDestroy() {
		mDestroyed = true;
		mCallback.onDestroy(this); 
		mActivityHelper.onActivityDestroy();
	    super.onDestroy();
	}
	
	@Override    
	public boolean dispatchTouchEvent(MotionEvent event) {
		mActivityHelper.onActivityDispatchTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}
	
	protected void overridePendingTransition() { 
		overridePendingTransition(R.anim.activity_left_enter, R.anim.activity_right_exit); 
	}
	
	//protected void onDataSetClear(Object params) {}
	//protected void onDataSetUpdated(Object params) {}
	//protected void onDataSetChanged(Object params) {}
	
	protected void onExceptionCatched(Object params) { 
		//mActivityHelper.onExceptionCatched(params);
		//postHideProgress(true);
		if (params != null) {
			ActionError error = null;
			if (params instanceof ActionError) 
				error = (ActionError)params;
			else if (params instanceof Throwable)
				error = new ActionError(ActionError.Action.EXCEPTION, (Throwable)params);
			onActionError(error);
		}
	}
	
	protected void onActionError(ActionError error) {
		if (error == null) return;
		mActivityHelper.onActionError(error);
    	postHideProgress(true);
	}
	
	protected void postShowContentMessage(CharSequence message) { 
    	if (mProgressRunning) {
    		mActivityHelper.postShowContentMessage(message, 0, null);
    	}
    }
	
	protected void postShowProgressDialog(CharSequence message) { 
    	if (mProgressRunning) { 
    		ActionHelper helper = getActionHelper();
    		if (helper != null)
    			helper.postShowProgressDialog(message);
    	}
    }
	
	protected void postHideProgressDialog() { 
		ActionHelper helper = getActionHelper();
		if (helper != null)
			helper.postHideProgressDialog();
	}
	
	@Override
	public final void postShowProgress(final boolean force) { 
		if (LOG.isDebugEnabled()) LOG.debug("postShowProgress: force=" + force);
		if (mActivityHelper.incrementProgress(force) == false)
			return;
		
		mProgressRunning = true;
		if (mDestroyed) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				public void run() { 
					if (LOG.isDebugEnabled()) LOG.debug("showProgressView");
					showProgressView(); 
				}
			});
	}
	
	@Override
	public final void postHideProgress(final boolean force) { 
		if (LOG.isDebugEnabled()) LOG.debug("postHideProgress: force=" + force);
		if (mActivityHelper.decrementProgress(force) == false) 
			return;
		
		mProgressRunning = false;
		if (mDestroyed) return;
		
		ResourceHelper.getHandler().post(new Runnable() {
				public void run() { 
					if (LOG.isDebugEnabled()) LOG.debug("hideProgressView");
					hideProgressView(); 
				}
			});
	}
	
	public boolean isProgressRunning() { return mProgressRunning; }
	public void showProgressView() { mActivityHelper.onShowProgressView(); }
	public void hideProgressView() { mActivityHelper.onHideProgressView(); }
	
	public Intent createShareIntent() { return null; }
	public void setShareIntent(Intent shareIntent) {}
	public void setShareIntent() {}
	
	@Override
	public CharSequence getLastUpdatedLabel() { 
		return null; 
	}
	
	@Override
	public boolean onActionHome() { 
		getActivityHelper().onActionHome();
		return super.onActionHome();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		getActivityHelper().onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onNetworkAvailableChanged(NetworkHelper helper) {
		//getActivityHelper().onNetworkAvailable(helper.isNetworkAvailable());
	}
	
}
