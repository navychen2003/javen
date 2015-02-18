package org.javenstudio.android.app;

import android.app.Activity;
import android.view.View;
import android.widget.ListAdapter;

import org.javenstudio.cocoka.app.IOptionsMenu;

public interface IFragment {

	public Activity getActivity();
	public View getView();
	public IOptionsMenu getOptionsMenu();
	public void setListAdapter(ListAdapter adapter);
	
}
