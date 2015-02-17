package org.javenstudio.cocoka.widget;

import java.util.HashMap;
import java.util.Map;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class SimpleDialog extends Dialog {

	public interface ViewBinder { 
		public void bindView(String name, View view); 
	}
	
	public interface OnCreatedListener { 
		public void onDialogCreated(SimpleDialog dialog); 
	}
	
	private Map<Integer, View.OnClickListener> mClickListeners = 
		new HashMap<Integer, View.OnClickListener>();
	
	private Map<Integer, View.OnLongClickListener> mLongClickListeners = 
		new HashMap<Integer, View.OnLongClickListener>();
	
	private final int mContentViewId; 
	private OnCreatedListener mOnCreatedListener = null; 
	private int[] mViewResources = null; 
	private String[] mViewNames = null; 
	private ViewBinder mViewBinder = null; 
	private boolean mCreated = false; 
	
	public SimpleDialog(Context context, int style, int contentView) {
		super(context, style); 
		
		mContentViewId = contentView; 
		mCreated = false; 
	}
	
	public void setOnCreatedListener(OnCreatedListener listener) { 
		mOnCreatedListener = listener;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(mContentViewId); 
		
		synchronized (this) { 
			for (Integer key : mClickListeners.keySet()) {
				View.OnClickListener l = mClickListeners.get(key); 
				if (l != null) {
					View view = findViewById(key.intValue()); 
					if (view != null) 
						view.setOnClickListener(l); 
				}
			}
			
			for (Integer key : mLongClickListeners.keySet()) {
				View.OnLongClickListener l = mLongClickListeners.get(key); 
				if (l != null) {
					View view = findViewById(key.intValue()); 
					if (view != null) 
						view.setOnLongClickListener(l); 
				}
			}
		}
		
		OnCreatedListener listener = mOnCreatedListener; 
		if (listener != null) 
			listener.onDialogCreated(this);
		
		mCreated = true; 
	}
	
	public void setViewBinder(String[] names, int[] resourceids, ViewBinder binder) { 
		synchronized (this) { 
			mViewNames = names; 
			mViewResources = resourceids; 
			mViewBinder = binder; 
			
			if (names != null && resourceids != null && names.length != resourceids.length) 
				throw new IllegalArgumentException("view binder parameter error"); 
		}
	}
	
	protected void bindViews() { 
		synchronized (this) { 
			final int[] ids = mViewResources; 
			final String[] names = mViewNames; 
			final ViewBinder binder = mViewBinder; 
			
			if (ids != null && names != null && ids.length > 0 && binder != null) { 
				for (int i=0; i < ids.length && i < names.length; i++) { 
					View view = findViewById(ids[i]); 
					if (view != null) 
						binder.bindView(names[i], view); 
				}
			}
		}
	}
	
	public boolean setOnClickListener(int resid, View.OnClickListener l) {
		if (resid != 0 && l != null) {
			if (mCreated) {
				View view = findViewById(resid); 
				if (view != null) {
					view.setOnClickListener(l); 
					return true; 
				}
			} else {
				synchronized (this) { 
					mClickListeners.put(new Integer(resid), l); 
				}
				return true; 
			}
		}
		return false; 
	}
	
	public boolean setOnLongClickListener(int resid, View.OnLongClickListener l) {
		if (resid != 0 && l != null) {
			if (mCreated) {
				View view = findViewById(resid); 
				if (view != null) {
					view.setOnLongClickListener(l); 
					return true; 
				}
			} else {
				synchronized (this) { 
					mLongClickListeners.put(new Integer(resid), l); 
				}
				return true; 
			}
		}
		return false; 
	}
	
}
