package org.javenstudio.provider.task.upload;

import android.net.Uri;

import org.javenstudio.android.entitydb.content.UploadData;

public class UploadHelper {

    public static final String ACTION_RETRY = "org.javenstudio.intent.action.UPLOAD_WAKEUP";
    public static final String ACTION_OPEN 	= "org.javenstudio.intent.action.UPLOAD_OPEN";
    public static final String ACTION_LIST 	= "org.javenstudio.intent.action.UPLOAD_LIST";
    public static final String ACTION_HIDE 	= "org.javenstudio.intent.action.UPLOAD_HIDE";
	
    public static final Uri ALL_UPLOADS_CONTENT_URI =
            Uri.parse("content://uploads/all_uploads");
	
    public static boolean isStatusError(int status) { 
    	return status == UploadData.STATUS_FAILED;
    }
    
}
