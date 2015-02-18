package org.javenstudio.android.information.activity;

import android.graphics.drawable.Drawable;

import org.javenstudio.cocoka.app.ActionItem;
import org.javenstudio.cocoka.widget.model.NavigationItem;

public final class NavigationActionItem extends ActionItem {

	private final NavigationItem mItem;
	
	public NavigationActionItem(String tag, int iconRes, Drawable icon, 
			OnClickListener l, NavigationItem item) {
		super(tag, iconRes, icon, l);
		mItem = item;
	}
	
	public NavigationItem getNavigationItem() { return mItem; }
	
}
