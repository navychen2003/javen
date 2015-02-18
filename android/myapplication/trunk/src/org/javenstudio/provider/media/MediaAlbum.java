package org.javenstudio.provider.media;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.android.data.media.PhotoSet;

public class MediaAlbum implements IMediaAlbum {

	private final PhotoSet mPhotoSet;
	private final MediaItemSet<Photo> mPhotos;
	
	MediaAlbum(PhotoSet photoSet) { 
		if (photoSet == null) throw new NullPointerException();
		mPhotoSet = photoSet;
		mPhotos = new MediaItemSet<Photo>(100, new Comparator<Photo>() {
				@Override
				public int compare(Photo lhs, Photo rhs) {
					long lm = lhs.getDateInMs();
					long rm = rhs.getDateInMs();
					return lm == rm ? (lhs.getLocation().compareTo(rhs.getLocation())) 
							: (lm < rm ? 1 : -1);
				}
			});
	}
	
	public final PhotoSet getPhotoSet() { return mPhotoSet; }
	public final MediaInfo getAlbumInfo() { return mPhotoSet.getMediaInfo(); }
	
	@Override
	public String getName() { 
		return getPhotoSet().getName();
	}
	
	@Override
	public int getPhotoCount() { 
		return getPhotoSet().getItemCount(); 
	}
	
	@Override
	public Image[] getAlbumImages(int count) { 
		Image[] images = mPhotoSet.getAlbumImages(count);
		if (images != null) 
			return images;
		
		return getBitmapImages(0, count);
	}
	
	@Override
	public Image[] getAllImages() { 
		ArrayList<Image> images = new ArrayList<Image>();
		
		Image[] imgs = getBitmapImages(0, getPhotoCount());
		if (imgs != null) {
			for (int i=0; i < imgs.length; i++) { 
				Image img = imgs[i];
				if (img != null) images.add(img);
			}
		}
		
		return images.toArray(new Image[images.size()]);
	}
	
	@Override
	public Image[] getBitmapImages(int start, int count) { 
		List<MediaItem> items = getPhotoSet().getItemList(start, count);
		if (items == null || items.size() <= 0) 
			return null;
		
		ArrayList<Image> images = new ArrayList<Image>();
		
		for (int i=0; i < items.size(); i++) { 
			MediaItem item = items.get(i);
			if (item != null && item instanceof Photo) { 
				Photo photo = (Photo)item;
				images.add(photo.getBitmapImage());
				mPhotos.add(photo);
			}
		}
		
		return images.toArray(new Image[images.size()]);
	}
	
	@Override
	public Photo[] getAllPhotos() { 
		List<MediaItem> items = getPhotoSet().getItemList(0, getPhotoCount());
		if (items == null || items.size() <= 0) 
			return null;
		
		ArrayList<Photo> photos = new ArrayList<Photo>();
		
		for (int i=0; i < items.size(); i++) { 
			MediaItem item = items.get(i);
			if (item != null && item instanceof Photo) { 
				Photo photo = (Photo)item;
				photos.add(photo);
				mPhotos.add(photo);
			}
		}
		
		return photos.toArray(new Photo[photos.size()]);
	}
	
	@Override
	public synchronized void setBitmapVisible(boolean visible) { 
		for (Photo item : mPhotos) { 
			if (item != null)
				item.getBitmapImage().setBitmapVisible(visible);
		}
	}
	
}
