package org.javenstudio.android.information.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.SlidingActivity;
import org.javenstudio.android.information.Information;
import org.javenstudio.android.information.InformationListController;
import org.javenstudio.android.information.InformationListModel;
import org.javenstudio.android.information.InformationNavItem;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.widget.model.Model;
import org.javenstudio.cocoka.widget.model.NavigationCallback;
import org.javenstudio.cocoka.widget.model.NavigationCallbackBase;
import org.javenstudio.cocoka.widget.model.NavigationGroup;
import org.javenstudio.cocoka.widget.model.NavigationGroupMenu;
import org.javenstudio.cocoka.widget.model.NavigationInfo;
import org.javenstudio.cocoka.widget.model.NavigationItem;

public abstract class InformationListActivity extends SlidingActivity {

	protected final NavigationGroupMenu mGroupMenu = new NavigationGroupMenuImpl();
	protected final NavigationCallback mCallback = new NavigationCallbackImpl();
	
	protected NavigationActionItem[] mNavigationItems = null;
	protected InformationNavItem mCurrentItem = null;
	
	public InformationListController getController() { 
		return InformationListController.getInstance(); 
	}
	
	@Override
	public NavigationActionItem[] getNavigationItems() { 
		return mNavigationItems;
	}
	
	public InformationNavItem getCurrentItem() { 
		return mCurrentItem;
	}
	
	@Override
	protected void onInvokeByModel(int action, Object params) { 
		switch (action) { 
		case Model.ACTION_ONLOADERSTART:
		case InformationListModel.ACTION_ONFETCHSTART: 
			postShowProgress(false);
			break; 
		case Model.ACTION_ONLOADERSTOP:
		case InformationListModel.ACTION_ONFETCHSTOP: 
			postHideProgress(false);
			postRefreshContent();
			break; 
		default:
			super.onInvokeByModel(action, params);
			break;
		}
	}
	
	@Override
	protected void doOnCreate(Bundle savedInstanceState) { 
		getController().initialize(getCallback()); 
        initNavigationItems();
        
        super.doOnCreate(savedInstanceState);
        setSlidingActionBarEnabled(true);
	}
	
	@Override
	protected ActionHelper createActionHelper() { 
		return new ActionHelperImpl(this, getSupportActionBar());
	}
	
	@Override
    public void onStart() {
        super.onStart();
        initContent();
	}
	
	@Override
	protected boolean onActionRefresh() { 
		refreshContent(true);
		return true;
	}
	
	@Override
	public void setContentFragment() { 
		setContentFragment(new InformationListFragment());
	}
	
	private void setSelectedActionItem(int position) { 
		mCurrentItem = null;
		
		ActionHelper helper = getActionHelper();
		if (helper != null) { 
			NavigationActionItem item = (NavigationActionItem)
					helper.getActionItem(position);
			
			if (item != null) {
				InformationNavItem navItem = (InformationNavItem)item.getNavigationItem();
				if (navItem != null) { 
					NavigationGroup group = navItem.getParent();
					if (group != null) 
						group.setChildSelected(navItem);
					
					mCurrentItem = navItem;
				}
			}
		}
	}
	
	private class ActionHelperImpl extends ActionHelper { 
		public ActionHelperImpl(Activity activity, IActionBar actionBar) { 
			super(activity, actionBar);
		}
		
		@Override
		public boolean onNavigationItemSelected(int position, long id) {
			if (mNavigationItems != null && mStarted) { 
				setSelectedActionItem(position);
				setContentFragment();
			}
			return true;
		}
	}
	
	private class NavigationCallbackImpl extends NavigationCallbackBase {
		@Override
		public Activity getActivity() {
			return InformationListActivity.this;
		}
		
		@Override
		public NavigationGroupMenu getGroupMenu(NavigationGroup group) {
			return mGroupMenu;
		}
		
