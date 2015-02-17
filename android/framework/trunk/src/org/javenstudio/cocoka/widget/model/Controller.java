package org.javenstudio.cocoka.widget.model;

import android.content.Context;

public abstract class Controller {

	public Controller() {}
	
	public final void initialize(Model.Callback callback) { 
		synchronized (this) { 
			getModel().initialize(this, callback); 
			onInitialized(); 
		}
	}
	
	protected void onInitialized() { 
		// do nothing
	}
	
	public abstract Context getContext(); 
	public abstract Model getModel(); 
	
}
