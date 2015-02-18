package org.javenstudio.cocoka.opengl;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.util.ApiHelper;
import org.javenstudio.common.util.Logger;

public class GLActionBar implements IActionBar.OnNavigationListener {
	private static final Logger LOG = Logger.getLogger(GLActionBar.class);
	
    private final Context mContext;
    private final GLActivity mActivity;
    private final IActionBar mActionBar;

    protected static class ActionItem {
        public int action;
        public boolean enabled;
        public boolean visible;
        public int spinnerTitle;
        public int dialogTitle;
        public int clusterBy;

        public ActionItem(int action, boolean applied, boolean enabled, 
        		int title, int clusterBy) {
            this(action, applied, enabled, title, title, clusterBy);
        }

        public ActionItem(int action, boolean applied, boolean enabled, 
        		int spinnerTitle, int dialogTitle, int clusterBy) {
            this.action = action;
            this.enabled = enabled;
            this.spinnerTitle = spinnerTitle;
            this.dialogTitle = dialogTitle;
            this.clusterBy = clusterBy;
            this.visible = true;
        }
    }

    public GLActionBar(GLActivity activity) {
        mActionBar = activity.getSupportActionBar();
        mContext = activity.getActivityContext();
        mActivity = activity;
    }

    public int getHeight() {
        return mActionBar != null ? mActionBar.getHeight() : 0;
    }

    public void onConfigurationChanged() {
        //if (mActionBar != null && mAlbumModeListener != null) {
        //    OnAlbumModeSelectedListener listener = mAlbumModeListener;
        //    enableAlbumModeMenu(mLastAlbumModeSelected, listener);
        //}
    }

    @TargetApi(ApiHelper.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setHomeButtonEnabled(boolean enabled) {
        if (mActionBar != null) mActionBar.setHomeButtonEnabled(enabled);
    }

    public void setDisplayOptions(boolean displayHomeAsUp, boolean showTitle) {
        if (mActionBar == null) return;
        
        int options = 0;
        if (displayHomeAsUp) options |= IActionBar.DISPLAY_HOME_AS_UP;
        if (showTitle) options |= IActionBar.DISPLAY_SHOW_TITLE;

        mActionBar.setDisplayOptions(options,
                IActionBar.DISPLAY_HOME_AS_UP | IActionBar.DISPLAY_SHOW_TITLE);
        mActionBar.setHomeButtonEnabled(displayHomeAsUp);
    }

    public void setIcon(int iconRes) { 
    	if (mActionBar != null) mActionBar.setIcon(iconRes);
    }
    
    public void setIcon(Drawable icon) { 
    	if (mActionBar != null) mActionBar.setIcon(icon);
    }
    
    public void setTitle(String title) {
        if (mActionBar != null) { 
        	mActionBar.setDisplayShowTitleEnabled(title != null);
        	mActionBar.setTitle(title); 
        }
    }

    public void setTitle(int titleId) {
        if (mActionBar != null) {
        	mActionBar.setDisplayShowTitleEnabled(titleId != 0);
            mActionBar.setTitle(mContext.getString(titleId));
        }
    }

    public void setSubtitle(String title) {
        if (mActionBar != null) mActionBar.setSubtitle(title);
    }

    public void setCustomView(View view) { 
    	if (mActionBar != null) { 
    		mActionBar.setDisplayShowCustomEnabled(view != null);
    		mActionBar.setCustomView(view); 
    	}
    }
    
    public void show() {
        if (mActionBar != null) mActionBar.show();
    }

    public void hide() {
        if (mActionBar != null) mActionBar.hide();
    }

    public void addOnMenuVisibilityListener(IActionBar.OnMenuVisibilityListener listener) {
        if (mActionBar != null) mActionBar.addOnMenuVisibilityListener(listener);
    }

    public void removeOnMenuVisibilityListener(IActionBar.OnMenuVisibilityListener listener) {
        if (mActionBar != null) mActionBar.removeOnMenuVisibilityListener(listener);
    }

    public boolean setSelectedAction(int type) {
        if (mActionBar == null) return false;

        return false;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return false;
    }

    private IMenu mActionBarMenu = null;
    private IMenuItem mProgressMenuItem = null;

    public final void createActionBarMenu(int menuRes, IMenu menu) {
    	if (LOG.isDebugEnabled()) 
    		LOG.debug("createActionBarMenu: menu=" + menu + " menuRes=" + menuRes);
    	
        mActivity.getSupportMenuInflater().inflate(menuRes, menu);
        mActionBarMenu = menu;
        mProgressMenuItem = null;
        
        onActionBarMenuCreated(menu);
    }
    
    protected void onActionBarMenuCreated(IMenu menu) {}

    public IMenu getMenu() {
        return mActionBarMenu;
    }

    public void setProgressMenuItem(int itemRes) { 
    	if (mActionBarMenu != null) 
    		mProgressMenuItem = mActionBarMenu.findItem(itemRes);
    	else 
    		mProgressMenuItem = null;
    }
    
    public IMenuItem getProgressMenuItem() { 
    	return mProgressMenuItem;
    }
    
    //public void setShareIntent(Intent shareIntent) {}
    //public void onActionShareTo(Activity activity) {}
    
}
