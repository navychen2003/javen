package org.javenstudio.provider.app.anybox;

import android.net.Uri;

import org.javenstudio.android.data.IDownloadable;
import org.javenstudio.provider.library.ISectionFolder;

public class AnyboxSectionFile extends AnyboxSection implements IDownloadable {

	public AnyboxSectionFile(AnyboxLibrary library, 
			ISectionFolder parent, AnyboxData data, String id, 
			boolean isfolder, long requestTime) {
		super(library, parent, data, id, isfolder, requestTime);
	}
	
	@Override
	public Uri getContentUri() { 
		String url = AnyboxHelper.getFileURL(getRequestWrapper(), getId(), false);
		return url != null ? Uri.parse(url) : null; 
	}
	
}
