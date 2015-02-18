package org.javenstudio.provider.media.album;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;

public abstract class AlbumBinderList extends AlbumBinder {

	@Override
	protected int getColumnSize(IActivity activity) { 
		int size = activity.getResources().getInteger(R.integer.list_column_size);
		return size > 1 ? size : 1;
	}
	
	@Override
	protected View inflateView(IActivity activity, LayoutInflater inflater, ViewGroup container) { 
		return inflater.inflate(R.layout.provider_list, container, false);
	}
	
	@Override
	protected View findListView(IActivity activity, View rootView) { 
		return rootView.findViewById(R.id.provider_list_listview);
	}
	
	protected int getAlbumItemViewRes() { return R.layout.album_item_list; }
	protected int getAlbumItemHeaderViewId() { return R.id.album_item_thumbnail; }
	protected int getAlbumItemHeaderHeightId() { return R.dimen.album_item_header_height; }
	protected int getAlbumItemPhotoSpaceId() { return R.dimen.album_item_photo_space_list; }
	protected int getAlbumItemImageViewId() { return R.id.album_item_thumbnail; }
	protected int getAlbumItemOverlayViewId() { return R.id.album_item_overlay; }
	protected int getAlbumItemProgressViewId() { return R.id.album_item_progress; }
	protected int getAlbumItemSelectViewId() { return R.id.album_item_select; }
	
	@Override
	protected int getAlbumItemSelectedDrawableRes(boolean selected) { 
		return AppResources.getInstance().getSmallSelectedDrawableRes(selected);
	}
	
	@Override
	protected void setImageViewSize(IActivity activity, AlbumItem item, View view) { 
		float photoSpace = (float)activity.getResources().getDimension(getAlbumItemPhotoSpaceId());
		int thumbWidth = (int)activity.getResources().getDimension(R.dimen.album_item_listthumb_width);
		int thumbHeight = (int)activity.getResources().getDimension(R.dimen.album_item_listthumb_height);
		
		item.setImageViewWidth(thumbWidth);
		item.setImageViewHeight(thumbHeight);
		item.setPhotoSpace(photoSpace);
	}
	
	@Override
	protected void bindAlbumText(final IActivity activity, final AlbumItem item, View view) { 
		//String title = item.getAlbum().getAlbumTitle();
		//String info = String.format(activity.getResources().getString(R.string.label_album_info), 
		//		item.getAlbum().getImageCount());
		
		//TextView titleView = (TextView)view.findViewById(R.id.album_item_title);
		//titleView.setText(title);
		//titleView.setLines(1);
		
		//TextView textView = (TextView)view.findViewById(R.id.album_item_text);
		//textView.setVisibility(View.GONE);
		
		//TextView dateView = (TextView)view.findViewById(R.id.album_item_date);
		//dateView.setText(info);
	}
	
}
