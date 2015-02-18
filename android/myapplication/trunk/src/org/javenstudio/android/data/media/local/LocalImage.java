package org.javenstudio.android.data.media.local;

import java.io.File;
import java.io.IOException;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.BitmapRegionDecoder;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Images.ImageColumns;

import org.javenstudio.android.data.CacheRequest;
import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.FileImage;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.cocoka.util.ApiHelper;
import org.javenstudio.cocoka.util.BitmapHolder;
import org.javenstudio.cocoka.util.BitmapRef;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.worker.job.Job;
import org.javenstudio.common.util.Logger;

final class LocalImage extends LocalMediaItem {
	private static final Logger LOG = Logger.getLogger(LocalImage.class);

    static final DataPath ITEM_PATH = DataPath.fromString("/local/image/item");

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
    private static final int INDEX_ORIENTATION = 9;
    private static final int INDEX_BUCKET_ID = 10;
    private static final int INDEX_SIZE = 11;
    private static final int INDEX_WIDTH = 12;
    private static final int INDEX_HEIGHT = 13;

    static final String[] PROJECTION =  {
            ImageColumns._ID,           // 0
            ImageColumns.TITLE,         // 1
            ImageColumns.MIME_TYPE,     // 2
            ImageColumns.LATITUDE,      // 3
            ImageColumns.LONGITUDE,     // 4
            ImageColumns.DATE_TAKEN,    // 5
            ImageColumns.DATE_ADDED,    // 6
            ImageColumns.DATE_MODIFIED, // 7
            ImageColumns.DATA,          // 8
            ImageColumns.ORIENTATION,   // 9
            ImageColumns.BUCKET_ID,     // 10
            ImageColumns.SIZE,          // 11
            "0",                        // 12
            "0"                         // 13
    };

    static {
        updateWidthAndHeightProjection();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static void updateWidthAndHeightProjection() {
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            PROJECTION[INDEX_WIDTH] = MediaColumns.WIDTH;
            PROJECTION[INDEX_HEIGHT] = MediaColumns.HEIGHT;
        }
    }
	
    private final LocalMediaSource mSource;
    private FileImage mImage;
    public int rotation;
    
    public LocalImage(LocalMediaSource source, DataPath path, Cursor cursor) { 
		super(path, nextVersionNumber());
		mSource = source;
		loadFromCursor(cursor);
    }
    
	public LocalImage(LocalMediaSource source, DataPath path, int id) { 
		super(path, nextVersionNumber());
		mSource = source;
		
		ContentResolver resolver = source.getDataApp().getContentResolver();
        Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
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
        rotation = cursor.getInt(INDEX_ORIENTATION);
        bucketId = cursor.getInt(INDEX_BUCKET_ID);
        fileSize = cursor.getLong(INDEX_SIZE);
        width = cursor.getInt(INDEX_WIDTH);
        height = cursor.getInt(INDEX_HEIGHT);
        
        mImage = new FileImage(mSource.getDataApp(), getDataPath(), filePath) { 
	        	@Override
	        	public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
	        		return LocalImage.this.requestImage(holder, type);
	        	}
	        };
    }
    
	@Override
	public Image getBitmapImage() { return mImage; }
    
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
        rotation = uh.update(rotation, cursor.getInt(INDEX_ORIENTATION));
        bucketId = uh.update(bucketId, cursor.getInt(INDEX_BUCKET_ID));
        fileSize = uh.update(fileSize, cursor.getLong(INDEX_SIZE));
        width = uh.update(width, cursor.getInt(INDEX_WIDTH));
        height = uh.update(height, cursor.getInt(INDEX_HEIGHT));
        return uh.isUpdated();
    }
	
    @Override
    public int getSupportedOperations() {
        //StitchingProgressManager progressManager = mApplication.getStitchingProgressManager();
        //if (progressManager != null && progressManager.getProgress(getContentUri()) != null) 
        //    return 0; // doesn't support anything while stitching!
        
        int operation = SUPPORT_DELETE;
        //		SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_CROP
        //        | SUPPORT_SETAS | SUPPORT_EDIT | SUPPORT_INFO;
        
        //if (MediaUtils.isSupportedByRegionDecoder(mimeType)) 
        //    operation |= SUPPORT_FULL_IMAGE;

        //if (MediaUtils.isRotationSupported(mimeType)) 
        //    operation |= SUPPORT_ROTATE;

        //if (MediaUtils.isValidLocation(latitude, longitude)) 
        //    operation |= SUPPORT_SHOW_ON_MAP;
        
        return operation;
    }

    @Override
    public boolean delete() {
        MediaUtils.assertNotInRenderThread();
        
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        int count = mSource.getDataApp().getContentResolver().delete(baseUri, "_id=?",
                new String[]{String.valueOf(id)});
        
        if (count > 0) mSource.removeMediaItem(this);
        
        if (LOG.isDebugEnabled())
        	LOG.debug("delete: uri=" + baseUri + " id=" + id + " count=" + count);
        
        return count > 0;
    }

    private static String getExifOrientation(int orientation) {
        switch (orientation) {
            case 0:
                return String.valueOf(ExifInterface.ORIENTATION_NORMAL);
            case 90:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_90);
            case 180:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_180);
            case 270:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_270);
            default:
                throw new AssertionError("invalid: " + orientation);
        }
    }

    @Override
    public void rotate(int degrees) {
        MediaUtils.assertNotInRenderThread();
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        ContentValues values = new ContentValues();
        int rotation = (this.rotation + degrees) % 360;
        if (rotation < 0) rotation += 360;

        if (mimeType.equalsIgnoreCase("image/jpeg")) {
            try {
                ExifInterface exif = new ExifInterface(filePath);
                exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                        getExifOrientation(rotation));
                exif.saveAttributes();
            } catch (IOException e) {
            	if (LOG.isWarnEnabled())
                	LOG.warn("cannot set exif data: " + filePath);
            }

            // We need to update the filesize as well
            fileSize = new File(filePath).length();
            values.put(Images.Media.SIZE, fileSize);
        }

        values.put(Images.Media.ORIENTATION, rotation);
        mSource.getDataApp().getContentResolver().update(baseUri, values, "_id=?",
                new String[]{String.valueOf(id)});
    }

    @Override
    public Uri getContentUri() {
        Uri baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_IMAGE;
    }

    @Override
    public MediaDetails getMediaDetails() {
        MediaDetails details = super.getMediaDetails();
        details.addDetail(MediaDetails.INDEX_ORIENTATION, Integer.valueOf(rotation));
        if (MediaUtils.MIME_TYPE_JPEG.equals(mimeType)) {
            // ExifInterface returns incorrect values for photos in other format.
            // For example, the width and height of an webp images is always '0'.
            MediaDetails.extractExifInfo(details, filePath);
        }
        return details;
    }

    @Override
    public int getRotation() { return rotation; }
	
    @Override
    public Job<BitmapRef> requestImage(BitmapHolder holder, int type) {
        return new CacheRequest.LocalRequest(mSource.getDataApp().getCacheData(), 
        		holder, getDataPath(), type, filePath);
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage(BitmapHolder holder) {
        return new CacheRequest.LocalLargeRequest(filePath);
    }

}
