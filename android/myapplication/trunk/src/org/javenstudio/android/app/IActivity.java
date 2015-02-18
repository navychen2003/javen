package org.javenstudio.android.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.NetworkHelper;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.cocoka.app.IMenu;

public interface IActivity extends NetworkHelper.NetworkListener, 
		ActionHelper.HelperApp, ActivityHelper.HelperApp {

	public DataApp getDataApp();
	public Activity getActivity();
	public Context getApplicationContext();
	public void overridePendingTransition(int enterAnim, int exitAnim);
	public void onBackPressed();
	
	public Object getSystemService(String name);
	public ComponentName getComponentName();
	public Resources getResources();
	
	public View getContentView();
	public View getContentAboveView();
	public View getContentBehindView();
	
	public IMenuOperation getMenuOperation();
	public IMenu getActivityMenu();
	public ActivityHelper getActivityHelper();
	public ActionHelper getActionHelper();
	public ModelCallback getCallback();
	
	public void refreshContent(boolean force);
	public CharSequence getLastUpdatedLabel();
	
	public void setContentFragment();
	public void setTitle(CharSequence title);
	public void setShareIntent(Intent shareIntent);
	public void setShareIntent();
	
	public void setActionModeBackgroundResource(int resid);
	public void setActionModeBackgroundColor(int color);
	public void setActionModeBackground(Drawable background);
	
	public void setActionModeCloseButtonBackgroundResource(int resid);
	public void setActionModeCloseButtonBackgroundColor(int color);
	public void setActionModeCloseButtonBackground(Drawable background);
	
	public void setActionBarBackgroundResource(int resid);
	public void setActionBarBackgroundColor(int color);
	public void setActionBarBackground(Drawable background);
	
	public void setContentBackgroundResource(int resid);
	public void setContentBackgroundColor(int color);
	public void setContentBackground(Drawable background);
	
	public void setActivityBackgroundResource(int resid);
	public void setActivityBackgroundColor(int color);
	public void setActivityBackground(Drawable background);
	
	public void postShowProgress(boolean force);
	public void postHideProgress(boolean force);
	public boolean isContentProgressEnabled();
	
	public int getScreenWidth();
	public int getScreenHeight();
	public boolean isDestroyed();
	
}
