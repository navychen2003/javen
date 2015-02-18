package org.javenstudio.android.data.media.local;

import java.io.File;
import java.util.Comparator;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.android.data.DataPath;
import org.javenstudio.android.data.image.FileImage;
import org.javenstudio.android.data.media.MediaSet;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.Utilities;

public class LocalHelper {
    public static final Comparator<MediaSet> NAME_COMPARATOR = new NameComparator();

    public static final int CAMERA_BUCKET_ID = MediaUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
    public static final int DOWNLOAD_BUCKET_ID = MediaUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.DOWNLOAD);
    public static final int EDITED_ONLINE_PHOTOS_BUCKET_ID = MediaUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.EDITED_ONLINE_PHOTOS);
    public static final int IMPORTED_BUCKET_ID = MediaUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + BucketNames.IMPORTED);
    public static final int SNAPSHOT_BUCKET_ID = MediaUtils.getBucketId(
            Environment.getExternalStorageDirectory().toString() +
            "/Pictures/Screenshots");

    private static final DataPath[] CAMERA_PATHS = {
            DataPath.fromString("/local/all/" + CAMERA_BUCKET_ID),
            DataPath.fromString("/local/image/" + CAMERA_BUCKET_ID),
            DataPath.fromString("/local/video/" + CAMERA_BUCKET_ID)};

    public static boolean isCameraSource(DataPath path) {
        return CAMERA_PATHS[0] == path || CAMERA_PATHS[1] == path
                || CAMERA_PATHS[2] == path;
    }

    // Sort MediaSets by name
    public static class NameComparator implements Comparator<MediaSet> {
	        @Override
	        public int compare(MediaSet set1, MediaSet set2) {
	            int result = set1.getName().compareToIgnoreCase(set2.getName());
	            if (result != 0) return result;
	            return set1.getDataPath().compareTo(set2.getDataPath());
	        }
	    }
    
    public static String getFilePath(Context context, Uri mediaUri) { 
    	if (mediaUri == null) return null;
    	
    	if ("file".equalsIgnoreCase(mediaUri.getScheme())) 
    		return mediaUri.getPath();
    	
    	String filePath = null;
    	String[] filePathColumn = { MediaStore.Images.Media.DATA };
    	 
        Cursor cursor = context.getContentResolver().query(mediaUri,
                filePathColumn, null, null, null);
        if (cursor.moveToFirst()) {
        	int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        	filePath = cursor.getString(columnIndex);
        }
        cursor.close();

    	return filePath;
    }
    
    public static FileImage getFileImage(DataApp app, Uri mediaUri) { 
    	if (mediaUri == null) return null;
    	
    	String filePath = getFilePath(app.getContext(), mediaUri);
    	if (filePath != null && filePath.length() > 0) { 
    		File file = new File(filePath);
    		if (file.exists() && file.isFile()) { 
    			DataPath path = DataPath.fromString("/local/image/item/" 
    					+ Utilities.toMD5(filePath));
    			
    			return new FileImage(app, path, filePath);
    		}
    	}
    	
    	return null;
    }
    
}
