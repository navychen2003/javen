package org.javenstudio.provider.library;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.app.FileOperation;

public interface ISectionInfoData {

	public String getId();
	public String getName();
	public String getExtension();
	public String getType();
	public String getPath();
	public String getOwner();
	public String getChecksum();
	
	public int getWidth();
	public int getHeight();
	public long getLength();
	
	public Drawable getTypeIcon();
	public String getPosterURL();
	public String getBackgroundURL();
	
	public boolean isFolder();
	public long getRefreshTime();
	public long getModifiedTime();
	
	public String getSizeInfo();
	public String getSizeDetails();
	
	public boolean supportOperation(FileOperation.Operation op);
	
}
