package org.javenstudio.provider;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.IMenuOperation;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.IActionNavigationListener;
import org.javenstudio.common.util.Logger;

public class ProviderList extends ProviderBase {
	private static final Logger LOG = Logger.getLogger(ProviderList.class);

	protected final List<Provider> mProviders;
	protected Provider mSelectProvider = null;
	private ActionItem mSelectActionItem = null;
	private ProviderActionItem[] mActionItems = null;
	private IActivity mActivity = null;
	
	public ProviderList(String name, int iconRes) { 
		super(name, iconRes);
		mProviders = new ArrayList<Provider>();
	}
	
	public final void clearProviders() {
		synchronized (mProviders) {
			mProviders.clear();
			
			mSelectProvider = null;
			mSelectActionItem = null;
			mActionItems = null;
		}
	}
	
	public final void addProvider(Provider source) { 
		if (source != null) {
			synchronized (mProviders) {
				for (Provider item : mProviders) { 
					if (item == source) return;
				}
				
				mProviders.add(source);
			}
		}
	}
	
	public final Provider[] getProviders() { 
		synchronized (mProviders) {
			return mProviders.toArray(new Provider[mProviders.size()]);
		}
	}
	
	public final int getProviderCount() { 
		synchronized (mProviders) {
			return mProviders.size();
		}
	}
	
	public Provider getSelectProvider() {
		Provider selectItem = mSelectProvider;
		if (selectItem == null) {
			synchronized (mProviders) {
				if (mProviders.size() > 0) {
					selectItem = mProviders.get(0);
					mSelectProvider = selectItem;
					//mSelectIndex = 0;
				}
			}
		}
		return selectItem;
	}
	
	@Override
	public IMenuOperation getMenuOperation() {
		Provider p = getSelectProvider();
		if (p != null) return p.getMenuOperation();
		
		return super.getMenuOperation();
	}
	
	@Override
	public boolean onActionHome(IActivity activity) { 
		boolean result = false;
		
		Provider p = getSelectProvider();
		if (p != null) result = p.onActionHome(activity);
		else result = super.onActionHome(activity);
		
		if (result) { 
			ActionItem item = mSelectActionItem;
			if (item != null && p != null) { 
				item.setSubTitle(p.getSubTitle());
				//item.rebindViews(activity.getActivity());
			}
		}
		
		return result;
	}
	
	@Override
	public boolean onBackPressed(IActivity activity) { 
		Provider p = getSelectProvider();
		if (p != null && p.onBackPressed(activity)) return true;
		return super.onBackPressed(activity); 
	}
	
	@Override
	public ProviderBinder getBinder() { 
		Provider p = getSelectProvider();
		if (p != null) return p.getBinder(); 
		
		return null;
	}
	
	@Override
	public void setContentBackground(IActivity activity) {
		Provider p = getSelectProvider();
		if (p != null) p.setContentBackground(activity);
	}
	
	@Override
	public boolean isContentProgressEnabled() { 
		Provider p = getSelectProvider();
		if (p != null) return p.isContentProgressEnabled();
		return false; 
	}
	
	public ProviderListAdapter getPagerAdapter(IActivity activity) {
		return new ProviderListAdapter(this, activity, getActionItems(activity));
	}
	
	@Override
	public ProviderActionItem[] getActionItems(IActivity activity) { 
		synchronized (mProviders) {
			ProviderActionItem[] items = mActionItems; 
			if (items == null || activity != mActivity) {
				items = createActionItems(activity);
				mActionItems = items;
				mActivity = activity;
			}
			return items;
		}
	}
	
	public void resetActionItems() {
		if (LOG.isDebugEnabled()) LOG.debug("resetActionItems: provider=" + this);
		synchronized (mProviders) {
			mActionItems = null;
			mActivity = null;
			mSelectActionItem = null;
		}
	}
	
	protected void onActionTitleBinded(ProviderActionItem item, 
			View view, View subview, boolean dropdown) {
	}
	
