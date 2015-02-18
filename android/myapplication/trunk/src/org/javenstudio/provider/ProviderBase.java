package org.javenstudio.provider;

import android.app.ActionBar;

import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.cocoka.app.IActionAdapterFactory;
import org.javenstudio.cocoka.app.IActionNavigationListener;
import org.javenstudio.cocoka.app.IOptionsMenu;
import org.javenstudio.common.util.Logger;

public abstract class ProviderBase extends Provider {
	private static final Logger LOG = Logger.getLogger(ProviderBase.class);

	private IActivity mActivity = null;
	private int mIndicatorRes = 0;
	
	public ProviderBase(String name, int iconRes) { 
		super(name, iconRes);
	}
	
	@Override
	public final void onFragmentPreCreate(IActivity activity) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("onFragmentPreCreate: activity=" + activity + " provider=" + this);
		
		ActionHelper helper = activity.getActionHelper();
		if (helper != null) { 
			helper.finishSelectMode();
			helper.setActionItems(getActionItems(activity), getActionNavigationMode(), 
					getActionAdapterFactory(activity), getActionNavigationListener(activity));
			helper.setHomeIcon(getHomeIconRes());
			helper.setHomeAsUpIndicator(getHomeAsUpIndicatorRes());
			helper.setActionTitle(getActionTitle(), getActionSubTitle());
			helper.setCustomView(getActionCustomView(activity), 
					getActionCustomLayoutParams(activity));
		}
		setActionBarTitle(activity);
		setOrientation(activity);
	}
	
	@Override
	public final void onFragmentCreate(IActivity activity) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("onFragmentCreate: activity=" + activity + " provider=" + this);
		
		ActionHelper helper = activity.getActionHelper();
		if (helper != null) { 
			helper.setHomeIcon(getHomeIconRes());
			helper.setHomeAsUpIndicator(getHomeAsUpIndicatorRes());
		}
	}
	
	public IActionAdapterFactory getActionAdapterFactory(IActivity activity) {
		return null;
	}
	
	public IActionNavigationListener getActionNavigationListener(IActivity activity) {
		return null;
	}
	
	public int getActionNavigationMode() {
		return ActionBar.NAVIGATION_MODE_LIST;
	}
	
	public int getHomeIconRes() {
		return getIconRes();
	}
	
	@Override
	public int getHomeAsUpIndicatorRes() {
		return mIndicatorRes;
	}
	
	public void setHomeAsUpIndicator(int resId) {
		mIndicatorRes = resId;
	}
	
	public CharSequence getActionTitle() {
		return getTitle();
	}
	
	public CharSequence getActionSubTitle() {
		return getSubTitle();
	}
	
	public void setActionBarTitle(IActivity activity) {
		activity.setTitle(getTitle());
	}
	
	public void setOrientation(IActivity activity) {
		activity.getActivityHelper().unlockOrientationIfCan();
	}
	
	@Override
	public void onAttach(IActivity activity) {
		if (activity == null) return;
		mActivity = activity;
	}
	
	@Override
	public void onDetach(IActivity activity) {
		if (activity == null || activity != mActivity) return;
		if (LOG.isDebugEnabled()) {
			LOG.debug("onDetach: provider=" + this 
					+ " activity=" + activity);
		}
		IOptionsMenu menu = getOptionsMenu();
		if (menu != null && activity != null) 
			menu.removeOptionsMenu(activity.getActivity());
	}
	
	@Override
	public boolean onFlingToRight(IActivity activity) { 
		if (activity == null || activity != mActivity) return false;
		if (LOG.isDebugEnabled()) {
			LOG.debug("onFlingToRight: provider=" + this 
					+ " activity=" + activity);
		}
		activity.onBackPressed();
		return true;
	}
	
}
