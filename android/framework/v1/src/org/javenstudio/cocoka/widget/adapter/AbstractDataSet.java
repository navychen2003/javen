package org.javenstudio.cocoka.widget.adapter;

import java.lang.ref.WeakReference;
import android.view.View;

import org.javenstudio.cocoka.widget.AdvancedAdapter;

public class AbstractDataSet<T extends IDataSetObject> implements AdvancedAdapter.DataSet {

	private final AbstractDataSets<T> mDataSets; 
	private T mObject; 
	private WeakReference<View> mBindedViewRef = null; 
	private long mBindedViewTime = 0;
	
	public AbstractDataSet(AbstractDataSets<T> dataSets, T data) {
		mDataSets = dataSets; 
		mObject = data; 
	}
	
	public final AbstractDataSets<T> getDataSets() { 
		return mDataSets; 
	}
	
	public final void setObject(T data) { 
		mObject = data; 
		setBindedView(null);
	}
	
	@Override 
	public final Object getObject() {
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
	
	@Override 
	public void setBindedView(View view) { 
		if (view != null) { 
			mBindedViewRef = new WeakReference<View>(view); 
			mBindedViewTime = System.currentTimeMillis();
		} else { 
			mBindedViewRef = null; 
			mBindedViewTime = 0;
		}
	}
	
	@Override 
	public View getBindedView() { 
		WeakReference<View> viewRef = mBindedViewRef; 
		View view = viewRef != null ? viewRef.get() : null; 
		if (view != null) { 
			if (mDataSets.getDataSetChangeTime() > mBindedViewTime) { 
				setBindedView(null);
				view = null;
			}
		}
		return view;
	}
	
}
