package org.javenstudio.cocoka.opengl;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.Window;
import android.view.WindowManager;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuInflater;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MediaUtils;

public abstract class GLActivityState implements BitmapHolder {
	
    protected static final int FLAG_HIDE_ACTION_BAR = 1;
    protected static final int FLAG_HIDE_STATUS_BAR = 2;
    protected static final int FLAG_SCREEN_ON_WHEN_PLUGGED = 4;
    protected static final int FLAG_SCREEN_ON_ALWAYS = 8;
    protected static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON = 16;
    protected static final int FLAG_SHOW_WHEN_LOCKED = 32;

    private final List<BitmapRef> mBitmaps = new ArrayList<BitmapRef>();
    
    protected GLActivity mActivity;
    protected Bundle mData;
    protected int mFlags;

    protected ResultEntry mReceivedResults;
    protected ResultEntry mResult;

    protected static class ResultEntry {
        public int requestCode;
        public int resultCode = Activity.RESULT_CANCELED;
        public Intent resultData;
    }

    protected boolean mHapticsEnabled;
    private ContentResolver mContentResolver;

    private boolean mDestroyed = false;
    private boolean mPlugged = false;
    boolean mIsFinishing = false;

    private static final String KEY_TRANSITION_IN = "transition-in";

    private StateTransitionAnimation.Transition mNextTransition =
            StateTransitionAnimation.Transition.None;
    private StateTransitionAnimation mIntroAnimation;
    private GLView mContentPane;

    protected GLActivityState() {
    }

    @Override
	public Context getContext() { 
    	return ResourceHelper.getContext(); 
    }
    
    @Override
	public void addBitmap(BitmapRef bitmap) { 
		if (bitmap == null) return;
		
		synchronized (mBitmaps) { 
			mBitmaps.add(bitmap);
		}
	}
    
    public GLActivity getActivity() { 
    	return mActivity;
    }
    
    protected void setContentPane(GLView content) {
        mContentPane = content;
        if (mIntroAnimation != null) {
            mContentPane.setIntroAnimation(mIntroAnimation);
            mIntroAnimation = null;
        }
        mContentPane.setBackgroundColor(getBackgroundColorArray());
        mActivity.getGLRoot().setContentPane(mContentPane);
    }

    void initialize(GLActivity activity, Bundle data) {
        mActivity = activity;
        mData = data;
        mContentResolver = activity.getActivityContext().getContentResolver();
    }

    public Bundle getData() {
        return mData;
    }

    protected void onBackPressed() {
        mActivity.getGLStateManager().finishState(this);
    }

    protected void setStateResult(int resultCode, Intent data) {
        if (mResult == null) return;
        mResult.resultCode = resultCode;
        mResult.resultData = data;
    }

    protected void onConfigurationChanged(Configuration config) {
    }

    protected void onSaveState(Bundle outState) {
    }

    protected void onStateResult(int requestCode, int resultCode, Intent data) {
    }

    protected float[] mBackgroundColor;

    protected int getBackgroundColorId() {
        return 0; //R.color.default_background;
    }

    protected int getBackgroundColor() { 
    	int color = getBackgroundColorId();
    	if (color != 0)
    		return mActivity.getResources().getColor(color);
    	else
    		return Color.BLACK;
    }
    
    protected final float[] getBackgroundColorArray() {
        return mBackgroundColor;
    }

    protected void onCreate(Bundle data, Bundle storedState) {
        mBackgroundColor = MediaUtils.intColorToFloatARGBArray(getBackgroundColor());
    }

    protected void clearStateResult() {
    }

