package org.javenstudio.cocoka.app;

final class AndroidMenu implements IMenu {

	protected final android.view.Menu mMenu;
	
	public AndroidMenu(android.view.Menu menu) { 
		mMenu = menu;
	}

	@Override
	public IMenuItem findItem(int id) {
		android.view.MenuItem item = mMenu.findItem(id);
		if (item != null) 
			return new AndroidMenuItem(item);
		
		return null;
	}

	@Override
	public IMenuItem add(int titleRes) {
		android.view.MenuItem item = mMenu.add(titleRes);
		if (item != null) 
			return new AndroidMenuItem(item);
		
		return null;
	}

	@Override
	public IMenuItem add(CharSequence title) {
		android.view.MenuItem item = mMenu.add(title);
		if (item != null) 
			return new AndroidMenuItem(item);
		
		return null;
	}

	@Override
	public IMenuItem add(int groupId, int itemId, int order, CharSequence title) {
		android.view.MenuItem item = mMenu.add(groupId, itemId, order, title);
		if (item != null) 
			return new AndroidMenuItem(item);
		
		return null;
	}

	@Override
	public IMenuItem add(int groupId, int itemId, int order, int titleRes) {
		android.view.MenuItem item = mMenu.add(groupId, itemId, order, titleRes);
		if (item != null) 
			return new AndroidMenuItem(item);
		
		return null;
	}

	@Override
	public void removeItem(int id) {
		mMenu.removeItem(id);
	}

	@Override
	public void removeGroup(int groupId) {
		mMenu.removeGroup(groupId);
	}
	
}
