package org.javenstudio.provider.app.picasa;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.comment.IMediaComments;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.android.data.media.MediaInfo;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.data.LoadCallback;
import org.javenstudio.cocoka.util.MediaUtils;

public abstract class PicasaMediaSet extends MediaSet 
		implements MediaInfo, PhotoSet, AlbumSet {

	private PicasaUserClickListener mUserClickListener = null;
	private boolean mDirty = false;
	
	public PicasaMediaSet(DataPath path, long version) { 
		super(path, version);
	}
	
    @Override
    public MediaInfo getMediaInfo() { 
    	return this;
    }
    
    public void setUserClickListener(PicasaUserClickListener listener) { 
    	mUserClickListener = listener;
    }
    
    public PicasaUserClickListener getUserClickListener() { 
    	return mUserClickListener;
    }
	
    public void notifyDirty() { mDirty = true; }
    synchronized void setDirty(boolean dirty) { mDirty = dirty; }
    
    @Override
    public SelectAction getSelectAction() { return null; }
    
    public boolean isDirty() { return mDirty; }
	public boolean isSearchable() { return false; }
	public boolean isDeleteEnabled() { return false; }
	
	public String getShareText() { return null; }
	public String getShareType() { return MediaUtils.MIME_TYPE_ALL; }
	
	public String getSearchText() { return null; }
	public String[] getPhotoTags() { return null; }
	
	public final String getPath() { return getDataPath().toString(); }
	public final MediaSet getMediaSet() { return this; }
	
	public MediaSet[] getMediaSets() { return new MediaSet[] { this }; }
	public Image[] getAlbumImages(int count) { return null; }
	
	public int getMediaType() { return 0; }
	
	public String getTitle() { return getName(); }
	public String getSubTitle() { return null; }
	public String getAuthor() { return null; }
	
	public String getUserId() { return null; }
	public String getAlbumId() { return null; }
	public String getPhotoId() { return null; }
	
	public String getAvatarLocation() { return null; }
	
	public long getDateInMs() { return 0; }
	public int getStatisticCount(int type) { return -1; }
	
	public void getDetails(IMediaDetails details) {}
	public void getExifs(IMediaDetails details) {}
	
	public IMediaComments getComments(LoadCallback callback) { return null; }
	public Drawable getProviderIcon() { return null; }
	
}
