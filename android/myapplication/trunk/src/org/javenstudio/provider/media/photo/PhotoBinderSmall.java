package org.javenstudio.provider.media.photo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;

public abstract class PhotoBinderSmall extends PhotoBinder {

	@Override
	protected int getColumnSize(IActivity activity) { 
		final int size = activity.getResources().getInteger(R.integer.photo_column_size);
		return size > 1 ? size : 1;
	}
	
	@Override
	protected float getColumnSpace(IActivity activity) { 
		return activity.getResources().getDimension(R.dimen.photo_space_size);
	}
	
	@Override
	protected View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.provider_list_photo, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	protected int getPhotoItemViewRes() { return R.layout.photo_item_small; }
	protected int getPhotoItemHeaderViewId() { return R.id.photo_item_header; }
	protected int getPhotoItemHeaderHeightId() { return R.dimen.photo_item_gridthumb_width; }
	protected int getPhotoItemImageViewId() { return R.id.photo_item_image; }
	protected int getPhotoItemAvatarViewId() { return R.id.photo_item_user_avatar; }
	protected int getPhotoItemOverlayViewId() { return R.id.photo_item_overlay; }
	protected int getPhotoItemProgressViewId() { return R.id.photo_item_progress; }
	protected int getPhotoItemSelectViewId() { return R.id.photo_item_select; }
	
	@Override
	protected int getPhotoItemSelectedDrawableRes(boolean selected) { 
		return AppResources.getInstance().getSmallSelectedDrawableRes(selected);
	}
	
	@Override
	protected void setImageViewSize(IActivity activity, PhotoItem item, View view) { 
		int thumbWidth = (int)activity.getResources().getDimension(R.dimen.photo_item_gridthumb_width);
		int thumbHeight = (int)activity.getResources().getDimension(R.dimen.photo_item_gridthumb_height);
		
		item.setImageViewWidth(thumbWidth);
		item.setImageViewHeight(thumbHeight);
	}
	
	@Override
	protected void bindPhotoText(final IActivity activity, final PhotoItem item, View view) { 
		//TextView titleView = (TextView)view.findViewById(R.id.photo_item_title);
		//titleView.setText(item.getImage().getTitle());
		//titleView.setLines(2);
		
		//TextView textView = (TextView)view.findViewById(R.id.photo_item_text);
		//textView.setVisibility(View.GONE);
		
		//TextView dateView = (TextView)view.findViewById(R.id.photo_item_date);
		//dateView.setText(item.getImage().getAuthor());
	}
	
}
