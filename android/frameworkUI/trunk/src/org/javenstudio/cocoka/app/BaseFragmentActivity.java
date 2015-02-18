package org.javenstudio.cocoka.app;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

import com.actionbarsherlock.ActionBarSherlock;

public abstract class BaseFragmentActivity extends FragmentActivity 
		implements ActionBarSherlock.OnCreatePanelMenuListener, ActionBarSherlock.OnPreparePanelListener, 
			ActionBarSherlock.OnMenuItemSelectedListener, ActionBarSherlock.OnActionModeStartedListener, 
			ActionBarSherlock.OnActionModeFinishedListener {
	private static final Logger LOG = Logger.getLogger(BaseFragmentActivity.class);
	
	private boolean mCanCommitFragment = false;
	protected boolean canCommitFragment() { return mCanCommitFragment; }
	
	public abstract SupportActionBar getSupportActionBarOrNull();
	//public abstract IActionBar getSupportActionBar();
	
    ///////////////////////////////////////////////////////////////////////////
    // General lifecycle/callback dispatching
    ///////////////////////////////////////////////////////////////////////////
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ResourceHelper.addActivity(this);
		mCanCommitFragment = true;
	}
	
	@Override
	protected void onStop() {
		SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchStop();
		
	    super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchDestroy();

		mCanCommitFragment = false;
	    super.onDestroy();
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchPostResume();
    }

    @Override
    protected void onPause() {
        SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchPause();
		
        super.onPause();
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchPostCreate(savedInstanceState);
		
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected final void onTitleChanged(CharSequence title, int color) {
        SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchTitleChanged(title, color);
		
        super.onTitleChanged(title, color);
    }

    @Override
    public final boolean onMenuOpened(int featureId, android.view.Menu menu) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
        if (actionBar != null && actionBar.getImpl().dispatchMenuOpened(featureId, menu)) 
            return true;
        
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public final void onPanelClosed(int featureId, android.view.Menu menu) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchPanelClosed(featureId, menu);
		
        super.onPanelClosed(featureId, menu);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
        if (actionBar != null && actionBar.getImpl().dispatchKeyEvent(event)) 
            return true;
        
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("onSaveInstanceState: out=" + outState);
    	
    	//mCanCommitFragment = false;
        super.onSaveInstanceState(outState);
        
        SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("onRestoreInstanceState: saved=" + savedInstanceState);
    	
        super.onRestoreInstanceState(savedInstanceState);
        //mCanCommitFragment = true;
        
        SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			actionBar.getImpl().dispatchRestoreInstanceState(savedInstanceState);
    }
	
    public final IMenuInflater getSupportMenuInflater() {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			return new SupportMenuInflaterImpl(actionBar.getImpl().getMenuInflater());
		
		return new AndroidMenuInflater(getMenuInflater());
    }

    @Override
    public final void invalidateOptionsMenu() {
    	if (LOG.isDebugEnabled()) LOG.debug("invalidateOptionsMenu");
    	
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) {
			actionBar.getImpl().dispatchInvalidateOptionsMenu();
			return;
		}
		super.invalidateOptionsMenu();
    }

    @Override
    public final void supportInvalidateOptionsMenu() {
    	if (LOG.isDebugEnabled()) LOG.debug("supportInvalidateOptionsMenu");
    	
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) {
			actionBar.getImpl().dispatchInvalidateOptionsMenu();
			return;
		}
    }

    @Override
    public final boolean onCreateOptionsMenu(android.view.Menu menu) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			return actionBar.getImpl().dispatchCreateOptionsMenu(menu);
		
		if (onCreateOptionsMenu(new AndroidMenu(menu)))
			return true;
		
		return super.onCreateOptionsMenu(menu);
    }

    @Override
    public final boolean onPrepareOptionsMenu(android.view.Menu menu) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			return actionBar.getImpl().dispatchPrepareOptionsMenu(menu);
		
		if (onPrepareOptionsMenu(new AndroidMenu(menu)))
			return true;
		
		return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public final void openOptionsMenu() {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
        if (actionBar == null || !actionBar.getImpl().dispatchOpenOptionsMenu()) 
            super.openOptionsMenu();
    }

    @Override
    public final void closeOptionsMenu() {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
        if (actionBar == null || !actionBar.getImpl().dispatchCloseOptionsMenu()) 
            super.closeOptionsMenu();
    }
    
	@Override
	public final boolean onOptionsItemSelected(MenuItem item) {
		SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) 
			return actionBar.getImpl().dispatchOptionsItemSelected(item);
		
		if (onOptionsItemSelected(new AndroidMenuItem(item)))
			return true;
		
		return super.onOptionsItemSelected(item);
	}
    
    ///////////////////////////////////////////////////////////////////////////
    // Content
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void addContentView(View view, LayoutParams params) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().addContentView(view, params);
    	else
    		super.addContentView(view, params);
    }

    @Override
    public void setContentView(int layoutResId) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setContentView(layoutResId);
    	else
    		super.setContentView(layoutResId);
    }

    @Override
    public void setContentView(View view, LayoutParams params) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setContentView(view, params);
    	else
    		super.setContentView(view, params);
    }

    @Override
    public void setContentView(View view) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setContentView(view);
    	else
    		super.setContentView(view);
    }

    public void requestWindowFeature(long featureId) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().requestFeature((int)featureId);
    	else
    		super.requestWindowFeature((int)featureId);
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Progress Indication
    ///////////////////////////////////////////////////////////////////////////

    public void setSupportProgress(int progress) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setProgress(progress);
    }

    public void setSupportProgressBarIndeterminate(boolean indeterminate) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setProgressBarIndeterminate(indeterminate);
    }

    public void setSupportProgressBarIndeterminateVisibility(boolean visible) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setProgressBarIndeterminateVisibility(visible);
    }

    public void setSupportProgressBarVisibility(boolean visible) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setProgressBarVisibility(visible);
    }

    public void setSupportSecondaryProgress(int secondaryProgress) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
    	if (actionBar != null)
        	actionBar.getImpl().setSecondaryProgress(secondaryProgress);
    }
	
    ///////////////////////////////////////////////////////////////////////////
    // Support methods
    ///////////////////////////////////////////////////////////////////////////
    
	@Override
	public void onActionModeFinished(com.actionbarsherlock.view.ActionMode mode) {
	}

	@Override
	public void onActionModeStarted(com.actionbarsherlock.view.ActionMode mode) {
	}

	@Override
	public final boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) 
            return onOptionsItemSelected(item);
        
		return false;
	}

	@Override
	public final boolean onPreparePanel(int featureId, View view, com.actionbarsherlock.view.Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) 
            return onPrepareOptionsMenu(menu);
        
		return false;
	}

	@Override
	public final boolean onCreatePanelMenu(int featureId, com.actionbarsherlock.view.Menu menu) {
		if (featureId == Window.FEATURE_OPTIONS_PANEL) 
            return onCreateOptionsMenu(menu);
        
		return false;
	}

    public final boolean onCreateOptionsMenu(SupportMenu menu) {
        return onCreateOptionsMenu(new SupportMenuImpl(menu));
    }
	
    public final boolean onPrepareOptionsMenu(SupportMenu menu) {
        return onPrepareOptionsMenu(new SupportMenuImpl(menu));
    }
    
    public final boolean onOptionsItemSelected(SupportMenuItem item) {
        return onOptionsItemSelected(new SupportMenuItemImpl(item));
    }
    
    public boolean onCreateOptionsMenu(IMenu menu) {
        return true;
    }
	
    public boolean onPrepareOptionsMenu(IMenu menu) {
        return true;
    }
    
    public boolean onOptionsItemSelected(IMenuItem item) {
        return false;
    }
    
    public View findViewByName(String name) {
    	return findViewByName(name, 0);
    }
    
    public View findViewByName(String name, int defRes) {
		int viewId = name == null || name.length() == 0 ? 0 : 
			//Resources.getSystem().getIdentifier(name, "id", "android");
			getResources().getIdentifier(name, "id", "android");
		if (viewId == 0) viewId = defRes;
		if (viewId != 0) return getWindow().findViewById(viewId);
		return null;
	}
    
    public TextView findTextView(String name, int defRes) {
    	View view =  findViewByName(name, defRes);
		if (view != null && view instanceof TextView) {
			TextView text = (TextView)view;
			return text;
		}
		return null;
	}
    
}
