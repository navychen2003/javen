package org.javenstudio.cocoka.widget.adapter;

import java.util.ArrayList;
import java.util.List;

public abstract class ListDataSetCursor<T extends IDataSetObject> 
		implements IDataSetCursor<T> {
	
	private final List<AbstractDataSet<T>> mList; 
	
	public ListDataSetCursor() { 
		mList = new ArrayList<AbstractDataSet<T>>(); 
	}
	
	@Override 
	public boolean refresh() { return true; } 
	
	@Override 
	public boolean requery() { return true; } 
	
	@Override 
	public boolean isClosed() { return false; } 
	
	@Override 
	public void close() {} 
	
	@Override 
	public int getCount() { return mList.size(); } 
	
	@Override 
	public void recycle() {
		synchronized (this) {
			if (mList == null) 
				return; 
			
			for (AbstractDataSet<T> dataSet : mList) {
				if (dataSet == null) 
					continue; 
				
				//dataSet.recycle(); 
			}
		}
	}
	
	@Override 
	public AbstractDataSet<T> getDataSet(int position) { 
		return position >= 0 && position < mList.size() ? mList.get(position) : null; 
	}
	
	@Override 
	public int getDataId(int position) { return position; }
	
	@Override 
	public void setDataSet(int position, AbstractDataSet<T> data) {
		synchronized (this) {
			if (position >= 0 && position < mList.size()) {
				if (data != null) {
					mList.set(position, data); 
					onDataSetted(data, position); 
				}
			}
		}
	}
	
	protected void onDataSetted(AbstractDataSet<T> data, int position) { 
		//data.setPosition(position); 
	}
	
	@Override 
	public void addDataSet(AbstractDataSet<T> data) {
		synchronized (this) {
			if (data != null) {
				mList.add(data); 
				onDataAdded(data); 
			}
		}
	}
	
	protected void onDataAdded(AbstractDataSet<T> data) { 
		//data.setPosition(mList.size() - 1); 
	}
	
	@Override 
	public void clear() {
		synchronized (this) {
			if (mList != null && mList.size() > 0) {
				mList.clear(); 
				onCleared(); 
			}
		}
	}
	
	protected void onCleared() { 
		// do nothing
	}
	
}
