package org.javenstudio.cocoka.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.javenstudio.cocoka.net.NetworkChangeReceiver;
import org.javenstudio.cocoka.net.OnNetworkChangeListener;

public final class NetworkMonitor extends BroadcastReceiver 
		implements NetworkChangeReceiver {

	private final Application mApp; 
	private final Context mContext; 
	private final List<OnNetworkChangeListener> mListeners; 
	
	public NetworkMonitor(Application app, Context context, boolean isModuleApp) { 
		mApp = app; 
		mContext = context; 
		mListeners = new ArrayList<OnNetworkChangeListener>(); 
		
		registerReceiver(app); 
	}
	
	public final Application getApplication() { 
		return mApp; 
	}
	
	public final Context getContext() { 
		return mContext; 
	}
	
	protected void registerReceiver(Application app) { 
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		
		app.registerReceiver(this, intentFilter);
	}
	
	@Override 
	public synchronized void onReceive(Context context, final Intent intent) { 
		final String action = intent.getAction();

        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
        	onNetworkChanged(null); 
        }
	}
	
	protected void onNetworkChanged(OnNetworkChangeListener listener) { 
		ConnectivityManager manager = (ConnectivityManager)
				getContext().getSystemService(Context.CONNECTIVITY_SERVICE); 
		if (manager == null) 
			return; 
		
		NetworkInfo activeInfo = manager.getActiveNetworkInfo(); 
		NetworkInfo mobileInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE); 
		NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI); 
		
		if (listener != null) 
			listener.onNetworkChanged(activeInfo, mobileInfo, wifiInfo); 
		else 
			notifyNetworkChanged(activeInfo, mobileInfo, wifiInfo); 
	}
	
	public synchronized void registerListener(OnNetworkChangeListener listener) { 
		if (listener != null) { 
			for (OnNetworkChangeListener lstr : mListeners) { 
				if (lstr == listener) 
					return; 
			}
			
			mListeners.add(listener); 
			onNetworkChanged(listener); 
		}
	}
	
	protected synchronized void notifyNetworkChanged(NetworkInfo activeInfo, NetworkInfo mobileInfo, NetworkInfo wifiInfo) { 
		for (OnNetworkChangeListener listener : mListeners) { 
			if (listener != null) 
				listener.onNetworkChanged(activeInfo, mobileInfo, wifiInfo); 
		}
	}
	
}
