package org.javenstudio.android;

import android.content.Context;
import android.content.Intent;

public interface ServiceHandler {

	public void onServiceCreate(ServiceCallback callback);
	public void onServiceDestroy(ServiceCallback callback);
	public void onServiceLowMemory(ServiceCallback callback);
	
	public boolean handleCommand(ServiceCallback callback, Intent intent, int flags, int startId); 
	public boolean checkServiceEnabled(Context context); 
	public void actionServiceStart(Context context); 
	public boolean canStopSelf(); 
	
}
