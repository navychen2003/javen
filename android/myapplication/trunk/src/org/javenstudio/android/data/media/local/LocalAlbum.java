package org.javenstudio.android.data.media.local;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataManager;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.ReloadCallback;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.MediaItem;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.Utils;
import org.javenstudio.common.util.Logger;

final class LocalAlbum extends LocalMediaSet {
	private static final Logger LOG = Logger.getLogger(LocalAlbum.class);

	private static final String[] COUNT_PROJECTION = { "count(*)" };
    private static final int INVALID_COUNT = -1;
    
    public static String getLocalizedName(Context context, int bucketId,
            String name) {
        if (bucketId == LocalHelper.CAMERA_BUCKET_ID) {
            return context.getResources().getString(R.string.folder_camera);
        } else if (bucketId == LocalHelper.DOWNLOAD_BUCKET_ID) {
            return context.getResources().getString(R.string.folder_download);
        } else if (bucketId == LocalHelper.IMPORTED_BUCKET_ID) {
            return context.getResources().getString(R.string.folder_imported);
        } else if (bucketId == LocalHelper.SNAPSHOT_BUCKET_ID) {
            return context.getResources().getString(R.string.folder_screenshot);
        } else if (bucketId == LocalHelper.EDITED_ONLINE_PHOTOS_BUCKET_ID) {
            return context.getResources().getString(R.string.folder_edited_online_photos);
        } else {
            return name;
        }
    }
    
	private final LocalMediaSource mSource;
	//private final MediaChangeNotifier mNotifier;
	private final ContentResolver mResolver;
    private final DataPath mItemPath;
    private final String mName;
    private final String mWhereClause;
    private final String mOrderClause;
    private final Uri mBaseUri;
    private final String[] mProjection;
    private final int mBucketId;
    private final boolean mIsImage;
    
    private int mCachedCount = INVALID_COUNT;
    private boolean mReloaded = false;
	
	public LocalAlbum(LocalMediaSource source, DataPath path, int bucketId,
            boolean isImage) { 
		this(source, path, bucketId, isImage, BucketHelper.getBucketName(
				source.getDataApp().getContentResolver(), bucketId));
	}
	
