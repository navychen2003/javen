package org.javenstudio.android.data.media.download;

import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.DataPathMatcher;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.data.media.MediaObject;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.data.media.MediaSource;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.cocoka.storage.fs.IFile;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.provider.media.MediaAction;

public final class DownloadMediaSource extends MediaSource {
	//private static final Logger LOG = Logger.getLogger(DownloadMediaSource.class);
	
	public static final String PREFIX = "download";
	public static final String TOP_DOWNLOAD_IMAGE_SET_PATH = "/download/image";
	
	private static final int DOWNLOAD_IMAGE_ALBUMSET = 1;
	private static final int DOWNLOAD_IMAGE_ALBUM = 2;
	private static final int DOWNLOAD_IMAGE_ITEM = 3;
	
	private final DataApp mApplication;
	private final HttpResource mImageResource;
	private final DataPathMatcher mMatcher;
	private final String mRootPath;
	
	//private final Set<DownloadMediaSet> mMediaSets;
	private final SelectAction mSelectAction;
	private boolean mDirty = false;
	
	public DownloadMediaSource(DataApp app) { 
		super(PREFIX);
		mApplication = app;
		mImageResource = app.getHttpResource();
		//mMediaSets = new HashSet<DownloadMediaSet>();
		mSelectAction = new MediaAction.LocalAlbumSelectAction();
		
		String storagePath = mImageResource.getFetchCache().getPath();
		if (!storagePath.endsWith("/")) storagePath += "/";
		mRootPath = storagePath;
		
		mMatcher = new DataPathMatcher();
		mMatcher.add("/download/image", DOWNLOAD_IMAGE_ALBUMSET);
		mMatcher.add("/download/image/*", DOWNLOAD_IMAGE_ALBUM);
		mMatcher.add("/download/image/item/*", DOWNLOAD_IMAGE_ITEM);
	}

	public final DataApp getDataApp() { 
		return mApplication;
	}
	
	public final HttpResource getImageResource() { 
		return mImageResource;
	}
	
	@Override
	public String[] getTopSetPaths() { 
    	return new String[] { 
    			TOP_DOWNLOAD_IMAGE_SET_PATH
    		};
	}
	
	@Override
	public boolean isDeleteEnabled() { return true; }
	
    @Override
    public synchronized void notifyDirty() { 
    	ContentHelper.getInstance().updateFetchDirtyWithPrefix(DownloadMediaSource.PREFIX);
    	mDirty = true;
    }
	
    @Override
    public synchronized boolean isDirty() { 
    	//for (DownloadMediaSet mediaSet : mMediaSets) { 
    	//	if (mediaSet != null && mediaSet.isDirty()) 
    	//		return true;
    	//}
    	return mDirty; 
    }
    
    synchronized void setDirty(boolean dirty) { mDirty = dirty; }
    
    @Override
    public SelectAction getSelectAction() { return mSelectAction; }
    
    @Override
	public synchronized MediaSet[] getMediaSets() { 
    	mDirty = false;
    	return super.getMediaSets();
    }
    
	@Override
	protected synchronized MediaObject createMediaObject(DataPath path) {
		switch (mMatcher.match(path)) {
		case DOWNLOAD_IMAGE_ALBUMSET: {
			String storagePath = mRootPath;
			DownloadAlbumSet albumSet = new DownloadAlbumSet(this, path, storagePath);
			//mMediaSets.add(albumSet);
			return albumSet;
		}
		default:
            throw new RuntimeException("bad path: " + path);
		}
	}
	
	DownloadAlbum newAlbum(DownloadAlbumSet albumSet, 
			String filepath, String name, String sourceName) { 
		DataPath path = DataPath.fromString("/download/image/" 
				+ Utilities.toMD5(filepath + "+" + name));
		return new DownloadAlbum(this, albumSet, path, name, sourceName, 0);
	}
	
	DownloadAlbum newAlbum(DownloadAlbumSet albumSet, 
			String pathString, String name, String sourceName, long lastModified) { 
		DataPath path = DataPath.fromString(pathString);
		return new DownloadAlbum(this, albumSet, path, name, sourceName, lastModified);
	}
	
	DownloadImage newImage(DownloadAlbumSet albumSet, 
			IFile file, String sourceName) { 
		DataPath path = DataPath.fromString("/download/image/item/" 
				+ Utilities.toMD5(file.getPath()));
		return new DownloadImage(this, albumSet, path, file, sourceName);
	}
	
}
