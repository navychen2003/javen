package org.javenstudio.android.data.media.local;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import android.content.UriMatcher;
import android.provider.MediaStore;

import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.DataPathMatcher;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.android.data.media.MediaObject;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.android.data.media.MediaSource;
import org.javenstudio.android.entitydb.content.ContentHelper;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.provider.media.MediaAction;

public final class LocalMediaSource extends MediaSource {

	public static final String PREFIX = "local";
	public static final String KEY_BUCKET_ID = "bucketId";
	
    private static final int LOCAL_IMAGE_ALBUMSET = 0;
    private static final int LOCAL_VIDEO_ALBUMSET = 1;
    private static final int LOCAL_IMAGE_ALBUM = 2;
    private static final int LOCAL_VIDEO_ALBUM = 3;
    private static final int LOCAL_IMAGE_ITEM = 4;
    private static final int LOCAL_VIDEO_ITEM = 5;
    private static final int LOCAL_ALL_ALBUMSET = 6;
    private static final int LOCAL_ALL_ALBUM = 7;
	
	private static final int NO_MATCH = -1;
    private final UriMatcher mUriMatcher = new UriMatcher(NO_MATCH);
	
	private final DataApp mApplication;
	private final DataPathMatcher mMatcher;
	private final SelectAction mSelectAction;
	
	private final Set<LocalMediaSet> mMediaSets;
	private boolean mDirty = false;
	
	public LocalMediaSource(DataApp app) { 
		super(PREFIX);
		
		mApplication = app;
		mMediaSets = new HashSet<LocalMediaSet>();
		mSelectAction = new MediaAction.LocalAlbumSelectAction();
		
		mMatcher = new DataPathMatcher();
        mMatcher.add("/local/image", LOCAL_IMAGE_ALBUMSET);
        mMatcher.add("/local/video", LOCAL_VIDEO_ALBUMSET);
        mMatcher.add("/local/all", LOCAL_ALL_ALBUMSET);

        mMatcher.add("/local/image/*", LOCAL_IMAGE_ALBUM);
        mMatcher.add("/local/video/*", LOCAL_VIDEO_ALBUM);
        mMatcher.add("/local/all/*", LOCAL_ALL_ALBUM);
        mMatcher.add("/local/image/item/*", LOCAL_IMAGE_ITEM);
        mMatcher.add("/local/video/item/*", LOCAL_VIDEO_ITEM);

        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/images/media/#", LOCAL_IMAGE_ITEM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/video/media/#", LOCAL_VIDEO_ITEM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/images/media", LOCAL_IMAGE_ALBUM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/video/media", LOCAL_VIDEO_ALBUM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/file", LOCAL_ALL_ALBUM);
	}
	
	public final DataApp getDataApp() { 
		return mApplication;
	}
	
	@Override
    public String[] getTopSetPaths() { 
    	return new String[] { 
    			MediaUtils.TOP_LOCAL_IMAGE_SET_PATH, 
    			MediaUtils.TOP_LOCAL_VIDEO_SET_PATH, 
    			//MediaUtils.TOP_LOCAL_SET_PATH
    		};
    }
	
	@Override
	public boolean isDeleteEnabled() { return true; }
	
    @Override
    public synchronized void notifyDirty() { 
    	ContentHelper.getInstance().updateFetchDirtyWithPrefix(LocalMediaSource.PREFIX);
    	mDirty = true;
    }
	
    @Override
    public synchronized boolean isDirty() { 
    	return mDirty; 
    }
    
    synchronized void setDirty(boolean dirty) { mDirty = dirty; }
    
    synchronized void removeMediaItem(MediaItem item) { 
    	if (item == null) return;
    	setDirty(true);
    	
    	for (LocalMediaSet mediaSet : mMediaSets) { 
    		if (mediaSet != null) mediaSet.removeMediaItem(item);
    	}
    }
    
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
        case LOCAL_ALL_ALBUMSET:
        case LOCAL_IMAGE_ALBUMSET:
        case LOCAL_VIDEO_ALBUMSET: {
        	LocalAlbumSet albumSet = new LocalAlbumSet(this, path);
            mMediaSets.add(albumSet);
            return albumSet;
        } case LOCAL_IMAGE_ALBUM: {
        	LocalAlbum album = new LocalAlbum(this, path, mMatcher.getIntVar(0), true);
        	mMediaSets.add(album);
        	return album;
        } case LOCAL_VIDEO_ALBUM: {
        	LocalAlbum album = new LocalAlbum(this, path, mMatcher.getIntVar(0), false);
        	mMediaSets.add(album);
        	return album;
        } case LOCAL_ALL_ALBUM: {
            int bucketId = mMatcher.getIntVar(0);
            MediaSet imageSet = (MediaSet) getMediaObject(
                    LocalAlbumSet.PATH_IMAGE.getChild(bucketId));
            MediaSet videoSet = (MediaSet) getMediaObject(
                    LocalAlbumSet.PATH_VIDEO.getChild(bucketId));
            Comparator<MediaItem> comp = sDateTakenComparator;
            LocalMergeAlbum album = new LocalMergeAlbum(this, path, comp, 
            		new MediaSet[] {imageSet, videoSet}, bucketId);
            mMediaSets.add(album);
            return album;
        }
        case LOCAL_IMAGE_ITEM:
            return new LocalImage(this, path, mMatcher.getIntVar(0));
        case LOCAL_VIDEO_ITEM:
            return new LocalVideo(this, path, mMatcher.getIntVar(0));
        default:
            throw new RuntimeException("bad path: " + path);
		}
	}
	
}
