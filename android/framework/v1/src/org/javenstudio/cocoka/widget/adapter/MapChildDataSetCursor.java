package org.javenstudio.cocoka.widget.adapter;

import java.util.HashMap;
import java.util.Map;

public abstract class MapChildDataSetCursor<T extends IExpandableObject, E extends IExpandableObject> 
			implements IChildDataSetCursor<T, E> {

	private final Map<Integer, AbstractChildDataSet<T, E>> mChildMap; 
	private final int mGroupPosition; 
	
	public MapChildDataSetCursor(int groupPosition) { 
		mChildMap = new HashMap<Integer, AbstractChildDataSet<T, E>>(); 
		mGroupPosition = groupPosition; 
	}
	
	protected abstract AbstractChildDataSet<T, E> createChildDataSet(int groupPosition, int childPosition); 
	
	@Override
	public synchronized boolean requery() {
		mChildMap.clear(); 
		return true; 
	}
	
	@Override
	public boolean isClosed() { return false; } 
	
	@Override
	public synchronized void close() { 
		mChildMap.clear(); 
	}
	
	@Override
	public long getChildId(int childPosition) { 
		return childPosition; 
	}
	
	@Override
	public AbstractChildDataSet<T, E> getChildDataSet(int childPosition) { 
		if (childPosition < 0 || childPosition >= getChildrenCount()) 
			return null; 
		
		synchronized (this) {
			Integer key = new Integer(childPosition); 
			AbstractChildDataSet<T, E> data = mChildMap.get(key); 
			if (data == null) {
				data = createChildDataSet(mGroupPosition, childPosition); 
				if (data != null) 
					mChildMap.put(key, data); 
			}
			return data; 
		}
	}
	
	@Override
	public boolean isChildSelectable(int childPosition) { 
		return true; 
	}
	
}
