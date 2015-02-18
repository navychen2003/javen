package org.javenstudio.android;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.OnNetworkChangeListener;

public final class NetworkHelper {

	public interface NetworkListener { 
		public void onNetworkAvailableChanged(NetworkHelper helper);
	}
	
	private static final NetworkHelper sInstance = new NetworkHelper();
	public static NetworkHelper getInstance() { return sInstance; }
	
	private final List<WeakReference<NetworkListener>> mListeners = 
			new ArrayList<WeakReference<NetworkListener>>(); 
	
	private int mNetworkType = -1;
	
	private NetworkHelper() { 
		ResourceHelper.getNetworkMonitor().registerListener(new OnNetworkChangeListener() {
				@Override
				public void onNetworkChanged(NetworkInfo activeInfo,
						NetworkInfo mobileInfo, NetworkInfo wifiInfo) {
					int preType = mNetworkType;
					mNetworkType = activeInfo != null ? activeInfo.getType() : -1;
					if (preType != mNetworkType) 
						onNetworkAvailableChanged();
				}
			});
	}
	
	public final void addNetworkListener(NetworkListener listener) { 
    	synchronized (mListeners) { 
    		boolean found = false; 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<NetworkListener> ref = mListeners.get(i); 
    			NetworkListener lsr = ref != null ? ref.get() : null; 
    			if (lsr == null) { 
    				mListeners.remove(i); continue; 
    			} else if (lsr == listener) 
    				found = true; 
    			i ++; 
    		}
    		
    		if (listener != null) {
    			listener.onNetworkAvailableChanged(this);
    			if (!found) 
    				mListeners.add(new WeakReference<NetworkListener>(listener)); 
    		}
    	}
    }
    
    private final void onNetworkAvailableChanged() { 
    	synchronized (mListeners) { 
    		for (int i=0; i < mListeners.size(); ) { 
    			WeakReference<NetworkListener> ref = mListeners.get(i); 
    			NetworkListener listener = ref != null ? ref.get() : null; 
    			if (listener == null) { 
    				mListeners.remove(i); continue; 
    			} else { 
    				listener.onNetworkAvailableChanged(this);
    			}
    			i ++; 
    		}
    	}
    }
	
	public final boolean isNetworkAvailable() { 
		return isWifiAvailable() || isMobileAvailable();
	}
	
	public final boolean isWifiAvailable() { 
		return mNetworkType == ConnectivityManager.TYPE_WIFI;
	}
	
	public final boolean isMobileAvailable() { 
		return mNetworkType == ConnectivityManager.TYPE_MOBILE;
	}
	
}
