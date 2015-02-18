package org.javenstudio.provider.app.picasa;

import java.util.ArrayList;
import java.util.List;

import android.graphics.BitmapRegionDecoder;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.data.DataAction;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.common.util.Logger;

public final class PicasaAlbum extends PicasaMediaSet {
	private static final Logger LOG = Logger.getLogger(PicasaAlbum.class);

	private final List<MediaItem> mMediaItems = new ArrayList<MediaItem>();
	
	private final PicasaSource mSource;
	private final GAlbumEntry mEntry;
	private final HttpImage mImage;
	private final String mAvatarLocation;
	private final int mIconRes;
	
	public PicasaAlbum(PicasaSource source, DataPath path, 
			GAlbumEntry album, String avatarURL, int iconRes) { 
		super(path, nextVersionNumber());
		mImage = HttpResource.getInstance().getImage(album.mediaUrl);
		mSource = source;
		mEntry = album;
		mAvatarLocation = avatarURL;
		mIconRes = iconRes;
	}
	
	public final PicasaSource getSource() { return mSource; }
	public final GAlbumEntry getAlbumEntry() { return mEntry; }
	
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
        return mEntry.numphotos; //mMediaItems.size();
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
	
	public String getAuthor() { return mEntry.authorName; }
	public String getUserId() { return mEntry.user; }
	public String getAlbumId() { return mEntry.albumId; }
	public String getAvatarLocation() { return mAvatarLocation; }
	
	@Override
    public boolean delete() throws DataException {
		SystemUser account = getSource().getAccount();
		if (account == null)
			throw new UnsupportedOperationException();
		
		if (getItemCount() != 0) {
			throw new DataException(DataException.CODE_ALBUM_NOTEMPTY, "Album: " + getName() 
					+ " isn't empty, cannot delete");
		}
		
		final String accountName = account.getAccountName();
		final String albumId = getAlbumId();
		
		try {
			boolean result = PicasaDeleter.deleteAlbum(getSource().getDataApp().getContext(), 
					account, albumId);
			
			if (result) { 
				ContentHelper.getInstance().updateFetchDirtyWithAccount(accountName);
			}
			
			return result;
		} catch (HttpException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("delete: failed: " + e + ", account=" + accountName 
						+ " albumId=" + albumId);
			}
			
			DataException ex = new DataException(e.getStatusCode(), e.getMessage(), e);
			ex.setAction(DataAction.DELETE);
			throw ex;
		}
		
		//return false;
    }
	
}
