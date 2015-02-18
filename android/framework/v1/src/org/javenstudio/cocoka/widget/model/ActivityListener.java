package org.javenstudio.cocoka.widget.model;

import android.app.Activity;

public interface ActivityListener {

	public void registerBroadcast(String action, Model.BroadcastHandler handler); 
	public void registerCallback(Model.Callback callback); 
	
	public void onActivityCreate(Activity activity); 
	public void onActivityStart(Activity activity); 
	public void onActivityResume(Activity activity); 
	public void onActivityStop(Activity activity); 
	public void onActivityDestroy(Activity activity); 
	
	public boolean onActivityBackPressed(Activity activity); 
	
}
