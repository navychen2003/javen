package org.javenstudio.cocoka.widget.adapter;

import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.DataSetObservable;
import android.database.DataSetObserver;

import org.javenstudio.cocoka.widget.AdvancedAdapter;
import org.javenstudio.common.util.Logger;

@SuppressWarnings({"unchecked"})
public abstract class AbstractDataSets<T extends IDataSetObject> 
		implements AdvancedAdapter.DataSets {
	private static final Logger LOG = Logger.getLogger(AbstractDataSets.class);
	
	private final ContentObservable mContentObservable = new ContentObservable();
	private final DataSetObservable mDataSetObservable = new DataSetObservable();
	
	private final IDataSetCursorFactory<T> mFactory; 
	private final IDataSetCursor<T> mCursor; 
	
	private Object mObject = null;
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
		if (LOG.isDebugEnabled()) LOG.debug("clear: dataSets=" + this);
		
		mCursor.clear(); 
		notifyContentChanged(true);
		notifyDataSetChanged();
	}
	
	public void setTag(Object tag) { mObject = tag; }
	public Object getTag() { return mObject; }
	
	protected void onDataSetPreAdd(AbstractDataSet<T> dataSet) {}
	protected void onDataSetAdded(AbstractDataSet<T> dataSet) {}
	
	public final AbstractDataSet<T> addDataList(IDataSetObject[] datas) {
		return addDataList(datas, true);
	}
	
	public final AbstractDataSet<T> addDataList(IDataSetObject[] datas, boolean notify) {
		AbstractDataSet<T> dataSet = null;
		
		if (datas != null) {
			for (IDataSetObject data : datas) {
				dataSet = addData(data, false);
			}
			
			if (notify) {
				notifyContentChanged(true);
				notifyDataSetChanged();
			}
		}
		
		return dataSet;
	}
	
	public final AbstractDataSet<T> addData(IDataSetObject data) {
		return addData(data, true);
	}
	
	public final AbstractDataSet<T> addData(IDataSetObject data, boolean notify) {
		AbstractDataSet<T> dataSet = createDataSet(data); 
		
		if (dataSet != null) {
			onDataSetPreAdd(dataSet);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("addData: dataSets=" + this 
						+ " data=" + data + " notify=" + notify);
			}
			
			//dataSet.setDataSets(this); 
			addDataSet(dataSet); 
			
			onDataSetAdded(dataSet); 
			
			if (notify) { 
				notifyContentChanged(true);
				notifyDataSetChanged();
			}
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
	
	@SuppressWarnings("deprecation")
	public void notifyContentChanged(boolean selfChange) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("notifyContentChanged: dataSets=" + this 
					+ " selfChange=" + selfChange);
		}
		mContentObservable.notifyChange(selfChange); //dispatchChange(selfChange, null);
	}
	
	public void notifyDataSetChanged() {
		if (LOG.isDebugEnabled()) 
			LOG.debug("notifyDataSetChanged: dataSets=" + this);
		
		mDataSetChangeTime = System.currentTimeMillis();
		mDataSetObservable.notifyChanged();
	}
	
	@Override 
	public void registerContentObserver(ContentObserver observer) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("registerContentObserver: dataSets=" + this 
					+ " observer=" + observer);
		}
		mContentObservable.registerObserver(observer);
	}
	
	@Override 
	public void unregisterContentObserver(ContentObserver observer) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("unregisterContentObserver: dataSets=" + this 
					+ " observer=" + observer);
		}
		mContentObservable.unregisterObserver(observer);
	}
	
	@Override 
	public void registerDataSetObserver(DataSetObserver observer) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("registerDataSetObserver: dataSets=" + this 
					+ " observer=" + observer);
		}
		mDataSetObservable.registerObserver(observer);
	}
	
	@Override 
	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("unregisterDataSetObserver: dataSets=" + this 
					+ " observer=" + observer);
		}
		mDataSetObservable.unregisterObserver(observer);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{factory=" + getFactory() 
				+ ",cursor=" + getCursor() + ",count=" + getCount() + "}";
	}
	
}
