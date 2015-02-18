package org.javenstudio.android.information;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;

public class InformationListBinderPhoto extends InformationListBinder {

	public InformationListBinderPhoto() {}
	
	@Override
	protected int getColumnSize(IActivity activity) { 
		final int colsize = activity.getResources().getInteger(R.integer.photo_column_size);
		return colsize > 1 ? colsize : 1;
	}
	
	@Override
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.photo_space_size);
	}
	
	@Override
	public View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) {
		return inflater.inflate(R.layout.information_list_photo, container, false);
	}
	
	@Override
	public View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.information_list_listview);
	}
	
	@Override
	protected int getItemViewHeight(IActivity activity, ViewGroup container, View view) { 
		//if (getColumnSize(activity) > 1) 
		//	return (int)activity.getResources().getDimension(R.dimen.photo_item_height);
		
		return getDefaultItemViewHeight(activity, container, view);
	}
	
	@Override
	protected void setImageViewSize(IActivity activity, InformationOne one, View view) { 
		int thumbWidth = (int)activity.getResources().getDimension(R.dimen.photo_item_gridthumb_width);
		int thumbHeight = (int)activity.getResources().getDimension(R.dimen.photo_item_gridthumb_height);
		
		one.setImageViewWidth(thumbWidth);
		one.setImageViewHeight(thumbHeight);
	}
	
	@Override
	protected int getInformationItemViewRes(IActivity activity) { 
		return R.layout.information_item_photo_small; 
	}
	
	@Override
	protected int getInformationHeaderDimenRes(IActivity activity) { 
		return R.dimen.photo_item_gridthumb_height; 
	}
	
	protected int getInformationItemHeaderViewId() { return R.id.information_item_header; }
	protected int getInformationItemOverlayViewId() { return R.id.information_item_overlay; }
	protected int getInformationItemImageViewId() { return R.id.information_item_image; }
	protected int getInformationItemProgressViewId() { return R.id.information_item_progress; }
	
}
