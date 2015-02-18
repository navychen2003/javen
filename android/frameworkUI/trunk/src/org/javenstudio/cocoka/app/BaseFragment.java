package org.javenstudio.cocoka.app;

import org.javenstudio.common.util.Logger;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

public abstract class BaseFragment extends Fragment {
	private static final Logger LOG = Logger.getLogger(BaseFragment.class);

	public final BaseFragmentActivity getFragmentActivity() {
		return (BaseFragmentActivity)getActivity();
	}
	
	public final SupportActionBar getSupportActionBarOrNull() {
		return getFragmentActivity().getSupportActionBarOrNull();
	}
	
	public final void invalidateOptionsMenu() {
		if (LOG.isDebugEnabled()) LOG.debug("invalidateOptionsMenu");
		getActivity().invalidateOptionsMenu();
	}
	
	@Override
	public final void setHasOptionsMenu(boolean hasMenu) {
		if (LOG.isDebugEnabled()) LOG.debug("setHasOptionsMenu: hasMenu=" + hasMenu);
		super.setHasOptionsMenu(hasMenu);
	}
	
	@Override
	public final void setMenuVisibility(boolean menuVisible) {
		if (LOG.isDebugEnabled()) LOG.debug("setMenuVisibility: menuVisible=" + menuVisible);
		super.setMenuVisibility(menuVisible);
	}
	
	@Override
	public void onAttach(Activity activity) {
		if (LOG.isDebugEnabled()) LOG.debug("onAttach: activity=" + activity);
		super.onAttach(activity);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (LOG.isDebugEnabled()) LOG.debug("onCreate");
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onStart() {
		if (LOG.isDebugEnabled()) LOG.debug("onStart");
		super.onStart();
	}
	
	@Override
	public void onResume() {
		if (LOG.isDebugEnabled()) LOG.debug("onResume");
		super.onResume();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (LOG.isDebugEnabled()) LOG.debug("onSaveInstanceState: out=" + outState);
		super.onSaveInstanceState(outState);
    }
    
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
		if (LOG.isDebugEnabled()) LOG.debug("onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }
	
	@Override
	public void onPause() {
		if (LOG.isDebugEnabled()) LOG.debug("onPause");
		super.onPause();
	}
	
	@Override
	public void onStop() {
		if (LOG.isDebugEnabled()) LOG.debug("onStop");
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		if (LOG.isDebugEnabled()) LOG.debug("onDestroy");
		super.onDestroy();
	}
	
	@Override
	public void onDetach() {
		if (LOG.isDebugEnabled()) LOG.debug("onDetach");
		super.onDetach();
	}
	
	@Override
    public final void onCreateOptionsMenu(android.view.Menu menu, 
    		android.view.MenuInflater inflater) {
		if (LOG.isDebugEnabled()) LOG.debug("onCreateOptionsMenu: menu=" + menu);
		
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) {
			//actionBar.getImpl().dispatchCreateOptionsMenu(menu);
			return;
		}
		
		if (onCreateOptionsMenu(new AndroidMenu(menu), new AndroidMenuInflater(inflater)))
			return;
		
		super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public final void onPrepareOptionsMenu(android.view.Menu menu) {
    	SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) {
			//actionBar.getImpl().dispatchPrepareOptionsMenu(menu);
			return;
		}
		
		if (onPrepareOptionsMenu(new AndroidMenu(menu)))
			return;
		
		super.onPrepareOptionsMenu(menu);
    }

	@Override
	public final boolean onOptionsItemSelected(MenuItem item) {
		SupportActionBar actionBar = getSupportActionBarOrNull();
		if (actionBar != null) {
			return true; //actionBar.getImpl().dispatchOptionsItemSelected(item);
		}
		
		if (onOptionsItemSelected(new AndroidMenuItem(item)))
			return true;
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onDestroyOptionsMenu() {
		if (LOG.isDebugEnabled()) LOG.debug("onDestroyOptionsMenu");
		super.onDestroyOptionsMenu();
    }
	
	public boolean onCreateOptionsMenu(IMenu menu, IMenuInflater inflater) {
        return false;
    }
	
    public boolean onPrepareOptionsMenu(IMenu menu) {
        return false;
    }
    
    public boolean onOptionsItemSelected(IMenuItem item) {
        return false;
    }
	
}
