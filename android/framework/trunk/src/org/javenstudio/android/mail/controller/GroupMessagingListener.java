package org.javenstudio.android.mail.controller;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class GroupMessagingListener {
    /* The synchronization of the methods in this class
       is not needed because we use ConcurrentHashMap.
       
       Nevertheless, let's keep the "synchronized" for a while in the case
       we may want to change the implementation to use something else
       than ConcurrentHashMap.
    */

	private final List<WeakReference<MessagingListener>> mListenerRefs = 
			new ArrayList<WeakReference<MessagingListener>>(); 
	
	public boolean registerListener(MessagingListener l) { 
		if (l == null) return false; 
		
		synchronized (mListenerRefs) { 
			boolean found = false; 
			for (int i=0; i < mListenerRefs.size(); ) { 
				WeakReference<MessagingListener> ref = mListenerRefs.get(i); 
				MessagingListener listener = ref != null ? ref.get() : null; 
				if (listener == null) { 
					mListenerRefs.remove(i); 
					continue; 
				}
				if (listener == l) found = true; 
				i++; 
			}
			
			if (!found) { 
				mListenerRefs.add(new WeakReference<MessagingListener>(l)); 
				return true; 
			}
		}
		
		return false; 
	}
	
	public boolean unregisterListener(MessagingListener l) { 
		if (l == null) return false; 
		
		synchronized (mListenerRefs) { 
			boolean found = false; 
			
			for (int i=0; i < mListenerRefs.size(); ) { 
				WeakReference<MessagingListener> ref = mListenerRefs.get(i); 
				MessagingListener listener = ref != null ? ref.get() : null; 
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
	
	public void notifyEvent(final MessagingEvent event) { 
		synchronized (mListenerRefs) { 
			for (int i=0; i < mListenerRefs.size(); ) { 
				WeakReference<MessagingListener> ref = mListenerRefs.get(i); 
				MessagingListener listener = ref != null ? ref.get() : null; 
				if (listener == null) { 
					mListenerRefs.remove(i); 
					continue; 
				}
				listener.onMessagingEvent(event); 
				i++; 
			}
		}
	}
	
}