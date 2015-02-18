package org.javenstudio.cocoka.app;

import android.app.Activity;

public interface IOptionsMenu {

	public boolean hasOptionsMenu(Activity activity);
	public boolean onCreateOptionsMenu(Activity activity, IMenu menu, IMenuInflater inflater);
    public boolean onPrepareOptionsMenu(Activity activity, IMenu menu);
    public boolean onOptionsItemSelected(Activity activity, IMenuItem item);
    public boolean onUpdateOptionsMenu(Activity activity);
    public boolean removeOptionsMenu(Activity activity);
	
}
