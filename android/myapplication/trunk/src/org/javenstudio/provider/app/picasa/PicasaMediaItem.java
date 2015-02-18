package org.javenstudio.provider.app.picasa;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.data.MediaHelper;

public abstract class PicasaMediaItem extends MediaItem 
		implements MediaInfo, Photo {

	public PicasaMediaItem(DataPath path, long version) { 
		super(path, version);
	}
	
	public abstract Image getBitmapImage();
	public Image getAvatarImage() { return null; }
	public String getAvatarLocation() { return null; }

    @Override
    public MediaInfo getMediaInfo() { 
    	return this;
    }
    
    //@Override
    //public Uri getContentUri() { 
    //	return getBitmapImage().getContentUri();
    //}
    
	public String getTitle() { return getName(); }
	public String getSubTitle() { return null; }
	public String getAuthor() { return null; }
	
	public String getUserId() { return null; }
	public String getAlbumId() { return null; }
	public String getPhotoId() { return null; }
	
	public int getStatisticCount(int type) { return -1; }
	public IMediaComments getComments(LoadCallback callback) { return null; }
	public Drawable getProviderIcon() { return null; }
	
	public void notifyDirty() {}
	
	@Override
	public void getDetails(IMediaDetails details) { 
		getBitmapImage().getDetails(details);
	}
	
	@Override
	public void getExifs(IMediaDetails details) { 
		getBitmapImage().getExifs(details);
	}

    @Override
    public String getMimeType() { return "image/*"; }

    @Override
    public long getDateInMs() { return 0; }
    
    @Override
    public long getSize() { return 0; }
	
	@Override
	public int getWidth() { return 0; }

	@Override
	public int getHeight() { return 0; }
	
	@Override
	public String getShareText() { 
		AppResources.Fields fields = new AppResources.Fields();
		fields.addField("title", getTitle());
		//fields.addField("content", text);
		fields.addField("author", getAuthor());
		//fields.addField("date", date);
		//fields.addField("link", link);
		fields.addField("source", "Picasa");
		
		return AppResources.getInstance().getShareInformation(fields);
	}
	
	@Override
	public String getShareType() { 
		return MediaHelper.getMimeType(getMediaType());
	}
	
}
