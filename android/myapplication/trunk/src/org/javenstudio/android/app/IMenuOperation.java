package org.javenstudio.android.app;

import android.app.Activity;

import org.javenstudio.cocoka.app.IMenu;
import org.javenstudio.cocoka.app.IMenuItem;

public interface IMenuOperation {

	public static interface IOperation { 
		public int getItemId();
		public boolean isEnabled();
		
		public void onCreateOptionsMenu(Activity activity, IMenu menu);
		public void onUpdateContent(Activity activity, IMenu menu);
		
		public boolean onOptionsItemSelected(Activity activity, IMenuItem item);
	}
	
	public IMenuExecutor getMenuExecutor();
	
	public void addOperation(IOperation op);
	public void addOperation(IOperation... ops);
	
	public void onCreateOptionsMenu(Activity activity, IMenu menu);
	public void onUpdateContent(Activity activity, IMenu menu);
	
	public boolean onOptionsItemSelected(Activity activity, IMenuItem item);
	
}