	public LocalAlbum(LocalMediaSource source, DataPath path, int bucketId,
            boolean isImage, String name) { 
		super(path, nextVersionNumber());
		mSource = source;
		mResolver = source.getDataApp().getContentResolver();
		mBucketId = bucketId;
        mName = getLocalizedName(source.getDataApp().getContext(), bucketId, name);
        mIsImage = isImage;
        
        if (isImage) {
            mWhereClause = ImageColumns.BUCKET_ID + " = ?";
            mOrderClause = ImageColumns.DATE_TAKEN + " DESC, "
                    + ImageColumns._ID + " DESC";
            mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
            mProjection = LocalImage.PROJECTION;
            mItemPath = LocalImage.ITEM_PATH;
        } else {
            mWhereClause = VideoColumns.BUCKET_ID + " = ?";
            mOrderClause = VideoColumns.DATE_TAKEN + " DESC, "
                    + VideoColumns._ID + " DESC";
            mBaseUri = Video.Media.EXTERNAL_CONTENT_URI;
            mProjection = LocalVideo.PROJECTION;
            mItemPath = LocalVideo.ITEM_PATH;
        }

        //mNotifier = new MediaChangeNotifier(this, mBaseUri, source.getApplication());
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public String getTitle() { return getName(); }
	
	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
    public boolean isCameraRoll() {
        return mBucketId == LocalHelper.CAMERA_BUCKET_ID;
    }

    public Uri getContentUri() {
        if (mIsImage) {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendQueryParameter(LocalMediaSource.KEY_BUCKET_ID,
                            String.valueOf(mBucketId)).build();
        } else {
            return MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendQueryParameter(LocalMediaSource.KEY_BUCKET_ID,
                            String.valueOf(mBucketId)).build();
        }
    }

    @Override
    public List<MediaItem> getItemList(int start, int count) {
        Uri uri = mBaseUri.buildUpon().appendQueryParameter("limit", start + "," + count).build();
        
        ArrayList<MediaItem> list = new ArrayList<MediaItem>();
        MediaUtils.assertNotInRenderThread();
        
        Cursor cursor = mResolver.query(
                uri, mProjection, mWhereClause,
                new String[]{String.valueOf(mBucketId)},
                mOrderClause);
        
        if (cursor == null) {
        	if (LOG.isWarnEnabled())
            	LOG.warn("query fail: " + uri);
        	
            return list;
        }

        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);  // _id must be in the first column
                DataPath childPath = mItemPath.getChild(id);
                MediaItem item = loadOrUpdateItem(mSource, childPath, cursor, mIsImage);
                list.add(item);
            }
        } finally {
            cursor.close();
        }
        
        return list;
    }

    private static MediaItem loadOrUpdateItem(LocalMediaSource source, 
    		DataPath path, Cursor cursor, boolean isImage) {
        synchronized (DataManager.LOCK) {
            LocalMediaItem item = (LocalMediaItem) source.peekMediaObject(path);
            if (item == null) {
                if (isImage) {
                    item = new LocalImage(source, path, cursor);
                } else {
                    item = new LocalVideo(source, path, cursor);
                }
            } else {
                item.updateContent(cursor);
            }
            return item;
        }
    }

    // The pids array are sorted by the (path) id.
    public static MediaItem[] getMediaItemById(LocalMediaSource source, 
    		boolean isImage, ArrayList<Integer> ids) {
        // get the lower and upper bound of (path) id
        MediaItem[] result = new MediaItem[ids.size()];
        if (ids.isEmpty()) return result;
        
        int idLow = ids.get(0);
        int idHigh = ids.get(ids.size() - 1);

        // prepare the query parameters
        Uri baseUri;
        String[] projection;
        DataPath itemPath;
        
        if (isImage) {
            baseUri = Images.Media.EXTERNAL_CONTENT_URI;
            projection = LocalImage.PROJECTION;
            itemPath = LocalImage.ITEM_PATH;
            
        } else {
            baseUri = Video.Media.EXTERNAL_CONTENT_URI;
            projection = LocalVideo.PROJECTION;
            itemPath = LocalVideo.ITEM_PATH;
        }

        ContentResolver resolver = source.getDataApp().getContentResolver();
        Cursor cursor = resolver.query(baseUri, projection, "_id BETWEEN ? AND ?",
                new String[]{String.valueOf(idLow), String.valueOf(idHigh)},
                "_id");
        
        if (cursor == null) {
        	if (LOG.isWarnEnabled())
            	LOG.warn("query fail" + baseUri);
        	
            return result;
        }
        
        try {
            int n = ids.size();
            int i = 0;

            while (i < n && cursor.moveToNext()) {
                int id = cursor.getInt(0);  // _id must be in the first column

                // Match id with the one on the ids list.
                if (ids.get(i) > id) 
                    continue;

                while (ids.get(i) < id) {
                    if (++i >= n) 
                        return result;
                }

                DataPath childPath = itemPath.getChild(id);
                MediaItem item = loadOrUpdateItem(source, childPath, cursor, isImage);
                result[i] = item;
                ++i;
            }
            
            return result;
        } finally {
            cursor.close();
        }
    }

    public static Cursor getItemCursor(ContentResolver resolver, Uri uri,
            String[] projection, int id) {
        return resolver.query(uri, projection, "_id=?",
                new String[]{String.valueOf(id)}, null);
    }

    @Override
    public int getItemCount() {
        if (mCachedCount == INVALID_COUNT) {
            Cursor cursor = mResolver.query(
                    mBaseUri, COUNT_PROJECTION, mWhereClause,
                    new String[]{String.valueOf(mBucketId)}, null);
            
            if (cursor == null) {
            	if (LOG.isWarnEnabled())
                	LOG.warn("query fail: " + mBaseUri);
            	
                return 0;
            }
            
            try {
                Utils.assertTrue(cursor.moveToNext());
                mCachedCount = cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }
        
        return mCachedCount;
    }
	
    @Override
    public synchronized long reloadData(ReloadCallback callback, ReloadType type) {
    	boolean dirty = isDirty() && !callback.isActionProcessing();
    	
        if (/*mNotifier.isDirty()*/ dirty || !mReloaded || type == ReloadType.FORCE) {
        	if (dirty || type == ReloadType.FORCE) { 
        		try { 
        			//callback.showProgressDialog(getApplication().getContext()
            		//		.getString(R.string.dialog_localalbum_scanning_message));
        			
        			reloadLocalItems(callback);
        		} finally { 
        			//callback.hideProgressDialog();
        		}
        	}
            mDataVersion = nextVersionNumber();
            mCachedCount = INVALID_COUNT;
            mReloaded = true;
            
            setDirty(false);
        }
        
        return mDataVersion;
    }

    private void reloadLocalItems(ReloadCallback callback) { 
    	int count = getItemCount();
		if (count <= 0) return;
		
    	List<MediaItem> items = getItemList(0, count);
		for (int i=0; items != null && i < items.size(); i++) { 
			LocalMediaItem item = (LocalMediaItem)items.get(i);
			if (item == null) continue;
			
			String filepath = item.getFilePath();
			if (filepath != null) { 
				File file = new File(filepath);
				if (!file.exists()) { 
					if (LOG.isWarnEnabled()) {
						LOG.warn("MediaItem: " + item.getDataPath() 
								+ " not exist, path=" + filepath);
					}
					
					callback.showContentMessage(filepath);
					try {
						item.delete();
						
					} catch (Throwable e) { 
						if (LOG.isWarnEnabled()) {
							LOG.warn("MediaItem: " + item.getDataPath() 
									+ " remove error: " + e);
						}
					}
				}
			}
		}
    }
    
    @Override
	public boolean isDeleteEnabled() { return true; }
    
    @Override
    public int getSupportedOperations() {
        return SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_INFO;
    }

    @Override
    public boolean delete() {
        MediaUtils.assertNotInRenderThread();
        int count = mResolver.delete(mBaseUri, mWhereClause,
                new String[]{String.valueOf(mBucketId)});
        
        if (LOG.isDebugEnabled())
        	LOG.debug("delete: uri=" + mBaseUri + " id=" + mBucketId + " count=" + count);
        
        return count > 0;
    }

    public boolean isLeafAlbum() {
        return true;
    }
    
}
