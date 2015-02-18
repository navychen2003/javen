package org.javenstudio.android.data.media.download;

import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.SourceHelper;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.common.util.Logger;

final class DownloadAlbum extends DownloadMediaSet {
	private static final Logger LOG = Logger.getLogger(DownloadAlbum.class);
	
	private final List<MediaItem> mMediaItems = new ArrayList<MediaItem>();
	
	private final DownloadMediaSource mSource;
	private final DownloadAlbumSet mAlbumSet;
	private final String mName;
	private final String mSourceName;
	
	private long mLastModified = 0;
	
	public DownloadAlbum(DownloadMediaSource source, 
			DownloadAlbumSet albumSet, DataPath path, String name, 
			String sourceName, long lastModified) { 
		super(path, nextVersionNumber());
		mSource = source;
		mAlbumSet = albumSet;
		mName = name;
		mSourceName = sourceName;
		mLastModified = lastModified;
	}
	
	public final DownloadMediaSource getMediaSource() { return mSource; }
	public final DownloadAlbumSet getAlbumSet() { return mAlbumSet; }
	public final String getSourceName() { return mSourceName; }
	
	@Override
	public boolean isDeleteEnabled() { return true; }
	
	@Override
	public final long getDateInMs() { 
		return mLastModified; 
	}
	
	@Override
	public String getName() {
		return mName;
	}
	
	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
	@Override
    public synchronized int getItemCount() {
        return mMediaItems.size();
    }
	
	@Override
	public synchronized long reloadData(ReloadCallback callback, ReloadType type) { 
		boolean dirty = isDirty() && !callback.isActionProcessing();
		if (dirty || type == ReloadType.FORCE) { 
			if (LOG.isDebugEnabled())
				LOG.debug("reloadData: type=" + type + " dirty=" + dirty);
			
			mDataVersion = nextVersionNumber();
			setDirty(false);
		}
		return mDataVersion; 
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
	
	synchronized void addMediaItem(MediaItem item, long lastModified) { 
		if (item != null) {
			mMediaItems.add(item);
			
			if (lastModified > mLastModified) 
				mLastModified = lastModified;
		}
	}
	
	synchronized boolean removeMediaItem(MediaItem item) { 
		if (item == null) return false;
		boolean removed = false;
		
		for (int i=0; i < mMediaItems.size(); ) {
			MediaItem mi = mMediaItems.get(i);
			if (mi != null && mi.equals(item)) { 
				mMediaItems.remove(i);
				removed = true;
				continue;
			}
			i++;
		}
		
		if (removed) { 
			setDirty(true);
			
			if (LOG.isDebugEnabled())
				LOG.debug("removeMediaItem: item=" + item);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public Drawable getProviderIcon() { 
		return SourceHelper.getSourceIcon(getSourceName());
	}
	
}
