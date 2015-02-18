package org.javenstudio.cocoka.app;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface IMenuItem {

	public static final int SHOW_AS_ACTION_NEVER = android.view.MenuItem.SHOW_AS_ACTION_NEVER;
    public static final int SHOW_AS_ACTION_IF_ROOM = android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;
    public static final int SHOW_AS_ACTION_ALWAYS = android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
    public static final int SHOW_AS_ACTION_WITH_TEXT = android.view.MenuItem.SHOW_AS_ACTION_WITH_TEXT;
    public static final int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = android.view.MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW;
	
	public int getItemId();
	public int getGroupId();
	public int getOrder();
	
	public void setTitle(CharSequence title);
	public void setTitle(int title);
	public CharSequence getTitle();
	
	public void setActionView(View view);
	public void setActionView(int resId);
	public View getActionView();
	
	public void setIcon(Drawable icon);
	public void setIcon(int iconRes);
	public Drawable getIcon();
	
	public void setVisible(boolean visible);
	public boolean isVisible();
	
	public void setShowAsAction(int actionEnum);
	
	public IActionProvider getActionProvider();
	
}
