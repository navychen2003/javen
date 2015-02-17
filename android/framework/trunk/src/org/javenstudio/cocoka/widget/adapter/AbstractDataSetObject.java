package org.javenstudio.cocoka.widget.adapter;

public abstract class AbstractDataSetObject implements IDataSetObject, IExpandableObject {

	public abstract Object get(Object key); 
	
	@Override 
	public Object get(Object key, int stat) { 
		return get(key); 
	}
	
}
