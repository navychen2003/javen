package org.javenstudio.cocoka.net;

import android.net.NetworkInfo;

public interface OnNetworkChangeListener {

	public void onNetworkChanged(NetworkInfo activeInfo, 
			NetworkInfo mobileInfo, NetworkInfo wifiInfo); 
	
}
