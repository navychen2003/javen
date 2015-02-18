package org.javenstudio.cocoka.widget;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.cocoka.android.ResourceHelper;

public class ViewHelper {

	public static void setBackgroundResource(View view, int resId) {
		if (view != null && resId != 0) 
			view.setBackgroundDrawable(ResourceHelper.getResourceContext().getDrawable(resId)); 
	}
	
	public static void setImageResource(ImageView view, int resId) { 
		if (view != null && resId != 0) 
			view.setImageDrawable(ResourceHelper.getResourceContext().getDrawable(resId)); 
	}
	
	public static void setText(TextView view, int resId) { 
		if (view != null && resId != 0) 
			view.setText(ResourceHelper.getResourceContext().getString(resId)); 
	}
	
}
