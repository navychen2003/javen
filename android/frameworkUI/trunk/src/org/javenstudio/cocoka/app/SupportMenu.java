package org.javenstudio.cocoka.app;

public interface SupportMenu {

	public SupportMenuItem findItem(int id);
	
	public SupportMenuItem add(int titleRes);
	public SupportMenuItem add(CharSequence title);
	
	public SupportMenuItem add(int groupId, int itemId, int order, CharSequence title);
	public SupportMenuItem add(int groupId, int itemId, int order, int titleRes);
	
	public void removeItem(int id);
	public void removeGroup(int groupId);
	
}
