package org.javenstudio.cocoka.widget.model;

public abstract class NavigationGroupMenu {

	public boolean isShowing() { return false; }
	public boolean isMenuItemInited() { return false; }
	
	public void addMenuItem(NavigationItem item) {}
	public void clear() {}
	
}
