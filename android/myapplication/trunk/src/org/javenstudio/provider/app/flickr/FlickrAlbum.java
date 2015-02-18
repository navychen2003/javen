package org.javenstudio.provider.app.flickr;

import java.util.ArrayList;
import java.util.List;

import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;

final class FlickrAlbum extends FlickrMediaSet {

	private final List<MediaItem> mMediaItems = new ArrayList<MediaItem>();
	
	private final FlickrSource mSource;
	private final YPhotoSetEntry mEntry;
	private final HttpImage mImage;
	private final int mIconRes;
	
	public FlickrAlbum(FlickrSource source, DataPath path, 
			YPhotoSetEntry entry, int iconRes) { 
		super(path, nextVersionNumber());
		mImage = HttpResource.getInstance().getImage(FlickrHelper.getPhotoURL(entry));
		mSource = source;
		mEntry = entry;
		mIconRes = iconRes;
	}
	
	public final FlickrSource getSource() { return mSource; }
	public final YPhotoSetEntry getAlbumEntry() { return mEntry; }
	
	@Override
	public final Image[] getAlbumImages(int count) { 
		return new Image[] { mImage }; 
	}
	
	@Override
	public Drawable getProviderIcon() { 
		int iconRes = mIconRes; 
		if (iconRes != 0) 
			return ResourceHelper.getResources().getDrawable(iconRes);
		return null;
	}
	
	@Override
	public String getName() {
		return mEntry.title;
	}
	
	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
	@Override
    public synchronized int getItemCount() {
        return mEntry.countPhotos + mEntry.countVideos; //mMediaItems.size();
    }
	
	@Override
    public synchronized List<MediaItem> getItemList(int start, int count) {
        List<MediaItem> result = new ArrayList<MediaItem>();
        if (start < 0 || start >= mMediaItems.size() || count <= 0) 
        	return result;
        
        int end = start + count;
        for (int i=start; i < end && i < mMediaItems.size(); i++) { 
        	MediaItem item = mMediaItems.get(i);
        	result.add(item);
        }
        
        return result;
    }
	
	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
		return mImage.requestImage(holder, type);
	}

	public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
		return mImage.requestLargeImage(holder);
	}
	
	public String getAuthor() { return null; }
	public String getUserId() { return null; }
	public String getAlbumId() { return mEntry.photosetId; }
	
}
