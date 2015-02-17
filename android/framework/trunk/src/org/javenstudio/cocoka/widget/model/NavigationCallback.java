package org.javenstudio.cocoka.widget.model;

import android.app.Activity;

public interface NavigationCallback {

	public Activity getActivity();
	public NavigationGroupMenu getGroupMenu(NavigationGroup group);
	
	public void hideGroupMenu(NavigationGroupMenu menu);
	public void showGroupMenu(NavigationGroupMenu menu);
	
	public void initHeaderTitle(NavigationItem item); 
	public void initContentView(NavigationItem item); 
	
	public void resetNavigationBar();
	
}