		@Override
		public void showGroupMenu(NavigationGroupMenu menu) {
			if (mNavigationItems != null) { 
				NavigationGroupMenuImpl impl = (NavigationGroupMenuImpl)menu;
				ActionHelper helper = getActionHelper();
				if (helper != null) { 
					helper.setActionItems(impl.mMenuItems.toArray(new ActionItem[0]), null);
					helper.setActionTitle(null, null);
				}
				showContent();
			}
		}
	}
	
	private class NavigationGroupMenuImpl extends NavigationGroupMenu {
		private final List<NavigationActionItem> mMenuItems = 
				new ArrayList<NavigationActionItem>();
		
		@Override
		public void addMenuItem(final NavigationItem item) { 
			final NavigationInfo info = item.getInfo();
			final int iconRes = Information.getItemIconRes(info);
			final Drawable icon = Information.getItemIcon(info, iconRes);
			
			final String name = info.getName();
			final String title = info.getTitle();
			final String subtitle = info.getSubTitle();
			final String dropdownTitle = info.getDropdownTitle();
			
			NavigationActionItem actionItem = new NavigationActionItem(name, 
					iconRes, icon, new NavigationActionItem.OnClickListener() {
						@Override
						public void onActionClick() {
							getController().onGroupItemClicked(mCallback, 
									NavigationGroupMenuImpl.this, item);
						}
					}, item);
			
			actionItem.setTitle(title);
			actionItem.setSubTitle(subtitle);
			actionItem.setDropdownTitle(dropdownTitle);
			
			mMenuItems.add(actionItem);
		}
		
		@Override
		public void clear() { 
			mMenuItems.clear();
		}
	}
	
	private void initNavigationItems() { 
		final InformationListController controller = getController();
		final NavigationItem[] groups = controller.getGroupItems(); 
		
		if (groups == null || groups.length == 0) { 
			mNavigationItems = new NavigationActionItem[0];
			return;
		}
		
		NavigationActionItem[] menuItems = new NavigationActionItem[groups.length];
		
		for (int i=0; i < groups.length; i++) { 
			final NavigationItem group = groups[i];
			
			final NavigationInfo info = group.getInfo();
			final int iconRes = Information.getItemIconRes(info);
			final Drawable icon = Information.getItemIcon(info, iconRes);
			
			menuItems[i] = new NavigationActionItem(info.getName(), 
					iconRes, icon, new NavigationActionItem.OnClickListener() {
						@Override
						public void onActionClick() {
							controller.onItemClicked(mCallback, group);
						}
					}, group);
		}
		
		mNavigationItems = menuItems;
	}
	
	private void initContent() { 
		if (mCurrentItem != null) return;
		
		final InformationListController controller = getController();
		final NavigationItem selectItem = controller.getSelectedItem();
		final ActionHelper helper = getActionHelper();
		
		if (selectItem != null) { 
			NavigationItem selectGroup = selectItem.getParent();
			if (selectGroup != null) { 
				controller.onItemClicked(mCallback, selectGroup);
				
				for (int i=0; helper != null && i < helper.getActionCount(); i++) { 
					NavigationActionItem item = (NavigationActionItem)helper.getActionItem(i);
					
					if (item != null) { 
						NavigationItem navItem = item.getNavigationItem();
						if (navItem == selectItem) { 
							helper.setSelectedItem(i);
							break;
						}
					}
				}
			}
		}
        
        if (helper != null && helper.getActionAdapter() == null) 
        	initWelcomeContent();
	}
	
	@Override
	public void refreshContent(boolean force) { 
		getController().setCurrentItem(mCurrentItem);
		getController().refreshContent(mCallback, force);
	}
	
	private void postRefreshContent() { 
		final InformationListController controller = getController();
		
		InformationNavItem item = mCurrentItem;
		if (item == null) 
			item = (InformationNavItem)controller.getSelectedItem();
		
		controller.postRefresh(item);
	}
	
	protected void initWelcomeContent() {}

}
