package org.javenstudio.cocoka.widget.adapter;

import org.javenstudio.cocoka.widget.AdvancedAdapter;

public abstract class AbstractDataSet<T extends IDataSetObject> 
		implements AdvancedAdapter.DataSet {

	private final AbstractDataSets<T> mDataSets; 
	//private View mBindedView = null; 
	private T mObject; 
	
	public AbstractDataSet(AbstractDataSets<T> dataSets, T data) {
		mDataSets = dataSets; 
		mObject = data; 
	}
	
	public final AbstractDataSets<T> getDataSets() { 
		return mDataSets; 
	}
	
	public final void setObject(T data) { 
		mObject = data; 
	}
	
	@Override 
	public final Object getObject() {
		return mObject; 
	}
	
	public final T getData() {
		return mObject;
	}
	
	@Override 
	public Object get(Object key) {
		if (mObject != null && mObject instanceof IExpandableObject) 
			return ((IExpandableObject)mObject).get(key, 0); 
		
		if (mObject != null && mObject instanceof IDataSetObject) 
			return ((IDataSetObject)mObject).get(key); 
		
		return null; 
	}
	
	@Override 
	public boolean isEnabled() { 
		return true; 
	}
	
	//@Override
	//public final void setBindedView(View view) { 
	//	mBindedView = view;
	//	if (view != null) 
	//		view.setTag(this);
	//}
	
	//@Override
	//public final View getBindedView() { 
	//	View view = mBindedView;
	//	if (view != null && view.getTag() == this) 
	//		return view;
	//	
	//	return null;
	//}
	
}
