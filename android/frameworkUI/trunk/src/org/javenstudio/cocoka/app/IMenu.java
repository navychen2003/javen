package org.javenstudio.cocoka.app;

public interface IMenu {

	public IMenuItem findItem(int id);
	
	public IMenuItem add(int titleRes);
	public IMenuItem add(CharSequence title);
	
	public IMenuItem add(int groupId, int itemId, int order, CharSequence title);
	public IMenuItem add(int groupId, int itemId, int order, int titleRes);
	
	public void removeItem(int id);
	public void removeGroup(int groupId);
	
}
