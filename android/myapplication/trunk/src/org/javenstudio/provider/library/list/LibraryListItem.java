package org.javenstudio.provider.library.list;

import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderItem;

public abstract class LibraryListItem extends DataBinderItem {

	public abstract int getViewRes();
	public abstract void bindView(IActivity activity, View view);
	
	public void onUpdateViewOnVisible(boolean restartSlide) {}
	
}
