package org.javenstudio.cocoka;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public abstract class ImplementsModule extends Implements {
	
	protected ImplementsModule(Application app, boolean debug) { 
		super(app, debug);
	}
	
	@Override 
	public String getLocalStorageDirectory() {
		return ModuleMethods.getLocalStorageDirectory();
	}
	
	@Override 
	public SharedPreferences getPreferences() { 
		return ModuleMethods.getPreferences();
	}
	
	@Override 
	protected void onInitialized(Context context) { 
		// do nothing
	}
	
}
