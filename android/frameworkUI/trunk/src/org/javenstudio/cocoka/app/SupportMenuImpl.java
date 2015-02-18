package org.javenstudio.cocoka.app;

final class SupportMenuImpl implements IMenu {

	protected final SupportMenu mMenu;
	
	public SupportMenuImpl(SupportMenu menu) { 
		mMenu = menu;
	}

	@Override
	public IMenuItem findItem(int id) {
		SupportMenuItem item = mMenu.findItem(id);
		if (item != null) 
			return new SupportMenuItemImpl(item);
		
		return null;
	}

	@Override
	public IMenuItem add(int titleRes) {
		SupportMenuItem item = mMenu.add(titleRes);
		if (item != null) 
			return new SupportMenuItemImpl(item);
		
		return null;
	}

	@Override
	public IMenuItem add(CharSequence title) {
		SupportMenuItem item = mMenu.add(title);
		if (item != null) 
			return new SupportMenuItemImpl(item);
		
		return null;
	}

	@Override
	public IMenuItem add(int groupId, int itemId, int order, CharSequence title) {
		SupportMenuItem item = mMenu.add(groupId, itemId, order, title);
		if (item != null) 
			return new SupportMenuItemImpl(item);
		
		return null;
	}

	@Override
	public IMenuItem add(int groupId, int itemId, int order, int titleRes) {
		SupportMenuItem item = mMenu.add(groupId, itemId, order, titleRes);
		if (item != null) 
			return new SupportMenuItemImpl(item);
		
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
