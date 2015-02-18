package org.javenstudio.provider.media;

import android.net.Uri;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.provider.OnClickListener;

public class MediaPhoto implements IMediaPhoto {

	private final Photo mPhotoData;
	private OnClickListener mTitleClickListener = null;
	
	MediaPhoto(Photo photo) { 
		if (photo == null) throw new NullPointerException();
		mPhotoData = photo;
	}
	
	public String getName() { return getPhotoData().getName(); }
	public Photo getPhotoData() { return mPhotoData; }
	public MediaInfo getPhotoInfo() { return getPhotoData().getMediaInfo(); }
	
	public Image getBitmapImage() { return getPhotoData().getBitmapImage(); }
	public Image getAvatarImage() { return getPhotoData().getAvatarImage(); }
	
	//@Override
	//public int getSupportedOperations() { 
	//	return getPhotoData().getSupportedOperations();
	//}
	
	@Override
	public Uri getContentUri() { 
		return getPhotoData().getContentUri();
	}
	
	@Override
	public Uri getPlayUri() { 
		return getPhotoData().getPlayUri();
	}
	
	@Override
	public void setBitmapVisible(boolean visible) {
		getPhotoData().getBitmapImage().setBitmapVisible(visible);
		
		Image image = getPhotoData().getAvatarImage();
		if (image != null) 
			image.setBitmapVisible(visible);
	}
	
	public void setTitleClickListener(OnClickListener listener) { 
		mTitleClickListener = listener;
	}
	
	@Override
	public OnClickListener getTitleClickListener() { 
		return mTitleClickListener;
	}
	
}
