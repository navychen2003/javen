package org.javenstudio.cocoka.widget.model;

import java.lang.ref.WeakReference;

import android.view.View;

import org.javenstudio.cocoka.android.ActivityHelper;
import org.javenstudio.cocoka.widget.adapter.AbstractDataSets;

public abstract class NavigationItem {

	public interface OnSelectedListener { 
		public void onSelected(NavigationItem item, boolean selected); 
	}
	
	public interface Callbacks { 
		public void onInvoke(NavigationItem item, int action, Object param); 
	}
	
	private final String mDisplayName; 
	private final String mDisplayTitle; 
	private NavigationGroup mGroup = null; 
	private OnSelectedListener mOnSelectedListener = null; 
	private WeakReference<Callbacks> mCallbackRef = null; 
	private WeakReference<View> mViewRef = null; 
	private boolean mSelected; 
	
	public NavigationItem(String displayName, String displayTitle, boolean selected) { 
		mDisplayName = displayName; 
		mDisplayTitle = displayTitle; 
		mSelected = selected; 
	}
	
	public String getDisplayName() { 
		return mDisplayName; 
	}
	
	public String getDisplayTitle() { 
		return mDisplayTitle; 
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
		
	}
	
	protected void onUnSelected() { 
		
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
	
	public void postClearDataSets() { 
		ActivityHelper.getHandler().post(new Runnable() { 
				public void run() { 
					clearDataSets(); 
				}
			});
	}
	
	public abstract AbstractDataSets<?> getDataSets(); 
	
}
