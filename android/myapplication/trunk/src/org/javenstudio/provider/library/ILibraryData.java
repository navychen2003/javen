package org.javenstudio.provider.library;

import android.graphics.drawable.Drawable;

import org.javenstudio.provider.library.select.ISelectData;

public interface ILibraryData extends ISectionFolder, 
		ISectionList, ISelectData {

	public String getName();
	public String getDisplayName();
	
	public int getTotalCount();
	public int getSubCount();
	public long getSubLength();
	
	public Drawable getTypeIcon();
	public String getPosterURL();
	public String getPosterThumbnailURL();
	
	public long getRefreshTime();
	public long getModifiedTime();
	
}
