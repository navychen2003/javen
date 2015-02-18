package org.javenstudio.cocoka.app;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;

public interface IActionBar {

	public static final int NAVIGATION_MODE_STANDARD = android.app.ActionBar.NAVIGATION_MODE_STANDARD;
	public static final int NAVIGATION_MODE_LIST = android.app.ActionBar.NAVIGATION_MODE_LIST;
	public static final int NAVIGATION_MODE_TABS = android.app.ActionBar.NAVIGATION_MODE_TABS;
	
	public static final int DISPLAY_USE_LOGO = android.app.ActionBar.DISPLAY_USE_LOGO;
	public static final int DISPLAY_SHOW_HOME = android.app.ActionBar.DISPLAY_SHOW_HOME;
	public static final int DISPLAY_HOME_AS_UP = android.app.ActionBar.DISPLAY_HOME_AS_UP;
	public static final int DISPLAY_SHOW_TITLE = android.app.ActionBar.DISPLAY_SHOW_TITLE;
	public static final int DISPLAY_SHOW_CUSTOM = android.app.ActionBar.DISPLAY_SHOW_CUSTOM;
	
    public interface OnNavigationListener {
        public boolean onNavigationItemSelected(int itemPosition, long itemId);
    }
	
    public interface OnMenuVisibilityListener {
        public void onMenuVisibilityChanged(boolean isVisible);
    }
    
    public interface ITabListener {
        public void onTabSelected(ITab tab, Object ft);
        public void onTabUnselected(ITab tab, Object ft);
        public void onTabReselected(ITab tab, Object ft);
    }
    
    public interface ITab {
    	public int getPosition();
    	public Drawable getIcon();
    	public CharSequence getText();
    	public View getCustomView();
    	public CharSequence getContentDescription();
    	public Object getTag();
    	
    	public ITab setIcon(Drawable icon);
    	public ITab setIcon(int resId);
    	public ITab setText(CharSequence text);
    	public ITab setText(int resId);
    	public ITab setCustomView(View view);
    	public ITab setCustomView(int layoutResId);
    	public ITab setContentDescription(int resId);
    	public ITab setContentDescription(CharSequence contentDesc);
    	
    	public ITab setTag(Object obj);
    	public ITab setTabListener(ITabListener listener);
    	public void select();
    }
    
	public Context getThemedContext();
	public void setNavigationMode(int mode);
	public int getNavigationMode();
	
	public CharSequence getTitle();
	public void setTitle(int resId);
	public void setTitle(CharSequence title);
	
	public CharSequence getSubtitle();
	public void setSubtitle(CharSequence subtitle);
	public void setSubtitle(int resId);
	
	public int getDisplayOptions();
	public void setDisplayOptions(int options);
	public void setDisplayOptions(int options, int mask);
	
	public void setHomeButtonEnabled(boolean enabled);
	public void setDisplayShowHomeEnabled(boolean showHome);
	public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp);
	public void setDisplayShowTitleEnabled(boolean showTitle);
	public void setDisplayUseLogoEnabled(boolean useLogo);
	public void setDisplayShowCustomEnabled(boolean showCustom);
	
	public void setBackgroundDrawable(Drawable d);
	public void setStackedBackgroundDrawable(Drawable d);
	public void setLogo(Drawable logo);
	public void setLogo(int resId);
	public void setIcon(Drawable icon);
	public void setIcon(int resId);
	
	public void setHomeAsUpIndicator(Drawable indicator);
	public void setHomeAsUpIndicator(int resId);
	
	public void setCustomView(View view, ViewGroup.LayoutParams layoutParams);
	public void setCustomView(View view);
	public void setCustomView(int resId);
	public View getCustomView();
	
	public void setListNavigationCallbacks(SpinnerAdapter adapter,
			OnNavigationListener callback);
	
	public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener);
	public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener);
	
	public void setSelectedNavigationItem(int position);
	public int getSelectedNavigationIndex();
	public int getNavigationItemCount();
	
	public ITab newTab();
	public void addTab(ITab tab);
	public void addTab(ITab tab, int position);
	public void addTab(ITab tab, int position, boolean setSelected);
	public void removeTab(ITab tab);
	public void removeTabAt(int position);
	public void removeAllTabs();
	public void selectTab(ITab tab);
	public ITab getSelectedTab();
	
	public IActionMode startActionMode(IActionMode.Callback callback);
	
	public int getHeight();
	public boolean hasEmbeddedTabs();
	
	public void show();
	public void hide();
	
}
