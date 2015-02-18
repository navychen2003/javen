package org.javenstudio.provider.account.space;

import android.app.ActionBar;
import android.os.Bundle;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.cocoka.app.IActionBar;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderActionItem;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.ProviderList;
import org.javenstudio.provider.ProviderListActionItem;

public abstract class SpacesProvider extends ProviderList {

	private final SpacesBinder mBinder;
	
	public SpacesProvider(String name, int iconRes) { 
		super(name, iconRes);
		mBinder = createSpacesBinder();
	}

	protected SpacesBinder createSpacesBinder() {
		return new SpacesBinder(this);
	}
	
	public final SpacesBinder getSpacesBinder() {
		return mBinder;
	}
	
	@Override
	public ProviderBinder getBinder() { 
		SpacesBinder binder = getSpacesBinder();
		if (binder != null) return binder;
		return super.getBinder();
	}
	
	@Override
	public boolean onActionItemSelected(IActivity activity, ProviderActionItem item) { 
		SpacesBinder binder = getSpacesBinder();
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
	//	SpacesBinder binder = getSpacesBinder();
	//	if (binder != null) binder.onDataSetChanged(activity, data);
	//}
	
	@Override
	public void onSaveFragmentState(Bundle savedInstanceState) {
		super.onSaveFragmentState(savedInstanceState);
		SpacesBinder binder = getSpacesBinder();
		if (binder != null) binder.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onSaveActivityState(Bundle savedInstanceState) {
		super.onSaveActivityState(savedInstanceState);
		SpacesBinder binder = getSpacesBinder();
		if (binder != null) binder.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	public void onRestoreActivityState(Bundle savedInstanceState) {
		super.onRestoreActivityState(savedInstanceState);
		SpacesBinder binder = getSpacesBinder();
		if (binder != null) binder.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		SpacesBinder binder = getSpacesBinder();
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
	
}
