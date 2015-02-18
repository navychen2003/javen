package org.javenstudio.android;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AndroidReceiver extends BroadcastReceiver 
		implements ReceiverCallback {

	@Override
    public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) 
			ServiceHelper.checkServiceStart(context); 
		
		handleIntent(this, intent); 
	}
	
	@Override
	public boolean isResultOk(int resultCode) { 
		return resultCode == Activity.RESULT_OK; 
	}
	
	private static List<WeakReference<ReceiverHandler>> mHandlerRefs = 
			new ArrayList<WeakReference<ReceiverHandler>>(); 
	
	public static void registerHandler(ReceiverHandler handler) { 
		if (handler == null) return; 
		
		synchronized (mHandlerRefs) { 
			boolean found = false; 
			
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ReceiverHandler> ref = mHandlerRefs.get(i); 
				ReceiverHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				} else if (sh == handler) 
					found = true; 
				i ++; 
			}
			
			if (!found) 
				mHandlerRefs.add(new WeakReference<ReceiverHandler>(handler)); 
		}
	}
	
	public static void unregisterHandler(ReceiverHandler handler) { 
		if (handler == null) return; 
		
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ReceiverHandler> ref = mHandlerRefs.get(i); 
				ReceiverHandler sh = ref != null ? ref.get() : null; 
				if (sh == null || sh == handler) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				i ++; 
			}
		}
	}
	
	static void handleIntent(ReceiverCallback callback, Intent intent) { 
		synchronized (mHandlerRefs) { 
			for (int i=0; i < mHandlerRefs.size(); ) { 
				WeakReference<ReceiverHandler> ref = mHandlerRefs.get(i); 
				ReceiverHandler sh = ref != null ? ref.get() : null; 
				if (sh == null) { 
					mHandlerRefs.remove(i); 
					continue; 
				}
				sh.handleIntent(callback, intent); 
				i ++; 
			}
		}
	}
	
}
