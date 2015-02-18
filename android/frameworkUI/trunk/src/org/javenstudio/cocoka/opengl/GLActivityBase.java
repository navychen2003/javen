package org.javenstudio.cocoka.opengl;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.javenstudio.cocoka.app.BaseFragmentActivity;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.util.BitmapHelper;
import org.javenstudio.cocoka.util.BitmapPool;

public abstract class GLActivityBase extends BaseFragmentActivity 
		implements GLActivityContext {

    private GLRootView mGLRootView;
    private GLStateManager mStateManager;
    private GLActionBar mActionBar;
    private OrientationManager mOrientationManager;
    private TransitionStore mTransitionStore = new TransitionStore();
    private boolean mDisableToggleStatusBar;
	
	@Override
	public final Context getActivityContext() {
		return this; 
	}

    protected boolean isFullscreen() {
        return (getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }
	
    protected boolean isRotationLockEnable() { 
    	ContentResolver resolver = getContentResolver();
    	return Settings.System.getInt(
                resolver, Settings.System.ACCELEROMETER_ROTATION, 0) != 1;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOrientationManager = new OrientationManager(this);
        toggleStatusBarByOrientation();
        getWindow().setBackgroundDrawable(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            getGLStateManager().saveState(outState);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mStateManager.onConfigurationChange(config);
        getGLActionBar().onConfigurationChanged();
        invalidateOptionsMenu();
        toggleStatusBarByOrientation();
    }

    @Override
    public boolean onCreateOptionsMenu(IMenu menu) {
        super.onCreateOptionsMenu(menu);
        return getGLStateManager().createOptionsMenu(menu);
    }

    public synchronized final GLStateManager getGLStateManager() {
        if (mStateManager == null) 
            mStateManager = createGLStateManager();
        
        return mStateManager;
    }

    public final GLRoot getGLRoot() {
        return mGLRootView;
    }

    public final OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    @Override
    public final void setContentView(int resId) {
        super.setContentView(resId);
        mGLRootView = (GLRootView) findGLRootView();
    }

    protected abstract void setContentView();
    protected abstract View findGLRootView();
    protected abstract View findGLMainRootView();
    
    protected abstract GLActionBar createGLActionBar();
    protected abstract GLStateManager createGLStateManager();
    
    @Override
    protected void onStart() {
        super.onStart();
        
    }

    @Override
    protected void onStop() {
        super.onStop();
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLRootView.lockRenderThread();
        
        try {
            getGLStateManager().resume();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        
        mGLRootView.onResume();
        mOrientationManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        mOrientationManager.pause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        
        try {
            getGLStateManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        
        clearBitmapPool(BitmapHelper.getMicroThumbPool());
        clearBitmapPool(BitmapHelper.getThumbPool());

        BitmapHelper.getBytesBufferPool().clear();
    }

    private static void clearBitmapPool(BitmapPool pool) {
        if (pool != null) pool.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGLRootView.lockRenderThread();
        
        try {
            getGLStateManager().destroy();
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            getGLStateManager().notifyActivityResult(requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            if (!getGLStateManager().onBackPressed())
            	onActivityBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    protected void onActivityBackPressed() { 
    	super.onBackPressed();
    }
    
    public synchronized final GLActionBar getGLActionBar() {
        if (mActionBar == null) 
            mActionBar = createGLActionBar();
        
        return mActionBar;
    }
    
    @Override
    public boolean onOptionsItemSelected(IMenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getGLStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    protected final void disableToggleStatusBar() {
        mDisableToggleStatusBar = true;
    }

    // Shows status bar in portrait view, hide in landscape view
    private void toggleStatusBarByOrientation() {
        if (mDisableToggleStatusBar) return;

        Window win = getWindow();
        //if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        //    win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //} else {
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //}
    }

    public final TransitionStore getTransitionStore() {
        return mTransitionStore;
    }

}
