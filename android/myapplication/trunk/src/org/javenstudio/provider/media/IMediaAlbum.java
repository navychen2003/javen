package org.javenstudio.provider.media;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.android.data.media.PhotoSet;

public interface IMediaAlbum {

	public String getName();
	public MediaInfo getAlbumInfo();
	
	public int getPhotoCount();
	public PhotoSet getPhotoSet();
	
	public Image[] getAllImages();
	public Photo[] getAllPhotos();
	
	public Image[] getAlbumImages(int count);
	public Image[] getBitmapImages(int start, int count);
	
	//public void registerListener(ImageListener listener);
	//public boolean hasImage(String location);
	
	public void setBitmapVisible(boolean visible);
	
}
