package org.anybox.android.library;

import android.app.Application;
import android.content.res.Configuration;

public class MyApplication extends Application {

	@Override
    public void onCreate() {
        //VMRuntime.getRuntime().setMinimumHeapSize(6 * 1024 * 1024);
        super.onCreate();
        new MyImplements(this).init(); 
	}
	
	@Override
    public void onTerminate() {
		MyImplements.terminate();
        super.onTerminate();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
    }
    
	@Override
    public void onLowMemory() {
		super.onLowMemory();
    }

}
