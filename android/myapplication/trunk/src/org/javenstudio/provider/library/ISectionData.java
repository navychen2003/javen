package org.javenstudio.provider.library;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.SelectManager;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.provider.library.select.ISelectData;

public interface ISectionData extends IVisibleData, IPhotoData, 
		ISectionInfoData, ISelectData, SelectManager.SelectData {

	public ILibraryData getLibrary();
	public ISectionFolder getParent();
	
	public Uri getContentUri();
	public boolean supportOperation(FileOperation.Operation op);
	
	public String getId();
	public String getName();
	public String getDisplayName();
	public String getType();
	public String getExtension();
	public String getPath();
	public String getOwner();
	public String getChecksum();
	
	public int getWidth();
	public int getHeight();
	public long getLength();
	
	public Drawable getTypeIcon();
	public String getPhotoURL();
	public String getPosterURL();
	public String getPosterThumbnailURL();
	public String getBackgroundURL();
	
	public long getRefreshTime();
	public long getModifiedTime();
	
	public String getSizeInfo();
	public String getSizeDetails();
	public boolean isFolder();
	
	public void getDetails(IMediaDetails details);
	public void getExifs(IMediaDetails details);
	
}
