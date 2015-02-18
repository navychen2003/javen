package org.javenstudio.provider.library;

import android.net.Uri;

import org.javenstudio.android.app.FileOperation;
import org.javenstudio.cocoka.data.IMediaDetails;

public interface IPhotoData {

	public String getId();
	public String getName();
	public String getType();
	public String getOwner();
	
	public String getPhotoURL();
	public String getPosterThumbnailURL();
	
	public long getLength();
	public String getSizeInfo();
	
	public Uri getContentUri();
	public boolean supportOperation(FileOperation.Operation op);
	
	public void getDetails(IMediaDetails details);
	public void getExifs(IMediaDetails details);
	
}
