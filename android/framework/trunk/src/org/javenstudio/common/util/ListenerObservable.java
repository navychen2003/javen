package org.javenstudio.common.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ListenerObservable {

	public static final int ACTION_ADD = 1; 
	public static final int ACTION_DELETE = 2; 
	public static final int ACTION_MODIFY = 3; 
	public static final int ACTION_START = 4; 
	public static final int ACTION_STOP = 5; 
	
	public static interface OnChangeListener { 
		public void onChangeEvent(int action, Object param); 
	}
	
	private final List<WeakReference<OnChangeListener>> mListenerRefs = 
			new ArrayList<WeakReference<OnChangeListener>>(); 
	
	public ListenerObservable() {}
	
	public boolean registerListener(OnChangeListener l) { 
		if (l == null) return false; 
		
		synchronized (mListenerRefs) { 
			boolean found = false; 
			for (int i=0; i < mListenerRefs.size(); ) { 
				WeakReference<OnChangeListener> ref = mListenerRefs.get(i); 
				OnChangeListener listener = ref != null ? ref.get() : null; 
				if (listener == null) { 
					mListenerRefs.remove(i); 
					continue; 
				}
				if (listener == l) found = true; 
				i++; 
			}
			
			if (!found) { 
				mListenerRefs.add(new WeakReference<OnChangeListener>(l)); 
				return true; 
			}
		}
		
		return false; 
	}
	
	public boolean unregisterListener(OnChangeListener l) { 
		if (l == null) return false; 
		
		synchronized (mListenerRefs) { 
			boolean found = false; 
			
			for (int i=0; i < mListenerRefs.size(); ) { 
				WeakReference<OnChangeListener> ref = mListenerRefs.get(i); 
				OnChangeListener listener = ref != null ? ref.get() : null; 
				if (listener == null || listener == l) { 
					mListenerRefs.remove(i); 
					if (listener == l) found = true; 
					continue; 
				}
				i++; 
			}
			
			return found; 
		}
	}
	
	public void notifyChange(final int action, final Object param) { 
		synchronized (mListenerRefs) { 
			for (int i=0; i < mListenerRefs.size(); ) { 
				WeakReference<OnChangeListener> ref = mListenerRefs.get(i); 
				OnChangeListener listener = ref != null ? ref.get() : null; 
				if (listener == null) { 
					mListenerRefs.remove(i); 
					continue; 
				}
				listener.onChangeEvent(action, param); 
				i++; 
			}
		}
	}
	
}
