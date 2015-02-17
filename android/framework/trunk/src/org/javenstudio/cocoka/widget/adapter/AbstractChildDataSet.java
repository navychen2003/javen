package org.javenstudio.cocoka.widget.adapter;

import android.view.View;

import org.javenstudio.cocoka.widget.ExpandableAdapter;

public class AbstractChildDataSet<T extends IExpandableObject, E extends IExpandableObject> 
				implements ExpandableAdapter.ChildDataSet {

	private final AbstractGroupDataSets<T, E> mDataSets; 
	private final int mGroupPosition; 
	private final int mChildPosition; 
	private final E mObject; 
	private View mBindedView = null; 
	
	public AbstractChildDataSet(AbstractGroupDataSets<T, E> dataSets, 
			int groupPosition, int childPosition, E data) { 
		mDataSets = dataSets; 
		mGroupPosition = groupPosition; 
		mChildPosition = childPosition; 
		mObject = data; 
	}
	
	public final AbstractGroupDataSets<T, E> getDataSets() { 
		return mDataSets; 
	}
	
	public final int getGroupPosition() { 
		return mGroupPosition; 
	}
	
	public final int getChildPosition() { 
		return mChildPosition; 
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
