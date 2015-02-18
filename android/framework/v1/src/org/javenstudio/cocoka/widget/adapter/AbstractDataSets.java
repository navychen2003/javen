package org.javenstudio.cocoka.widget.adapter;

import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.DataSetObservable;
import android.database.DataSetObserver;

import org.javenstudio.cocoka.widget.AdvancedAdapter;

@SuppressWarnings({"unchecked"})
public abstract class AbstractDataSets<T extends IDataSetObject> implements AdvancedAdapter.DataSets {

	private final ContentObservable mContentObservable = new ContentObservable();
	private final DataSetObservable mDataSetObservable = new DataSetObservable();
	
	private final IDataSetCursorFactory<T> mFactory; 
	private final IDataSetCursor<T> mCursor; 
	
	private long mDataSetChangeTime = 0; 
	
	public AbstractDataSets(IDataSetCursorFactory<T> factory) {
		mFactory = factory; 
		mCursor = factory.create(); 
	}
	
	protected abstract AbstractDataSets<T> createDataSets(IDataSetCursorFactory<T> factory); 
	protected abstract AbstractDataSet<T> createDataSet(IDataSetObject data); 
	
	public final IDataSetCursorFactory<T> getFactory() { 
		return mFactory; 
	}
	
	public final IDataSetCursor<T> getCursor() {
		return mCursor; 
	}
	
	@Override 
	public final AdvancedAdapter.DataSets create(int count) {
		return createDataSets(getFactory()); 
	}
	
	@Override 
	public final AdvancedAdapter.DataSets create(AdvancedAdapter.DataSets data) {
		return createDataSets(getFactory()); 
	}
	
	@Override 
	public boolean requery() { return mCursor.requery(); } 
	
	@Override 
	public boolean isClosed() { return mCursor.isClosed(); } 
	
	@Override 
	public void close() { mCursor.close(); } 
	
	@Override 
	public int getCount() { return mCursor.getCount(); } 
	
	public void recycle() { mCursor.recycle(); }
	
	@Override 
	public AdvancedAdapter.DataSet getDataSet(int position) { 
		return mCursor.getDataSet(position); 
	}
	
	@Override 
	public int getDataId(int position) { 
		return mCursor.getDataId(position); 
	}
	
	public void setDataSet(int position, AdvancedAdapter.DataSet data) {
		mCursor.setDataSet(position, (AbstractDataSet<T>)data); 
	}
	
	@Override 
	public void addDataSet(AdvancedAdapter.DataSet data) {
		mCursor.addDataSet((AbstractDataSet<T>)data); 
	}
	
	public void clear() {
		mCursor.clear(); 
		notifyContentChanged(true);
		notifyDataSetChanged();
	}
	
	protected void onDataSetAdded(AbstractDataSet<T> dataSet) { 
	}
	
	public AbstractDataSet<T> addData(IDataSetObject data) {
		AbstractDataSet<T> dataSet = createDataSet(data); 
		
		if (dataSet != null) {
			//dataSet.setDataSets(this); 
			addDataSet(dataSet); 
			
			onDataSetAdded(dataSet); 
			
			notifyContentChanged(true);
			notifyDataSetChanged();
		}
		
		return dataSet; 
	}
	
	public void onChanged(IDataSetObject data) {
		if (data != null) {
			notifyContentChanged(true);
			//notifyDataSetChanged();
		}
	}
	
	public final long getDataSetChangeTime() { 
		return mDataSetChangeTime; 
	}
	
	public void notifyContentChanged(boolean selfChange) {
		mContentObservable.notifyChange(selfChange);
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
