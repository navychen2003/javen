package org.javenstudio.android.data.media.download;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.util.Utilities;

public abstract class DownloadMediaItem extends MediaItem 
		implements MediaInfo, Photo {

	public long fileSize;
	public long dateTakenInMs;
    public int width;
    public int height;
    
	public DownloadMediaItem(DataPath path, long version) { 
		super(path, version);
	}
	
	public abstract Image getBitmapImage();
	public Image getAvatarImage() { return null; }
	public String getAvatarLocation() { return null; }
	
    @Override
    public MediaInfo getMediaInfo() { 
    	return this;
    }
    
    @Override
    public Uri getContentUri() { 
    	return getBitmapImage().getContentUri();
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
    	ContentHelper.getInstance().updateFetchDirtyWithPrefix(DownloadMediaSource.PREFIX);
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
	
    @Override
    public String getMimeType() { return "image/*"; }

    @Override
    public long getSize() { return fileSize; }
	
    @Override
    public long getDateInMs() { return dateTakenInMs; }
    
	@Override
	public int getWidth() { return width; }

	@Override
	public int getHeight() { return height; }
	
	@Override
	public boolean isLocalItem() { return true; }
	
}