	protected ProviderActionItem[] createActionItems(final IActivity activity) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("createActionItems: activity=" + activity 
					+ " provider=" + this);
		}
		
		final ProviderActionItem[] items = 
				new ProviderActionItem[mProviders.size()];
		
		ProviderActionItem defaultItem = null;
		Provider defaultProvider = null;
		
		for (int i=0; i < items.length; i++) { 
			final Provider item = mProviders.get(i);
			final ProviderActionItem actionItem = createActionItem(activity, item);
			items[i] = actionItem;
			
			if (item.isDefault() || defaultProvider == null) {
				defaultItem = actionItem;
				defaultProvider = item;
			}
		}
		
		if (mSelectProvider == null && defaultProvider != null) { 
			if (defaultProvider != null) defaultProvider.setDefault(true);
			mSelectActionItem = defaultItem;
			mSelectProvider = defaultProvider;
		}
		
		return items; 
	}
	
	protected ProviderActionItem createActionItem(IActivity activity, Provider item) {
		return new ProviderListActionItem(this, item);
	}
	
	@Override
	public IActionNavigationListener getActionNavigationListener(final IActivity activity) {
		return new IActionNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int itemPosition, long itemId) {
					return onActionItemSelected(activity, itemPosition, itemId);
				}
				@Override
				public void onActionItemsInited(IActionBar actionBar, ActionItem[] items) {
					ProviderList.this.onActionItemsInited(actionBar, items);
				}
			};
	}
	
	@Override
	public boolean onActionItemSelected(IActivity activity, int position, long id) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onNavigationItemSelected: position=" + position + " id=" + id);
		
		ProviderActionItem[] items = mActionItems;
		if (items != null && position >= 0 && position < items.length) {
			ProviderActionItem item = items[position];
			if (item != null && item.getIdentity() == id)
				return onActionItemSelected(activity, item);
		}
		
		return false;
	}
	
	public void onActionItemsInited(IActionBar actionBar, ActionItem[] items) {
		if (LOG.isDebugEnabled())
			LOG.debug("onActionItemsInited: actionBar=" + actionBar + " items=" + items);
	}
	
	public boolean onActionItemSelected(IActivity activity, ProviderActionItem item) { 
		if (LOG.isDebugEnabled())
			LOG.debug("onActionItemSelected: activity=" + activity + " item=" + item);
		
		if (item != null && item instanceof ProviderActionItem) { 
			if (!item.isEnabled()) return false;
			
			ProviderActionItem actionItem = (ProviderActionItem)item;
			Provider sourceItem = actionItem.getProvider();
			
			boolean changed = mSelectProvider != sourceItem;
			Provider old = mSelectProvider;
			mSelectProvider = sourceItem;
			mSelectActionItem = actionItem;
			
			if (changed) { 
				if (old != null) old.onDetach(activity);
				activity.setContentFragment(); 
			}
			
			actionItem.onItemSelected(activity);
			return true;
		}
		
		return false; 
	}
	
	@Override
	public CharSequence getLastUpdatedLabel(IActivity activity) { 
		final Provider selectItem = getSelectProvider();
		if (selectItem != null) return selectItem.getLastUpdatedLabel(activity);
		return null; 
	}
	
	@Override
	public int getHomeAsUpIndicatorRes() {
		int indicatorRes = 0;
		final Provider selectItem = getSelectProvider();
		if (selectItem != null) indicatorRes = selectItem.getHomeAsUpIndicatorRes();
		if (indicatorRes == 0) indicatorRes = super.getHomeAsUpIndicatorRes(); 
		return indicatorRes;
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		final Provider selectItem = getSelectProvider();
		if (selectItem != null) 
			selectItem.reloadOnThread(callback, type, reloadId);
	}
	
	@Override
	public int getActionNavigationMode() {
		return ActionBar.NAVIGATION_MODE_LIST;
	}
	
	@Override
	public CharSequence getActionTitle() {
		return null; //getTitle(activity);
	}
	
	@Override
	public CharSequence getActionSubTitle() {
		return null; //getSubTitle(activity);
	}
	
	@Override
	public void onDetach(IActivity activity) {
		super.onDetach(activity);
		Provider selectItem = getSelectProvider();
		if (selectItem != null) selectItem.onDetach(activity);
		mActivity = null;
		mActionItems = null;
	}
	
}
