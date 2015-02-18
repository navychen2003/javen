package org.javenstudio.cocoka.app;

import android.content.Context;
import android.widget.SpinnerAdapter;

public interface IActionAdapterFactory {

	public SpinnerAdapter createActionAdapter(Context context, ActionItem[] items);
	
}
