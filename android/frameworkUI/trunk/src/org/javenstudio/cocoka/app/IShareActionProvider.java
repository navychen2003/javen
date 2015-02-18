package org.javenstudio.cocoka.app;

import android.content.Intent;

public interface IShareActionProvider extends IActionProvider {

	public void setShareHistoryFileName(String shareHistoryFile);
	public void setShareIntent(Intent shareIntent);
	
}
