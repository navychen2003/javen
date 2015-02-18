package org.javenstudio.provider.app.flickr;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.provider.media.MediaPhotoBinderLarge;
import org.javenstudio.provider.media.MediaPhotoSource;
import org.javenstudio.provider.media.photo.PhotoItem;

final class FlickrPhotoBinderLarge extends MediaPhotoBinderLarge {

	public FlickrPhotoBinderLarge(MediaPhotoSource source) { 
		super(source);
	}
	
	@Override
	protected void bindPhotoText(final IActivity activity, final PhotoItem item, View view) { 
		//onUpdateText(activity.getActivity(), item);
	}
	
	protected void onUpdateText(PhotoItem item) {
		final View view = item != null ? item.getBindView() : null;
		if (view == null) return;
		
		FlickrPhotoBase photo = (FlickrPhotoBase)item.getPhoto().getPhotoData();
		MediaInfo info = item.getPhoto().getPhotoInfo();
		int commentCount = info.getStatisticCount(MediaInfo.COUNT_COMMENT);
		
		TextView titleView = (TextView)view.findViewById(R.id.photo_item_title);
		if (titleView != null) { 
			titleView.setText(info.getTitle());
			titleView.setLines(2);
			titleView.setVisibility(View.VISIBLE);
		}
		
		TextView textView = (TextView)view.findViewById(R.id.photo_item_text);
		if (textView != null)
			textView.setVisibility(View.GONE);
		
		TextView dateView = (TextView)view.findViewById(R.id.photo_item_date);
		if (dateView != null) { 
			dateView.setText(photo.getPhotoDate());
			dateView.setVisibility(View.VISIBLE);
		}
		
		View userView = view.findViewById(R.id.photo_item_user);
		if (userView != null) 
			userView.setVisibility(View.VISIBLE);
		
		TextView usernameView = (TextView)view.findViewById(R.id.photo_item_user_name);
		if (usernameView != null) { 
			usernameView.setText(photo.getAuthor());
			usernameView.setVisibility(View.VISIBLE);
		}
		
		TextView commentView = (TextView)view.findViewById(R.id.photo_item_comment);
		if (commentView != null) { 
			if (commentCount >= 0) {
				commentView.setText(""+commentCount);
				commentView.setVisibility(View.VISIBLE);
			} else
				commentView.setVisibility(View.GONE);
		}
		
		ImageView logoView = (ImageView)view.findViewById(R.id.photo_item_logo);
		if (logoView != null) { 
			logoView.setImageDrawable(info.getProviderIcon());
			logoView.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	protected void onUpdateAvatar(PhotoItem item) { 
		final View view = item != null ? item.getBindView() : null;
		if (view == null) return;
		
		final ImageView avatarView = (ImageView)view.findViewById(R.id.photo_item_user_avatar);
		if (avatarView != null) { 
			Image avatarImage = item.getPhoto().getAvatarImage();
			if (avatarImage != null) { 
				Drawable d = avatarImage.getThumbnailDrawable(100, 100);
				avatarView.setImageDrawable(d);
			}
		}
		
		onUpdateText(item);
	}
	
	@Override
	protected void requestDownload(IActivity activity, final PhotoItem item) {
		super.requestDownload(activity, item);
		
		final FlickrPhotoBase photo = (FlickrPhotoBase)item.getPhoto().getPhotoData();
		YPhotoInfoEntry info = photo.getPhotoInfoEntry();
		if (info == null) { 
			// disable other request
			photo.setPhotoInfoEntry(new YPhotoInfoEntry());
			
			FlickrHelper.fetchPhotoInfo(photo.getPhotoId(), 
				new YPhotoInfoEntry.FetchListener() {
					public void onPhotoInfoFetching(String source) {}
					@Override
					public void onPhotoInfoFetched(YPhotoInfoEntry entry) {
						if (entry != null && entry.photoId != null && 
							entry.photoId.length() > 0) {
							photo.setPhotoInfoEntry(entry);
							item.setImageListener();
							item.postUpdateViews();
							requestAvatar(item);
						}
					}
				}, true);
		} else { 
			requestAvatar(item);
		}
	}
	
	protected void requestAvatar(final PhotoItem item) {
		final Image avatarImage = item.getPhoto().getAvatarImage();
		if (avatarImage != null && avatarImage instanceof HttpImage) 
			requestDownload((HttpImage)avatarImage);
	}
	
}
