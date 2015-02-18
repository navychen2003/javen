package org.javenstudio.cocoka.widget.adapter;

public abstract class AbstractExpandableObject implements IExpandableObject, IDataSetObject {

	public abstract Object get(Object key, int stat); 
	
	@Override 
	public Object get(Object key) { 
		return get(key, 0); 
	}
	
}
