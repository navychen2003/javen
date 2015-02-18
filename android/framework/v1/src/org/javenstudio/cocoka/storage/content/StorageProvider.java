package org.javenstudio.cocoka.storage.content;

import org.javenstudio.cocoka.storage.Storage;

import android.app.Activity;
import android.graphics.drawable.Drawable;

public interface StorageProvider {

	public Storage getStorage(); 
	
	public String getDisplayName(); 
	public Drawable getDisplayDrawable(); 
	
	public void startActivity(Activity fromActivity); 
	
}
