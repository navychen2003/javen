package org.javenstudio.cocoka.widget.adapter;

import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.DataSetObservable;
import android.database.DataSetObserver;

import org.javenstudio.cocoka.widget.ExpandableAdapter;

public abstract class AbstractGroupDataSets<T extends IExpandableObject, E extends IExpandableObject> 
		implements ExpandableAdapter.ExpandableDataSets {

	private final ContentObservable mContentObservable = new ContentObservable();
	private final DataSetObservable mDataSetObservable = new DataSetObservable();
	
	private final IGroupDataSetCursorFactory<T, E> mFactory; 
	private final IGroupDataSetCursor<T, E> mCursor; 
	
	private long mDataSetChangeTime = 0; 
	
	public AbstractGroupDataSets(IGroupDataSetCursorFactory<T, E> factory) {
		mFactory = factory; 
		mCursor = factory.createGroupCursor(); 
	}
	
	protected abstract AbstractGroupDataSet<T, E> createGroupDataSet(int groupPosition, T data); 
	protected abstract AbstractChildDataSet<T, E> createChildDataSet(int groupPosition, int childPosition, E data); 
	
	public final IGroupDataSetCursorFactory<T, E> getFactory() { 
		return mFactory; 
	}
	
	public final IGroupDataSetCursor<T, E> getCursor() {
		return mCursor; 
	}
	
	@Override 
	public boolean requery() { 
		return mCursor.requery(); 
	}
	
	@Override 
	public boolean isClosed() { 
		return mCursor.isClosed(); 
	}
	
	@Override 
	public void close() { 
		mCursor.close(); 
	}
	
	public boolean closeGroupDataSet(int groupPosition) { 
		return mCursor.closeGroupDataSet(groupPosition); 
	}
	
	@Override 
	public int getGroupCount() { 
		return mCursor.getGroupCount(); 
	}
	
	@Override 
	public long getGroupId(int groupPosition) { 
		return mCursor.getGroupId(groupPosition); 
	}
	
	@Override 
	public ExpandableAdapter.GroupDataSet getGroupDataSet(int groupPosition) { 
		return mCursor.getGroupDataSet(groupPosition); 
	}
	
	@Override 
	public boolean hasStableIds() { 
		return mCursor.hasStableIds(); 
	}
	
	protected void onChanged(IDataSetObject data) {
		if (data != null) {
			notifyContentChanged(true);
			//notifyDataSetChanged();
		}
	}
	
	public final long getDataSetChangeTime() { 
		return mDataSetChangeTime; 
	}
	
	@SuppressWarnings("deprecation")
	public void notifyContentChanged(boolean selfChange) {
		mContentObservable.notifyChange(selfChange); //dispatchChange(selfChange, null);
	}
	
	public void notifyDataSetChanged() {
		mDataSetChangeTime = System.currentTimeMillis();
		mDataSetObservable.notifyChanged();
	}
	
	@Override 
	public void registerContentObserver(ContentObserver observer) {
		mContentObservable.registerObserver(observer);
	}
	
	@Override 
	public void unregisterContentObserver(ContentObserver observer) {
		mContentObservable.unregisterObserver(observer);
	}
	
	@Override 
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.registerObserver(observer);
	}
	
	@Override 
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservable.unregisterObserver(observer);
	}
	
}