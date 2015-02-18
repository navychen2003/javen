package org.javenstudio.android.data.media.local;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;

import org.javenstudio.android.MessageHelper;
import org.javenstudio.android.data.CacheRequest;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.FileImage;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.cocoka.graphics.BitmapUtil;
import org.javenstudio.cocoka.util.BitmapHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.cocoka.worker.job.JobContext;
import org.javenstudio.common.util.Logger;

public final class LocalVideo extends LocalMediaItem {
	private static final Logger LOG = Logger.getLogger(LocalVideo.class);

    static final DataPath ITEM_PATH = DataPath.fromString("/local/video/item");

    // Must preserve order between these indices and the order of the terms in
    // the following PROJECTION array.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_DURATION = 9;
    private static final int INDEX_BUCKET_ID = 10;
    private static final int INDEX_SIZE = 11;
    private static final int INDEX_RESOLUTION = 12;

    static final String[] PROJECTION = new String[] {
            VideoColumns._ID,
            VideoColumns.TITLE,
            VideoColumns.MIME_TYPE,
            VideoColumns.LATITUDE,
            VideoColumns.LONGITUDE,
            VideoColumns.DATE_TAKEN,
            VideoColumns.DATE_ADDED,
            VideoColumns.DATE_MODIFIED,
            VideoColumns.DATA,
            VideoColumns.DURATION,
            VideoColumns.BUCKET_ID,
            VideoColumns.SIZE,
            VideoColumns.RESOLUTION,
    };
	
    
    private final LocalMediaSource mSource;
    private FileImage mImage;
    public int durationInSec;
    
    public LocalVideo(LocalMediaSource source, DataPath path, Cursor cursor) { 
		super(path, nextVersionNumber());
		mSource = source;
		loadFromCursor(cursor);
    }
    
	public LocalVideo(LocalMediaSource source, DataPath path, int id) { 
		super(path, nextVersionNumber());
		mSource = source;
		
        ContentResolver resolver = source.getDataApp().getContentResolver();
        Uri uri = Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = LocalAlbum.getItemCursor(resolver, uri, PROJECTION, id);
        if (cursor == null) 
            throw new RuntimeException("cannot get cursor for: " + path);
        
        try {
            if (cursor.moveToNext()) 
                loadFromCursor(cursor);
            else 
                throw new RuntimeException("cannot find data for: " + path);
        } finally {
            cursor.close();
        }
	}
	
	@Override
	public DataApp getDataApp() { 
		return mSource.getDataApp();
	}
	
    private void loadFromCursor(Cursor cursor) {
        id = cursor.getInt(INDEX_ID);
        caption = cursor.getString(INDEX_CAPTION);
        mimeType = cursor.getString(INDEX_MIME_TYPE);
        latitude = cursor.getDouble(INDEX_LATITUDE);
        longitude = cursor.getDouble(INDEX_LONGITUDE);
        dateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        dateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        dateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        filePath = cursor.getString(INDEX_DATA);
        durationInSec = cursor.getInt(INDEX_DURATION) / 1000;
        bucketId = cursor.getInt(INDEX_BUCKET_ID);
        fileSize = cursor.getLong(INDEX_SIZE);
        
        parseResolution(cursor.getString(INDEX_RESOLUTION));
        
        mImage = new FileImage(mSource.getDataApp(), getDataPath(), filePath) { 
	        	@Override
	        	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
	        		return LocalVideo.this.requestImage(holder, type);
	        	}
	        };
    }

	@Override
	public Image getBitmapImage() { return mImage; }
    
    private void parseResolution(String resolution) {
        if (resolution == null) return;
        int m = resolution.indexOf('x');
        if (m == -1) return;
        try {
            int w = Integer.parseInt(resolution.substring(0, m));
            int h = Integer.parseInt(resolution.substring(m + 1));
            width = w;
            height = h;
        } catch (Throwable t) {
        	if (LOG.isWarnEnabled())
            	LOG.warn(t.toString(), t);
        }
    }
	
    @Override
    protected boolean updateFromCursor(Cursor cursor) {
        UpdateHelper uh = new UpdateHelper();
        id = uh.update(id, cursor.getInt(INDEX_ID));
        caption = uh.update(caption, cursor.getString(INDEX_CAPTION));
        mimeType = uh.update(mimeType, cursor.getString(INDEX_MIME_TYPE));
        latitude = uh.update(latitude, cursor.getDouble(INDEX_LATITUDE));
        longitude = uh.update(longitude, cursor.getDouble(INDEX_LONGITUDE));
        dateTakenInMs = uh.update(
                dateTakenInMs, cursor.getLong(INDEX_DATE_TAKEN));
        dateAddedInSec = uh.update(
                dateAddedInSec, cursor.getLong(INDEX_DATE_ADDED));
        dateModifiedInSec = uh.update(
                dateModifiedInSec, cursor.getLong(INDEX_DATE_MODIFIED));
        filePath = uh.update(filePath, cursor.getString(INDEX_DATA));
        durationInSec = uh.update(
                durationInSec, cursor.getInt(INDEX_DURATION) / 1000);
        bucketId = uh.update(bucketId, cursor.getInt(INDEX_BUCKET_ID));
        fileSize = uh.update(fileSize, cursor.getLong(INDEX_SIZE));
        return uh.isUpdated();
    }
    
    @Override
    public int getSupportedOperations() {
        return SUPPORT_DELETE; //SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_PLAY | SUPPORT_INFO | SUPPORT_TRIM;
    }

    @Override
    public boolean delete() {
        MediaUtils.assertNotInRenderThread();
        
        Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;
        int count = mSource.getDataApp().getContentResolver().delete(baseUri, "_id=?",
                new String[]{String.valueOf(id)});
        
        if (count > 0) mSource.removeMediaItem(this);
        
        if (LOG.isDebugEnabled())
        	LOG.debug("delete: uri=" + baseUri + " id=" + id + " count=" + count);
        
        return count > 0;
    }

    @Override
    public void rotate(int degrees) {
        // TODO
    }

    @Override
    public Uri getContentUri() {
        Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Override
    public Uri getPlayUri() {
        return getContentUri();
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_VIDEO;
    }

    @Override
    public MediaDetails getMediaDetails() {
        MediaDetails details = super.getMediaDetails();
        int s = durationInSec;
        if (s > 0) {
            details.addDetail(MediaDetails.INDEX_DURATION, MessageHelper.formatDuration(
                    mSource.getDataApp().getContext(), durationInSec));
        }
        return details;
    }
	
    @Override
    public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
        return new LocalVideoRequest(mSource.getDataApp(), 
        		holder, getDataPath(), type, filePath);
    }

    public static class LocalVideoRequest extends CacheRequest {
        private final String mLocalFilePath;

        public LocalVideoRequest(DataApp app, BitmapHolder holder, 
        		DataPath path, int type, String localFilePath) {
            super(app.getCacheData(), holder, path, type, 
            		BitmapHelper.getTargetSize(type));
            mLocalFilePath = localFilePath;
        }

        @Override
        public BitmapRef onDecodeOriginal(JobContext jc, int type) {
        	BitmapRef bitmap = BitmapUtil.createVideoThumbnail(getBitmapHolder(), mLocalFilePath);
            if (bitmap == null || jc.isCancelled()) return null;
            return bitmap;
        }
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
        throw new UnsupportedOperationException("Cannot regquest a large image"
                + " to a local video!");
    }
    
}
