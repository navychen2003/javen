package org.javenstudio.provider.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.javenstudio.android.app.ContentFragment;
import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuInflater;
import org.javenstudio.cocoka.app.IMenuItem;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderBinder;

public class ProviderListFragment extends ContentFragment {
	private static final Logger LOG = Logger.getLogger(ProviderListFragment.class);
	
	public ProviderListActivity getProviderActivity() { 
		return (ProviderListActivity)getActivity();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ProviderListActivity activity = getProviderActivity();
		activity.resetContentBackground();
		
		Provider provider = activity.getCurrentProvider();
		if (provider != null) {
			activity.setContentBackground(provider);
			provider.setContentBackground(activity);
			provider.onFragmentCreate(activity);
			
			IOptionsMenu optionsMenu = provider.getOptionsMenu();
			if (optionsMenu != null) { 
				boolean hasMenu = optionsMenu.hasOptionsMenu(activity);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("onCreate: setHasOptionsMenu: provider=" + provider 
							+ " menu=" + optionsMenu + " hasMenu=" + hasMenu);
				}
				
				setHasOptionsMenu(hasMenu);
				invalidateOptionsMenu();
			}
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			Provider provider = activity.getCurrentProvider();
			if (provider != null) { 
				final ProviderBinder binder = provider.getBinder();
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("onCreateView: provider=" + provider + " binder=" + binder 
							+ " savedInstanceState=" + savedInstanceState);
				}
				
				if (binder != null) {
					final View rootView = binder.bindView(
							activity, inflater, container, savedInstanceState);
					
					binder.bindBehindAbove(activity, inflater, savedInstanceState);
					
					return rootView;
				}
			}
			
			return new LinearLayout(activity.getApplicationContext());
		}
		return null;
	}
	
	@Override
	public boolean onCreateOptionsMenu(IMenu menu, IMenuInflater inflater) {
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			Provider provider = activity.getCurrentProvider();
			IOptionsMenu optionsMenu = provider != null ? provider.getOptionsMenu() : null;
			if (optionsMenu != null) return optionsMenu.onCreateOptionsMenu(activity, menu, inflater);
		}
        return false;
    }
	
	@Override
    public boolean onPrepareOptionsMenu(IMenu menu) {
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			Provider provider = activity.getCurrentProvider();
			IOptionsMenu optionsMenu = provider != null ? provider.getOptionsMenu() : null;
			if (optionsMenu != null) return optionsMenu.onPrepareOptionsMenu(activity, menu);
		}
        return false;
    }
    
	@Override
    public boolean onOptionsItemSelected(IMenuItem item) {
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			Provider provider = activity.getCurrentProvider();
			IOptionsMenu optionsMenu = provider != null ? provider.getOptionsMenu() : null;
			if (optionsMenu != null) return optionsMenu.onOptionsItemSelected(activity, item);
		}
        return false;
    }
	
	//@Override
	//public void onDestroyOptionsMenu() {
	//	super.onDestroyOptionsMenu();
	//	removeOptionsMenu();
	//}
	
	@SuppressWarnings("unused")
	private void removeOptionsMenu() {
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			Provider provider = activity.getCurrentProvider();
			IOptionsMenu optionsMenu = provider != null ? provider.getOptionsMenu() : null;
			if (optionsMenu != null) optionsMenu.removeOptionsMenu(activity);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			Provider provider = activity.getCurrentProvider();
			if (provider != null) provider.onSaveFragmentState(outState);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			activity.refreshContent(false);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		ProviderListActivity activity = getProviderActivity();
		if (activity != null) {
			//activity.resetContentBackground();
			//removeOptionsMenu();
		}
	}
	
}
