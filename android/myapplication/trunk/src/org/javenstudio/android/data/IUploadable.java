package org.javenstudio.android.data;

import android.net.Uri;

public interface IUploadable {

	public Uri getContentUri();
	
	public String getContentId();
	public String getContentName();
	public String getContentType();
	
	public String getDataPath();
	public String getFilePath();
	
}
