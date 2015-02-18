package org.javenstudio.cocoka.app;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

import org.javenstudio.common.util.Logger;

public class BaseActionHelper implements IActionNavigationListener {
	private static final Logger LOG = Logger.getLogger(BaseActionHelper.class);

	private final IActionBar mActionBar;
	private SpinnerAdapter mAdapter = null;
	private ActionItem[] mItems = null;
	
	public BaseActionHelper(IActionBar actionBar) { 
		mActionBar = actionBar;
		initActionBar();
	}
	
	public final IActionBar getActionBar() { return mActionBar; }
	public final SpinnerAdapter getActionAdapter() { return mAdapter; }
	public final ActionItem[] getActionItems() { return mItems; }
	
	public final Resources getResources() { 
		return mActionBar.getThemedContext().getResources();
	}
	
	protected void initActionBar() {}
	
	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		return true;
	}
	
	@Override
	public void onActionItemsInited(IActionBar actionBar, ActionItem[] items) {
	}
	
	public final void setActionTitle(CharSequence title, CharSequence subtitle) { 
		final IActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(title != null);
		actionBar.setTitle(title);
		actionBar.setSubtitle(subtitle);
	}
	
	public final void setCustomView(View view) { 
		setCustomView(view, null);
	}
	
	public final void setCustomView(View view, ViewGroup.LayoutParams layoutParams) { 
		final IActionBar actionBar = getActionBar();
		actionBar.setDisplayShowCustomEnabled(view != null);
		
		if (layoutParams != null)
			actionBar.setCustomView(view, layoutParams);
		else
			actionBar.setCustomView(view);
	}
	
	public final View getCustomView() { 
		final IActionBar actionBar = getActionBar();
		return actionBar.getCustomView();
	}
	
	public final void setActionItems(ActionItem[] items, 
			IActionNavigationListener callback) {
		setActionItems(items, ActionBar.NAVIGATION_MODE_LIST, null, callback);
	}
	
	public final void setActionItems(ActionItem[] items, int mode, 
			IActionNavigationListener callback) {
		setActionItems(items, mode, null, callback);
	}
	
	public final void setActionItems(ActionItem[] items, int mode, 
			IActionAdapterFactory factory, IActionNavigationListener callback) {
		final IActionBar actionBar = getActionBar();
		
		if (items != null && items.length > 0) {
			if (mode == ActionBar.NAVIGATION_MODE_LIST) {
				if (LOG.isDebugEnabled())
					LOG.debug("setActionItems: list mode: items=" + items + " mode=" + mode);
				
				mAdapter = (factory != null) ? 
					factory.createActionAdapter(actionBar.getThemedContext(), items) : 
					createActionAdapter(actionBar.getThemedContext(), items);
				mItems = items;
				
				// Set up the dropdown list navigation in the action bar.
				if (callback == null) callback = this;
				actionBar.setListNavigationCallbacks(mAdapter, callback);
				actionBar.setNavigationMode(mode);
				
				if (callback != null) callback.onActionItemsInited(actionBar, items);
				return;
				
			} else if (mode == ActionBar.NAVIGATION_MODE_TABS) {
				if (LOG.isDebugEnabled())
					LOG.debug("setActionItems: tabs mode: items=" + items + " mode=" + mode);
				
				mItems = items;
				actionBar.removeAllTabs();
				
				for (final ActionItem item : items) {
					IActionBar.ITab tab = actionBar.newTab();
					item.initTab(tab);
					actionBar.addTab(tab);
				}
				
				actionBar.setNavigationMode(mode);
				return;
			}
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("setActionItems: standard mode: items=" + items + " mode=" + mode);
		
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}
	
	protected SpinnerAdapter createActionAdapter(Context context, ActionItem[] items) { 
		return new ActionAdapter(context, items);
	}
	
	public void setSelectedItem(int selectIndex) { 
		final IActionBar actionBar = getActionBar();
		if (LOG.isDebugEnabled()) {
			LOG.debug("setSelectedItem: selectIndex=" + selectIndex 
					+ " mode=" + actionBar.getNavigationMode() 
					+ " hasEmbedded=" + actionBar.hasEmbeddedTabs());
		}
		
		switch (actionBar.getNavigationMode()) { 
		case ActionBar.NAVIGATION_MODE_LIST:
		case ActionBar.NAVIGATION_MODE_TABS:
			if (selectIndex >= 0 && selectIndex < getActionCount())
				actionBar.setSelectedNavigationItem(selectIndex);
			break;
		}
	}
	
	public int getSelectedItem() {
		final IActionBar actionBar = getActionBar();
		return actionBar.getSelectedNavigationIndex();
	}
	
	public int getActionCount() { 
		ActionItem[] items = mItems;
		return items != null ? items.length : 0;
	}
	
	public ActionItem getActionItem(int position) { 
		ActionItem[] items = mItems;
		if (items != null && position >= 0 && position < items.length) 
			return items[position];
		
		return null;
	}
	
	public void setHomeIcon(int iconRes) {
		final IActionBar actionBar = getActionBar();
		if (iconRes != 0 && actionBar != null) {
			actionBar.setIcon(iconRes);
			actionBar.setDisplayShowHomeEnabled(true);
		}
	}
	
	public void setHomeAsUpIndicator(int resId) {
		final IActionBar actionBar = getActionBar();
		if (actionBar != null) {
			if (resId != 0) {
				actionBar.setHomeAsUpIndicator(resId);
				actionBar.setDisplayHomeAsUpEnabled(true);
			} else {
				actionBar.setDisplayHomeAsUpEnabled(false);
			}
		}
	}
	
}
