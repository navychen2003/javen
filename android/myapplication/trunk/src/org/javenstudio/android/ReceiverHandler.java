package org.javenstudio.android;

import android.content.Intent;

public interface ReceiverHandler {

	public boolean handleIntent(ReceiverCallback callback, Intent intent); 
	
}
