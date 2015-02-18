package org.javenstudio.cocoka.app;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public class ActionItem implements IActionBar.ITabListener {
	private static final Logger LOG = Logger.getLogger(ActionItem.class);
	
	public static interface OnClickListener { 
		public void onActionClick();
	}
	
	private final long mIdentity = ResourceHelper.getIdentity();
	private final String mName;
	
	private int mIconRes = 0;
	private Drawable mIcon = null;
	private Drawable mDropdownIcon = null;
	
	private CharSequence mTitle = null;
	private CharSequence mSubTitle = null;
	private CharSequence mDropdownTitle = null;
	
	private ActionItem mParentItem = null;
	private ActionItem[] mSubItems = null;
	private OnClickListener mClickListener = null;
	private Runnable mCollapser = null;
	private Runnable mExpender = null;
	
	public ActionItem(String name, int iconRes) { 
		this(name, iconRes, null, null);
	}
	
	public ActionItem(String name, int iconRes, Drawable icon, 
			OnClickListener l) {
		mName = name; 
		mIconRes = iconRes;
		mIcon = icon;
		mClickListener = l;
	}
	
	public final String getName() { return mName; }
	public final long getIdentity() { return mIdentity; }
	
	public boolean isEnabled() { return true; }
	
	public int getIconRes() { return mIconRes; }
	public void setIconRes(int iconRes) { mIconRes = iconRes; }
	
	public Drawable getIcon() { return mIcon; }
	public void setIcon(Drawable d) { mIcon = d; }
	
	public Drawable getDropdownIcon() { return mDropdownIcon != null ? mDropdownIcon : mIcon; }
	public void setDropdownIcon(Drawable d) { mDropdownIcon = d; }
	
	public int getTitleColorRes() { return 0; }
	public int getTitleColorStateListRes() { return 0; }
	public int getTitleSizeRes() { return 0; }
	
	public int getBackgroundRes() { return 0; }
	public Drawable getBackgroundDrawable() { return null; }
	
	public CharSequence getTitle() { return mTitle != null ? mTitle : mName; }
	public void setTitle(CharSequence title) { mTitle = title; }
	
	public CharSequence getSubTitle() { return mSubTitle; }
	public void setSubTitle(CharSequence title) { mSubTitle = title; }
	
	public void setDropdownTitle(CharSequence title) { mDropdownTitle = title; }
	public CharSequence getDropdownTitle() { return mDropdownTitle; }
	
	public CharSequence getDropdownText() { 
		CharSequence title = getDropdownTitle();
		if (title != null && title.length() > 0) 
			return title;
		
		title = getTitle();
		
		CharSequence subtext = getSubTitle();
		if (subtext != null && subtext.length() > 0) 
			title = subtext;
		
		return title;
	}
	
	public boolean showIcon(boolean dropdown) { 
		return dropdown; 
	}
	
	public void onTitleBinded(View view, View subview, 
			boolean dropdown) {
	}
	
	public ActionItem getParentItem() { return mParentItem; }
	public ActionItem[] getSubItems() { return mSubItems; }
	
	public void setSubItems(ActionItem[] items) { 
		mSubItems = items; 
		
		for (int i=0; items != null && i < items.length; i++) { 
			ActionItem item = items[i];
			if (item != null) 
				item.mParentItem = this;
		}
	}
	
	public OnClickListener getOnClickListener() { return mClickListener; }
	public void setOnClickListener(OnClickListener l) { mClickListener = l; }
	
	public boolean onSubItemExpand(Activity activity) { return false; }
	
	void setCollapser(Runnable r) { mCollapser = r; }
	void setExpender(Runnable r) { mExpender = r; }
	
	public final void collapseSubItems() { 
		Runnable r = mCollapser;
		if (r != null) r.run();
	}
	
	public final void expendSubItems() { 
		Runnable r = mExpender;
		if (r != null) r.run();
	}
	
	public View getView(LayoutInflater inflater, View convertView, 
			boolean dropdown) {
		return null;
	}
	
	public void onViewBinded(View view, boolean dropdown) {}
	
	public void initTab(IActionBar.ITab tab) {
		tab.setTag(this).setText(this.getTitle()).setTabListener(this);
	}
	
	@Override
	public void onTabSelected(IActionBar.ITab tab, Object ft) {
		if (LOG.isDebugEnabled())
			LOG.debug("onTabSelected: tab=" + tab + " ft=" + ft);
	}

	@Override
	public void onTabUnselected(IActionBar.ITab tab, Object ft) {
		if (LOG.isDebugEnabled())
			LOG.debug("onTabUnselected: tab=" + tab + " ft=" + ft);
	}

	@Override
	public void onTabReselected(IActionBar.ITab tab, Object ft) {
		if (LOG.isDebugEnabled())
			LOG.debug("onTabReselected: tab=" + tab + " ft=" + ft);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "-" + mIdentity + "{name=" + mName + "}";
	}
	
}
