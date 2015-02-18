package org.javenstudio.android.app;

import android.app.Activity;
import android.os.Bundle;

import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;

public interface IMenuExecutor {

	public void onCreate(Activity activity, Bundle savedInstanceState);
	public void onUpdateContent(Activity activity);
	
	public boolean onCreateOptionsMenu(Activity activity, IMenu menu);
	public boolean onOptionsItemSelected(Activity activity, IMenuItem item);
	
	public void showProgressView(Activity activity);
	public void hideProgressView(Activity activity);
	
}
