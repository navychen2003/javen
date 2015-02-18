package org.javenstudio.provider.account.space;

import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.cocoka.android.ResourceHelper;

public abstract class SpaceItem extends DataBinderItem {

	private final long mIdentity = ResourceHelper.getIdentity();
	private final SpaceProvider mProvider;
	
	public SpaceItem(SpaceProvider p) { 
		if (p == null) throw new NullPointerException();
		mProvider = p;
	}
	
	public SpaceProvider getProvider() { return mProvider; }
	
	public abstract int getViewRes();
	public abstract void bindView(View view);
	
	public void onUpdateViewOnVisible(boolean restartSlide) {}
	public void onViewBinded(IActivity activity) {}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + mIdentity + "{}";
	}
	
}
