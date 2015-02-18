package org.javenstudio.provider.task;

import android.app.ActionBar;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderActionItem;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.ProviderList;
import org.javenstudio.provider.ProviderListActionItem;

public class TaskListProvider extends ProviderList {

	private final AccountApp mApp;
	private final AccountUser mAccount;
	private final TaskListBinder mBinder;
	
	public TaskListProvider(AccountApp app, AccountUser account, 
			int titleRes, int iconRes, int indicatorRes) { 
		super(ResourceHelper.getResources().getString(titleRes), iconRes);
		if (app == null || account == null) throw new NullPointerException();
		mApp = app;
		mAccount = account;
		mBinder = createTaskListBinder();
		//setOptionsMenu(new AccountOptionsMenu(app, account));
		setHomeAsUpIndicator(indicatorRes);
	}

	public AccountUser getAccountUser() { return mAccount; }
	public AccountApp getAccountApp() { return mApp; }
	
	protected TaskListBinder createTaskListBinder() {
		return new TaskListBinder(this);
	}
	
	@Override
	public final TaskListBinder getBinder() { 
		return mBinder;
	}
	
	@Override
	public Provider getSelectProvider() {
		return getBinder().getCurrentProvider();
	}
	
	@Override
	public boolean onActionItemSelected(IActivity activity, ProviderActionItem item) { 
		TaskListBinder binder = getBinder();
		if (binder != null) {
			if (item != null && item instanceof ProviderActionItem) {
				ProviderActionItem actionItem = (ProviderActionItem)item;
				return binder.onItemSelected(activity, actionItem);
			}
			return false;
		}
		return super.onActionItemSelected(activity, item);
	}
	
	//@Override
	//public void onDataSetChanged(IActivity activity, Object data) {
	//	super.onDataSetChanged(activity, data);
	//	TaskListBinder binder = getBinder();
	//	if (binder != null) binder.onDataSetChanged(activity, data);
	//}
	
	@Override
	public void onSaveFragmentState(Bundle savedInstanceState) {
		super.onSaveFragmentState(savedInstanceState);
		TaskListBinder binder = getBinder();
		if (binder != null) binder.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onSaveActivityState(Bundle savedInstanceState) {
		super.onSaveActivityState(savedInstanceState);
		TaskListBinder binder = getBinder();
		if (binder != null) binder.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreActivityState(Bundle savedInstanceState) {
		super.onRestoreActivityState(savedInstanceState);
		TaskListBinder binder = getBinder();
		if (binder != null) binder.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		TaskListBinder binder = getBinder();
		if (binder != null) {
			binder.reloadOnThread(callback, type, reloadId);
			return;
		}
		super.reloadOnThread(callback, type, reloadId);
	}
	
	@Override
	protected ProviderActionItem createActionItem(final IActivity activity, 
			final Provider item) {
		return new ProviderListActionItem(this, item) { 
				@Override
				public void onTabSelected(IActionBar.ITab tab, Object ft) {
					super.onTabSelected(tab, ft);
					onActionItemSelected(activity, this);
					getProvider().onPagerItemSelected(activity);
				}
			};
	}
	
	@Override
	public int getActionNavigationMode() {
		return ActionBar.NAVIGATION_MODE_TABS;
	}
	
	@Override
	public CharSequence getActionTitle() {
		return getTitle();
	}
	
	@Override
	public CharSequence getActionSubTitle() {
		return getSubTitle();
	}
	
	@Override
	public void setOrientation(IActivity activity) {
		activity.getActivityHelper().lockOrientation(Configuration.ORIENTATION_PORTRAIT);
	}
	
	@Override
	public boolean isUnlockOrientationDisabled() { 
		return true; 
	}
	
	private int mPageState = 0;
	
	public void onPageScrollStateChanged(int state) { 
		mPageState = state;
	}
	
	@Override
	public boolean onFlingToRight(IActivity activity) { 
		if (mPageState != ViewPager.SCROLL_STATE_IDLE) return false;
		return super.onFlingToRight(activity);
	}
	
}
