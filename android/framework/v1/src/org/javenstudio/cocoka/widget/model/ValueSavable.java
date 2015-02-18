package org.javenstudio.cocoka.widget.model;

public interface ValueSavable {

	public void removeValue(String key); 
	public void putValue(String key, Object value); 
	public Object getValue(String key); 
	
}
