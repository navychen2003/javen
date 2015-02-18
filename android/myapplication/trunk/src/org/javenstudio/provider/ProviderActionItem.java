package org.javenstudio.provider;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.cocoka.app.ActionItem;

public class ProviderActionItem extends ActionItem {

	private final Provider mItem;
	
	public ProviderActionItem(String tag, int iconRes, Drawable icon, 
			OnClickListener l, Provider item) {
		super(tag, iconRes, icon, l);
		mItem = item;
	}
	
	public Provider getProvider() { return mItem; }
	
	public boolean handleNavigationSubClick(IActivity activity) { 
		if (mItem != null) return mItem.handleClick(activity, this);
		return false; 
	}
	
	public void onItemSelected(IActivity activity) {}
	
}
