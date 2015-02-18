package org.javenstudio.provider.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.IMenuExecutor;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.SlidingActivity;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.IProviderActivity;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.ProviderController;
import org.javenstudio.provider.ProviderModel;

public abstract class ProviderListActivity extends SlidingActivity 
		implements IProviderActivity {
	private static final Logger LOG = Logger.getLogger(ProviderListActivity.class);
	
	@Override
	protected ProviderCallback createCallback() { 
		return new ProviderCallback() { 
				@Override
				protected void invokeByModel(int action, Object params) { 
					ProviderListActivity.this.onInvokeByModel(action, params);
				}
				@Override
				public ProviderController getController() { 
					return ProviderListActivity.this.getController();
				}
				@Override
				public Provider getProvider() { 
					return ProviderListActivity.this.getCurrentProvider();
				}
				@Override
			    public void onActionError(ActionError error) {
					ProviderListActivity.this.onActionError(error);
				}
				@Override
				public void showContentMessage(CharSequence message) { 
					postShowContentMessage(message);
				}
				@Override
				public void showProgressDialog(CharSequence message) { 
					postShowProgressDialog(message);
				}
				@Override
				public void hideProgressDialog() { 
					postHideProgressDialog();
				}
				@Override
				public boolean isActionProcessing() { 
					ActionHelper helper = getActionHelper();
					if (helper != null) return helper.getActionExecutor().isActionProcessing();
					return false;
				}
			};
	}
	
    public abstract ProviderController getController();
    public abstract ActionItem[] getNavigationItems();
	
    public abstract Provider getCurrentProvider();
    public abstract Provider setCurrentProvider(Provider provider);
    
    @Override
	public CharSequence getLastUpdatedLabel() { 
    	Provider p = getCurrentProvider();
    	if (p != null) return p.getLastUpdatedLabel(this);
		return null; 
	}
    
    @Override
    public boolean isContentProgressEnabled() { 
    	Provider p = getCurrentProvider();
    	if (p != null) return p.isContentProgressEnabled();
    	return false; 
    }
    
    @Override
    protected boolean isLockOrientationDisabled(int orientation) { 
    	Provider p = getCurrentProvider();
    	if (p != null) return p.isLockOrientationDisabled(orientation);
    	return false; 
    }
    
    @Override
	protected boolean isUnlockOrientationDisabled() { 
		Provider p = getCurrentProvider();
    	if (p != null) return p.isUnlockOrientationDisabled();
		return false; 
	}
    
    public void setContentBackground(Provider p) {}
    
    public void resetContentBackground() { 
    	View aboveView = getContentAboveView();
		if (aboveView != null && aboveView instanceof ViewGroup) {
			ViewGroup aboveGroup = (ViewGroup)aboveView;
			aboveGroup.removeAllViews();
			aboveGroup.setVisibility(View.GONE);
		}
		
		View behindView = getContentBehindView();
		if (behindView != null && behindView instanceof ViewGroup) {
			ViewGroup behindGroup = (ViewGroup)behindView;
			behindGroup.removeAllViews();
			behindGroup.setVisibility(View.GONE);
		}
    }
    
    public IMenuExecutor getMenuExeccutor() { 
    	IMenuOperation p = getMenuOperation();
    	if (p != null) return p.getMenuExecutor();
    	return null;
    }
    
    @Override
    public IMenuOperation getMenuOperation() { 
    	Provider p = getCurrentProvider();
    	if (p != null) return p.getMenuOperation();
    	return null;
    }
    
	@Override
	public ActionItem[] getSecondaryNavigationItems() { 
		return null;
	}
	
	@Override
	protected void onInvokeByModel(int action, Object params) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onInvokeByModel: action=" + action+ " params=" + params);
		
		switch (action) { 
		case Model.ACTION_ONLOADERSTART:
		case ProviderModel.ACTION_ONFETCHSTART:
			postShowProgress(false);
			break; 
		case Model.ACTION_ONLOADERSTOP:
		case ProviderModel.ACTION_ONFETCHSTOP:
			postHideProgress(false);
			break; 
		default:
			super.onInvokeByModel(action, params);
			break;
		}
	}
	
	//@Override
	//protected void onDataSetChanged(Object params) {
	//	super.onDataSetChanged(params);
	//	Provider p = getCurrentProvider();
	//	if (p != null) p.onDataSetChanged(this, params);
	//}
	
	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		getController().initialize(getCallback()); 
        
        super.doOnCreate(savedInstanceState);
        setSlidingActionBarEnabled(true);
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		Provider p = getCurrentProvider();
		if (p != null) p.onSaveActivityState(savedInstanceState);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Provider p = getCurrentProvider();
		if (p != null) p.onRestoreActivityState(savedInstanceState);
	}
	
	@Override
	protected ActionHelper createActionHelper() { 
		return new ActionHelperImpl(this, getSupportActionBar());
	}
	
	private class ActionHelperImpl extends ActionHelper { 
		public ActionHelperImpl(Activity activity, IActionBar actionBar) { 
			super(activity, actionBar);
		}
		
		@Override
		public boolean onNavigationItemSelected(int position, long id) {
			Provider provider = getCurrentProvider();
			if (mStarted && provider != null) {
				return provider.onActionItemSelected(ProviderListActivity.this, position, id);
			}
			return true;
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Provider provider = setCurrentProvider(null);
		if (provider != null) provider.onDetach(this);
	}
	
	@Override
	protected boolean onActionRefresh() { 
		refreshContent(true);
		return true;
	}
	
	@Override
	public void setContentFragment() { 
		getActivityHelper().setRefreshView(null);
		
		Provider provider = getCurrentProvider();
		if (provider != null) provider.onFragmentPreCreate(this);
		
		setContentFragment(new ProviderListFragment());
	}
	
	@Override
	public void setContentProvider(Provider provider) { 
		setContentProvider(provider, false);
	}
	
	@Override
	public final void setContentProvider(Provider provider, boolean force) {
		if (provider != null && (provider != getCurrentProvider() || force)) {
			Provider old = getCurrentProvider();
			setCurrentProvider(provider);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("setContentProvider: provider=" + provider 
						+ " old=" + old + " force=" + force);
			}
			
			getController().setProvider(provider);
			if (old != null && old != provider) old.onDetach(this);
			provider.onAttach(this);
			
			//provider.onFragmentPreCreate(this);
			setContentFragment();
		}
	}
	
	@Override
	public void refreshContent(boolean force) { 
		ActionHelper helper = getActionHelper();
		if (helper != null && helper.isActionMode()) 
			return;
		
		getController().refreshContent(getCallback(), force);
	}
	
	@Override
	protected boolean onActionHome() { 
		if (getActivityHelper().onActionHome()) 
			return true;
		
		Provider provider = getCurrentProvider();
		if (provider != null && provider.onActionHome(this))
			return true;
		
		return super.onActionHome();
	}
	
	@Override
    public void onBackPressed() {
		if (getActivityHelper().onActionHome()) 
			return;
		
		if (!isSlidingMenuShowing()) {
			Provider provider = getCurrentProvider();
			if (provider != null && provider.onBackPressed(this))
				return;
		}
		
		super.onBackPressed();
	}
	
	@Override
	protected void overridePendingTransition() { 
		Provider provider = getCurrentProvider();
		if (provider != null && provider.overridePendingTransitionOnFinish(this))
			return;
		
		super.overridePendingTransition();
	}
	
}
