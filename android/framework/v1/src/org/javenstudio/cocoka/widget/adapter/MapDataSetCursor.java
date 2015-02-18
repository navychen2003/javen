package org.javenstudio.cocoka.widget.adapter;

import java.util.HashMap;
import java.util.Map;

public abstract class MapDataSetCursor<T extends IDataSetObject> implements IDataSetCursor<T> {

	private final Map<Integer, AbstractDataSet<T>> mMap; 
	
	public MapDataSetCursor() {
		mMap = new HashMap<Integer, AbstractDataSet<T>>(); 
	}
	
	protected abstract AbstractDataSet<T> createDataSet(int position); 
	
	@Override
	public boolean requery() {
		synchronized (this) {
			mMap.clear(); 
		}
		return true; 
	}
	
	@Override
	public boolean refresh() {
		synchronized (this) {
			mMap.clear(); 
		}
		return true; 
	}
	
	@Override
	public AbstractDataSet<T> getDataSet(int position) { 
		if (position < 0 || position >= getCount()) 
			return null; 
		
		synchronized (this) {
			Integer key = new Integer(position); 
			AbstractDataSet<T> data = mMap.get(key); 
			if (data == null) {
				data = createDataSet(position); 
				if (data != null) 
					mMap.put(key, data); 
			}
			return data; 
		}
	}
	
	@Override
	public int getDataId(int position) { return position; } 
	
	@Override
	public void setDataSet(int position, AbstractDataSet<T> data) {} 
	
	@Override
	public void addDataSet(AbstractDataSet<T> data) { } 
	
	@Override
	public void recycle() {} 
	
	@Override
	public void clear() {} 
	
	@Override
	public boolean isClosed() { return false; } 
	
	@Override
	public void close() {} 
	
}
