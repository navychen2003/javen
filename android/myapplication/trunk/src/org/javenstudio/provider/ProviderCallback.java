package org.javenstudio.provider;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.android.app.ModelCallback;
import org.javenstudio.android.data.ReloadCallback;

public abstract class ProviderCallback extends ModelCallback 
		implements ReloadCallback {

	private final Map<String,String> mParams = new HashMap<String,String>();
	
	public String getParam(String name) { return mParams.get(name); }
	public void setParam(String name, String value) { mParams.put(name, value); }
	public void clearParams() { mParams.clear(); }
	
	public abstract ProviderController getController();
	public abstract Provider getProvider();
	
}
