package org.javenstudio.provider.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.ActionError;
import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.IMenuExecutor;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.app.SimpleActivity;
import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.IProviderActivity;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.ProviderController;
import org.javenstudio.provider.ProviderModel;

public abstract class ProviderActivity extends SimpleActivity 
		implements IProviderActivity {
	private static final Logger LOG = Logger.getLogger(ProviderActivity.class);
	
	@Override
	protected ProviderCallback createCallback() { 
		return new ProviderCallback() { 
				@Override
				protected void invokeByModel(int action, Object params) { 
					ProviderActivity.this.onInvokeByModel(action, params);
				}
				@Override
				public ProviderController getController() { 
					return ProviderActivity.this.getController();
				}
				@Override
				public Provider getProvider() { 
					return ProviderActivity.this.getCurrentProvider();
				}
				@Override
			    public void onActionError(ActionError error) {
					ProviderActivity.this.onActionError(error);
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
	
	@Override
	protected ActionHelper createActionHelper() { 
		return new ActionHelper(this, getSupportActionBar());
	}
	
    public abstract ProviderController getController();
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
	protected void onDestroy() {
		super.onDestroy();
		Provider provider = setCurrentProvider(null);
		if (provider != null) provider.onDetach(this);
	}
	
	@Override
	public boolean onActionRefresh() { 
		refreshContent(true);
		return true;
	}
	
	@Override
	protected boolean onFlingToRight() { 
		Provider provider = getCurrentProvider();
		if (provider != null) return provider.onFlingToRight(this);
		return false; 
	}
	
	@Override
	public void setContentFragment() { 
		getActivityHelper().setRefreshView(null);
		
		Provider provider = getCurrentProvider();
		if (provider != null) provider.onFragmentPreCreate(this);
		
		setContentFragment(new ProviderFragment());
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
	public boolean onActionHome() { 
		if (getActivityHelper().onActionHome()) 
			return true;
		
		Provider provider = getCurrentProvider();
		if (provider != null && provider.onActionHome(this))
			return true;
		
		if (super.onActionHome()) 
			return true;
		
		onBackPressed(); //onAnimationBack();
		return true;
	}
	
	@Override
	protected void overridePendingTransition() { 
		Provider provider = getCurrentProvider();
		if (provider != null && provider.overridePendingTransitionOnFinish(this))
			return;
		
		super.overridePendingTransition();
	}
	
}
