package org.javenstudio.android.data.media.local;

import java.text.DateFormat;
import java.util.Date;

import android.database.Cursor;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.Utilities;

public abstract class LocalMediaItem extends MediaItem 
		implements MediaInfo, Photo {

    // database fields
    public int id;
    public String caption;
    public String mimeType;
    public long fileSize;
    public double latitude = MediaUtils.INVALID_LATLNG;
    public double longitude = MediaUtils.INVALID_LATLNG;
    public long dateTakenInMs;
    public long dateAddedInSec;
    public long dateModifiedInSec;
    public String filePath;
    public int bucketId;
    public int width;
    public int height;
	
	public LocalMediaItem(DataPath path, long version) { 
		super(path, version);
	}
	
	public abstract Image getBitmapImage();
	public Image getAvatarImage() { return null; }
	public String getAvatarLocation() { return null; }
	
    @Override
    public long getDateInMs() {
        return dateTakenInMs;
    }

    @Override
    public String getName() {
        return caption;
    }

    @Override
    public String getLocation() { 
    	if (filePath != null) return filePath;
    	return super.getLocation();
    }
    
    public void getLatLong(double[] latLong) {
        latLong[0] = latitude;
        latLong[1] = longitude;
    }

    protected abstract boolean updateFromCursor(Cursor cursor);

    public int getBucketId() {
        return bucketId;
    }

    protected void updateContent(Cursor cursor) {
        if (updateFromCursor(cursor)) 
            mDataVersion = nextVersionNumber();
    }

    @Override
    public MediaInfo getMediaInfo() { 
    	return this;
    }
	
	public String getTitle() { return getName(); }
	public String getAuthor() { return null; }
	
	public String getUserId() { return null; }
	public String getAlbumId() { return null; }
	public String getPhotoId() { return null; }
	
	public int getStatisticCount(int type) { return -1; }
	public IMediaComments getComments(LoadCallback callback) { return null; }
	public Drawable getProviderIcon() { return null; }
	
    @Override
    public void notifyDirty() { 
    	ContentHelper.getInstance().updateFetchDirtyWithPrefix(LocalMediaSource.PREFIX);
    }
	
	@Override
	public void getDetails(IMediaDetails details) { 
		getBitmapImage().getDetails(details);
	}
    
	@Override
	public void getExifs(IMediaDetails details) { 
		getBitmapImage().getExifs(details);
	}
	
	@Override
	public String getShareText() { 
		return getBitmapImage().getShareText();
	}
	
	@Override
	public String getShareType() { 
		return getBitmapImage().getShareType();
	}
	
	@Override
	public String getSubTitle() { 
		if (width > 0 && height > 0) 
			return "" + width + " x " + height;
		if (fileSize > 0) 
			return Utilities.formatSize(fileSize);
		return null; 
	}
	
    public MediaDetails getMediaDetails() {
        MediaDetails details = new MediaDetails();
        
        details.addDetail(MediaDetails.INDEX_PATH, filePath);
        details.addDetail(MediaDetails.INDEX_TITLE, caption);
        
        DateFormat formater = DateFormat.getDateTimeInstance();
        details.addDetail(MediaDetails.INDEX_DATETIME,
                formater.format(new Date(dateModifiedInSec * 1000)));
        
        details.addDetail(MediaDetails.INDEX_WIDTH, width);
        details.addDetail(MediaDetails.INDEX_HEIGHT, height);

        if (MediaUtils.isValidLocation(latitude, longitude)) 
            details.addDetail(MediaDetails.INDEX_LOCATION, new double[] {latitude, longitude});
        
        if (fileSize > 0) 
        	details.addDetail(MediaDetails.INDEX_SIZE, fileSize);
        
        return details;
    }

    @Override
    public String getMimeType() { return mimeType; }

    @Override
    public long getSize() { return fileSize; }
    
    @Override
    public int getWidth() { return width; }
    
    @Override
    public int getHeight() { return height; }

    public String getFilePath() { return filePath; }
    
	@Override
	public boolean isLocalItem() { return true; }
    
}
