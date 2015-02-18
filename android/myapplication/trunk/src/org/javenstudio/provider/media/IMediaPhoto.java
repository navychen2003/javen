package org.javenstudio.provider.media;

import android.net.Uri;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.provider.OnClickListener;

public interface IMediaPhoto {

	public String getName();
	
	public Photo getPhotoData();
	public MediaInfo getPhotoInfo();
	
	public Uri getContentUri();
	public Uri getPlayUri();
	
	public Image getBitmapImage();
	public Image getAvatarImage();
	
	public void setBitmapVisible(boolean visible);
	
	public OnClickListener getTitleClickListener();
	//public int getSupportedOperations();
	
}
