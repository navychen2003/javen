package org.javenstudio.cocoka.widget.model;

import java.lang.ref.WeakReference;

import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;

public abstract class NavigationItem {

	public interface OnSelectedListener { 
		public void onSelected(NavigationItem item, boolean selected); 
	}
	
	public interface Callbacks { 
		public void onInvoke(NavigationItem item, int action, Object param); 
	}
	
	private final NavigationInfo mInfo; 
	private NavigationGroup mGroup = null; 
	private OnSelectedListener mOnSelectedListener = null; 
	private WeakReference<Callbacks> mCallbackRef = null; 
	private WeakReference<View> mViewRef = null; 
	private boolean mSelected; 
	
	public NavigationItem(NavigationInfo info, boolean selected) { 
		mInfo = info; 
		mSelected = selected; 
		
		if (info == null) 
			throw new NullPointerException("NavigationInfo is null");
	}
	
	public final NavigationInfo getInfo() { 
		return mInfo; 
	}
	
	public void setParent(NavigationGroup group) { 
		mGroup = group; 
	}
	
	public NavigationGroup getParent() { 
		return mGroup; 
	}
	
	public void setOnSelectedListener(OnSelectedListener listener) { 
		mOnSelectedListener = listener; 
	}
	
	public void setSelected(boolean selected) { 
		boolean old = mSelected; 
		mSelected = selected; 
		
		OnSelectedListener listener = mOnSelectedListener; 
		if (listener != null) 
			listener.onSelected(this, selected); 
		
		if (selected) onSelected(old != selected); 
		else onUnSelected(); 
	}
	
	public void setCallbacks(Callbacks callbacks) { 
		mCallbackRef = new WeakReference<Callbacks>(callbacks); 
	}
	
	public void callback(int action, Object param) { 
		WeakReference<Callbacks> callbackRef = mCallbackRef; 
		if (callbackRef != null) { 
			Callbacks callbacks = callbackRef.get(); 
			if (callbacks != null) 
				callbacks.onInvoke(this, action, param); 
		}
	}
	
	public void setBindView(View view) { 
		if (view != null) 
			mViewRef = new WeakReference<View>(view); 
		else
			mViewRef = null; 
	}
	
	public View getBindView() { 
		WeakReference<View> viewRef = mViewRef; 
		return viewRef != null ? viewRef.get() : null; 
	}
	
	public boolean isSelected() { 
		return mSelected; 
	}
	
	protected void onSelected(boolean updated) { 
		// do nothing
	}
	
	protected void onUnSelected() { 
		// do nothing
	}
	
	public boolean shouldReload() { 
		AbstractDataSets<?> dataSets = getDataSets(); 
		if (dataSets != null && dataSets.getCount() > 0) 
			return false; 
		else 
			return true; 
	}
	
	public void clearDataSets() { 
		getDataSets().clear(); 
	}
	
	public void notifyDataSets() { 
		getDataSets().notifyContentChanged(true);
		getDataSets().notifyDataSetChanged();
	}
	
	public void postClearDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() { 
				public void run() { 
					clearDataSets(); 
				}
			});
	}
	
	public void postNotifyDataSets() { 
		ResourceHelper.getHandler().post(new Runnable() { 
				public void run() { 
					notifyDataSets(); 
				}
			});
	}
	
	public abstract AbstractDataSets<?> getDataSets(); 
	
}
