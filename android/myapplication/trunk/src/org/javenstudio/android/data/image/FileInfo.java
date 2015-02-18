package org.javenstudio.android.data.image;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.data.DataPath;

public abstract class FileInfo {

	public abstract DataPath getDataPath();
	public abstract Drawable getTypeIcon();
	
	public abstract String getLocation();
	public abstract String getContentType();
	
	public abstract String getFilePath();
	public abstract String getFileName();
	
	public abstract boolean exists();
	public abstract long getFileLength();
	public abstract long getModifiedTime();
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{file=" + getFilePath() + "}";
	}
	
}
