package org.javenstudio.cocoka.widget.model;

import android.content.Context;

public abstract class Controller {

	public Controller() {}
	
	public final void initialize(ActivityInitializer initializer) { 
		synchronized (this) { 
			getModel().initialize(this, initializer); 
			onInitialized(initializer); 
		}
	}
	
	protected void onInitialized(ActivityInitializer initializer) { 
		// do nothing
	}
	
	public abstract Context getContext(); 
	public abstract Model getModel(); 
	
	public void startLoader(Context context, boolean isLaunching) {
		getModel().startLoader(context, isLaunching); 
	}
	
}
