package org.javenstudio.cocoka.android;

import android.content.SharedPreferences;

public interface PreferenceAdapter {

	public SharedPreferences getPreferences(); 
	public String getStringKey(String id); 
	
}
