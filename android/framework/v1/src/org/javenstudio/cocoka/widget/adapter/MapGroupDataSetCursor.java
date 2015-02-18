package org.javenstudio.cocoka.widget.adapter;

import java.util.HashMap;
import java.util.Map;

public abstract class MapGroupDataSetCursor<T extends IExpandableObject, E extends IExpandableObject> 
			implements IGroupDataSetCursor<T, E> {

	private final Map<Integer, AbstractGroupDataSet<T, E>> mGroupMap; 
	
	public MapGroupDataSetCursor() {
		mGroupMap = new HashMap<Integer, AbstractGroupDataSet<T, E>>(); 
	}
	
	protected abstract AbstractGroupDataSet<T, E> createGroupDataSet(int groupPosition); 
	
	@Override
	public synchronized boolean requery() {
		mGroupMap.clear(); 
		return true; 
	}
	
	@Override
	public final AbstractGroupDataSet<T, E> getGroupDataSet(final int groupPosition) { 
		if (groupPosition < 0 || groupPosition >= getGroupCount()) 
			return null; 
		
		synchronized (this) {
			Integer key = new Integer(groupPosition); 
			AbstractGroupDataSet<T, E> data = mGroupMap.get(key); 
			if (data == null) {
				data = createGroupDataSet(groupPosition); 
				if (data != null) 
					mGroupMap.put(key, data); 
			}
			return data; 
		}
	}
	
	@Override
	public boolean hasStableIds() { 
		return false; 
	}
	
	@Override
	public long getGroupId(int groupPosition) { 
		return groupPosition; 
	}
	
	@Override
	public boolean isClosed() { return false; } 
	
	@Override
	public synchronized void close() { 
		for (AbstractGroupDataSet<T, E> data : mGroupMap.values()) { 
			if (data != null) data.close(); 
		}
		mGroupMap.clear(); 
	}
	
	@Override 
	public boolean closeGroupDataSet(int groupPosition) { 
		if (groupPosition < 0 || groupPosition >= getGroupCount()) 
			return false; 
		
		synchronized (this) {
			Integer key = new Integer(groupPosition); 
			AbstractGroupDataSet<T, E> data = mGroupMap.get(key); 
			if (data != null) {
				data.close(); 
				mGroupMap.remove(key); 
				return true; 
			}
		}
		
		return false; 
	}
	
}
