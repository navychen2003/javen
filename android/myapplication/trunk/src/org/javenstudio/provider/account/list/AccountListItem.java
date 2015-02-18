package org.javenstudio.provider.account.list;

import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.cocoka.android.ResourceHelper;

public abstract class AccountListItem extends DataBinderItem {

	protected final long mIdentity = ResourceHelper.getIdentity();
	
	public abstract int getViewRes();
	public abstract void bindViews(IActivity activity, View view);
	
	public void onUpdateViewsOnVisible(boolean restartSlide) {}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + mIdentity + "{}";
	}
	
}
