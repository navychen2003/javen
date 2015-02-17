package org.javenstudio.cocoka.widget.adapter;

import android.view.View;

import org.javenstudio.cocoka.widget.ExpandableAdapter; 

public class AbstractGroupDataSet<T extends IExpandableObject, E extends IExpandableObject> 
				implements ExpandableAdapter.GroupDataSet {

	private final AbstractGroupDataSets<T, E> mDataSets; 
	private final int mGroupPosition; 
	private final T mObject; 
	private IChildDataSetCursor<T, E> mCursor; 
	private View mBindedView = null; 
	
	public AbstractGroupDataSet(AbstractGroupDataSets<T, E> dataSets, 
			int groupPosition, T data) { 
		mDataSets = dataSets; 
		mGroupPosition = groupPosition; 
		mObject = data; 
		mCursor = null; 
	}
	
	public final AbstractGroupDataSets<T, E> getGroupDataSets() { 
		return mDataSets; 
	}
	
	public final int getGroupPosition() { 
		return mGroupPosition; 
	}
	
	@Override 
	public Object getObject() {
		return mObject; 
	}
	
	@Override 
	public Object get(Object key, int stat) {
		if (mObject != null && mObject instanceof IExpandableObject) 
			return ((IExpandableObject)mObject).get(key, stat); 
		
		if (mObject != null && mObject instanceof IDataSetObject) 
			return ((IDataSetObject)mObject).get(key); 
		
		return null; 
	}
	
	private final synchronized void checkCursor() { 
		if (mCursor != null) return; 
		mCursor = mDataSets.getFactory().createChildCursor(this); 
	}
	
	@Override 
	public synchronized boolean requery() { 
		checkCursor(); 
		return mCursor != null ? mCursor.requery() : false; 
	}
	
	@Override 
	public synchronized boolean isClosed() { 
		return mCursor != null ? mCursor.isClosed() : true; 
	}
	
	@Override 
	public synchronized void close() { 
		if (mCursor != null) mCursor.close(); 
		mCursor = null; 
	}
	
	public synchronized IChildDataSetCursor<T, E> getChildDataSetCursor() { 
		checkCursor(); 
		return mCursor; 
	}
	
	@Override 
	public synchronized int getChildrenCount() { 
		checkCursor(); 
		return mCursor != null ? mCursor.getChildrenCount() : 0; 
	}
	
	@Override 
	public synchronized long getChildId(int childPosition) { 
		checkCursor(); 
		return mCursor != null ? mCursor.getChildId(childPosition) : 0; 
	}
	
	@Override 
	public synchronized ExpandableAdapter.ChildDataSet getChildDataSet(int childPosition) { 
		checkCursor(); 
		return mCursor != null ? mCursor.getChildDataSet(childPosition) : null; 
	}
	
	@Override 
	public synchronized boolean isChildSelectable(int childPosition) { 
		checkCursor(); 
		return mCursor != null ? mCursor.isChildSelectable(childPosition) : false; 
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
