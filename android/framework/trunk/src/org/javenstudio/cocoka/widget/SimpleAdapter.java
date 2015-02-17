package org.javenstudio.cocoka.widget;

import android.content.Context;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;

public class SimpleAdapter {

	public static interface SimpleData {
		public Object get(Object key); 
	}
	
	public static interface SimpleCursorFactory {
		public SimpleCursor create(); 
	}
	
	public static interface SimpleCursor {
		public boolean requery(); 
		public boolean isClosed(); 
		public void close(); 
		public int getCount(); 
		public void recycle(); 
		
		public SimpleDataSet getDataSet(int position); 
		public int getDataId(int position); 
		public void setDataSet(int position, SimpleDataSet data); 
		public void addDataSet(SimpleDataSet data); 
		public void clear(); 
	}
	
	public static class SimpleAdapterImpl extends AdvancedAdapter {
		public SimpleAdapterImpl(Context context, SimpleDataSets data,
	            int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
		}
	}
	
	public static class SimpleDataSet implements AdvancedAdapter.DataSet {
		protected final SimpleDataSets mDataSets; 
		protected final SimpleData mData; 
		protected boolean mSelected = false; 
		private View mBindedView = null; 
		
		public SimpleDataSet(SimpleDataSets dataSets, SimpleData data) {
			this.mDataSets = dataSets; 
			this.mData = data; 
		}
		
		public void setSelected(boolean selected) {
			this.mSelected = selected; 
		}
		
		public boolean isSelected() {
			return mSelected; 
		}
		
		public void recycle() {
			// do nothing
		}
		
		@Override 
		public Object getObject() {
			return mData; 
		}
		
		@Override 
		public Object get(Object key) {
			return mData != null ? mData.get(key) : null; 
		}
		
		@Override 
		public boolean isEnabled() { 
			return true; 
		}
		
		@Override
		public final void setBindedView(View view) { 
			mBindedView = view;
			if (view != null) 
				view.setTag(this);
		}
		
		@Override
		public final View getBindedView() { 
			View view = mBindedView;
			if (view != null && view.getTag() == this) 
				return view;
			
			return null;
		}
	}
	
	public static class SimpleDataSets implements AdvancedAdapter.DataSets {

		private final ContentObservable mContentObservable = new ContentObservable();
		private final DataSetObservable mDataSetObservable = new DataSetObservable();
		
		private SimpleCursorFactory mFactory; 
		private SimpleCursor mCursor; 
		
		private long mDataSetChangeTime = 0; 
		
		public SimpleDataSets(SimpleCursorFactory factory) {
			this.mFactory = factory; 
			this.mCursor = factory.create(); 
		}
		
		public SimpleCursor getCursor() {
			return mCursor; 
		}
		
		public AdvancedAdapter.DataSets create(int count) {
			return new SimpleDataSets(mFactory);
		}
		
		public AdvancedAdapter.DataSets create(AdvancedAdapter.DataSets data) {
			if (data == null || !(data instanceof SimpleDataSets)) 
				return null; 
			
			return new SimpleDataSets(mFactory);
		}
		
		public boolean requery() { return mCursor.requery(); } 
		public boolean isClosed() { return mCursor.isClosed(); } 
		public void close() { mCursor.close(); } 
		public int getCount() { return mCursor.getCount(); } 
		public void recycle() { mCursor.recycle(); }
		
		public AdvancedAdapter.DataSet getDataSet(int position) { 
			return mCursor.getDataSet(position); 
		}
		
		public int getDataId(int position) { 
			return mCursor.getDataId(position); 
		}
		
		public void setDataSet(int position, AdvancedAdapter.DataSet data) {
			mCursor.setDataSet(position, (SimpleDataSet)data); 
		}
		
		public void addDataSet(AdvancedAdapter.DataSet data) {
			mCursor.addDataSet((SimpleDataSet)data); 
		}
		
		public void clear() {
			mCursor.clear(); 
			notifyContentChanged(true);
			notifyDataSetChanged();
		}
		
		public SimpleDataSet createDataSet(SimpleData data) {
			return new SimpleDataSet(this, data); 
		}
		
		public SimpleDataSet addItem(SimpleData data) {
			SimpleDataSet dataSet = createDataSet(data); 
			
			if (dataSet != null) {
				addDataSet(dataSet); 

				notifyContentChanged(true);
				notifyDataSetChanged();
			}
			
			return dataSet; 
		}
		
		public void onChanged(SimpleData data) {
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
		
		public void registerContentObserver(ContentObserver observer) {
			mContentObservable.registerObserver(observer);
		}
		
		public void unregisterContentObserver(ContentObserver observer) {
			mContentObservable.unregisterObserver(observer);
		}
		
		public void registerDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.registerObserver(observer);
		}
		
		public void unregisterDataSetObserver(DataSetObserver observer) {
			mDataSetObservable.unregisterObserver(observer);
		}
	}
	
}
