package org.javenstudio.android.data;

import android.net.Uri;

public interface IDownloadable {

	public Uri getContentUri();
	
	public String getContentId();
	public String getContentName();
	public String getContentType();
	
}
