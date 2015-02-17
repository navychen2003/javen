package org.javenstudio.cocoka.storage;

import android.content.Context;

public final class Constants {

	public static void initStorageManager(Context context, StorageManager.Directories dirs) { 
		StorageManager.initInstance(context, dirs);
	}
	
}
