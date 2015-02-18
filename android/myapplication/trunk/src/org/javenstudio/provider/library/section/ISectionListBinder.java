package org.javenstudio.provider.library.section;

import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SelectMode;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.library.IVisibleData;

public interface ISectionListBinder extends ProviderBinder {

	public void setVisibleSelection(IVisibleData data);
	public void updateItemView(SectionListItem item, View view, boolean restartSlide);
	public void showAboveCenterView(boolean show);
	public void onActionModeViewBinded(SelectMode mode, View view);
	public void bindBackgroundView(IActivity activity);
	
	public SectionListDataSets getDataSets();
	public IActivity getBindedActivity();
	public View getBindedListView();
	public int getHomeAsUpIndicatorMenuRes();
	public int getHomeAsUpIndicatorBackRes();
	public int getItemViewBackgroundRes(boolean selected);
	public int getColumnSize();
	
}