    BroadcastReceiver mPowerIntentReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	            final String action = intent.getAction();
	            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
	                boolean plugged = (0 != intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));
	
	                if (plugged != mPlugged) {
	                    mPlugged = plugged;
	                    setScreenFlags();
	                }
	            }
	        }
	    };

    private void setScreenFlags() {
        final Window win = mActivity.getWindow();
        final WindowManager.LayoutParams params = win.getAttributes();
        
        if ((0 != (mFlags & FLAG_SCREEN_ON_ALWAYS)) ||
                (mPlugged && 0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED))) {
            params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        }
        
        if (0 != (mFlags & FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)) {
            params.flags |= WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
        }
        
        if (0 != (mFlags & FLAG_SHOW_WHEN_LOCKED)) {
            params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        } else {
            params.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        }
        
        win.setAttributes(params);
    }

    protected void transitionOnNextPause(Class<? extends GLActivityState> outgoing,
            Class<? extends GLActivityState> incoming, StateTransitionAnimation.Transition hint) {
        //if (outgoing == PhotoPage.class && incoming == AlbumPage.class) {
        //    mNextTransition = StateTransitionAnimation.Transition.Outgoing;
        //} else if (outgoing == AlbumPage.class && incoming == PhotoPage.class) {
        //    mNextTransition = StateTransitionAnimation.Transition.PhotoIncoming;
        //} else {
            mNextTransition = hint;
        //}
    }

    protected void onPause() {
        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            ((Activity) mActivity).unregisterReceiver(mPowerIntentReceiver);
        }
        if (mNextTransition != StateTransitionAnimation.Transition.None) {
            mActivity.getTransitionStore().put(KEY_TRANSITION_IN, mNextTransition);
            PreparePageFadeoutTexture.prepareFadeOutTexture(mActivity, mContentPane);
            mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    // should only be called by StateManager
    void resume() {
        GLActivity activity = mActivity;
        IActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            if ((mFlags & FLAG_HIDE_ACTION_BAR) != 0) 
                actionBar.hide();
            else 
                actionBar.show();
            
            int stateCount = mActivity.getGLStateManager().getStateCount();
            mActivity.getGLActionBar().setDisplayOptions(stateCount > 1, true);
            // Default behavior, this can be overridden in ActivityState's onResume.
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }

        activity.invalidateOptionsMenu();
        setScreenFlags();

        boolean lightsOut = ((mFlags & FLAG_HIDE_STATUS_BAR) != 0);
        mActivity.getGLRoot().setLightsOutMode(lightsOut);

        ResultEntry entry = mReceivedResults;
        if (entry != null) {
            mReceivedResults = null;
            onStateResult(entry.requestCode, entry.resultCode, entry.resultData);
        }

        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            // we need to know whether the device is plugged in to do this correctly
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            activity.registerReceiver(mPowerIntentReceiver, filter);
        }

        try {
            mHapticsEnabled = Settings.System.getInt(mContentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED) != 0;
        } catch (SettingNotFoundException e) {
            mHapticsEnabled = false;
        }

        onResume();

        // the transition store should be cleared after resume;
        mActivity.getTransitionStore().clear();
    }

    // a subclass of ActivityState should override the method to resume itself
    protected void onResume() {
        RawTexture fade = mActivity.getTransitionStore().get(
                PreparePageFadeoutTexture.KEY_FADE_TEXTURE);
        mNextTransition = mActivity.getTransitionStore().get(
                KEY_TRANSITION_IN, StateTransitionAnimation.Transition.None);
        if (mNextTransition != StateTransitionAnimation.Transition.None) {
            mIntroAnimation = new StateTransitionAnimation(mNextTransition, fade);
            mNextTransition = StateTransitionAnimation.Transition.None;
        }
    }

    protected boolean onCreateActionBar(IMenu menu) {
        // TODO: we should return false if there is no menu to show
        //       this is a workaround for a bug in system
        return true;
    }

    protected boolean onItemSelected(IMenuItem item) {
        return false;
    }

    protected void onDestroy() {
    	synchronized (mBitmaps) { 
    		for (BitmapRef bitmap : mBitmaps) { 
    			bitmap.recycle();
    		}
    	}
        mDestroyed = true;
    }

    boolean isDestroyed() {
        return mDestroyed;
    }

    public boolean isFinishing() {
        return mIsFinishing;
    }

    protected IMenuInflater getSupportMenuInflater() {
        return mActivity.getSupportMenuInflater();
    }
    
}
