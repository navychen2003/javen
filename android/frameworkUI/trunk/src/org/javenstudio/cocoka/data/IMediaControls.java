package org.javenstudio.cocoka.data;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.cocoka.app.ActionItem;

public interface IMediaControls {

	public String getTitle();
	public String getSubTitle();
	
	public String getActionTitle();
	public String getActionSubTitle();
	
	public View getActionCustomView(Activity activity);
	public int getStatisticCount(int type);
	
	public ActionItem[] getActionItems(Activity activity);
	public Drawable getProviderIcon();
	
	public boolean showControls();
	
}
