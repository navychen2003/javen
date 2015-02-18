package org.javenstudio.provider.library;

import org.javenstudio.provider.library.select.ISelectData;

public interface ISectionFolder extends ISectionData, 
		ISectionList, ISelectData {

	public int getSectionSetCount();
	public ISectionSet getSectionSetAt(int index);
	public ISectionSet[] getSectionList();
	
	public void clearSectionSet();
	public void getSectionThumbnails(IThumbnailCallback cb);
	
	public int getTotalCount();
	public int getSubCount();
	public long getSubLength();
	
	public void setFirstVisibleItem(IVisibleData data);
	public IVisibleData getFirstVisibleItem();
	
	public ICategoryData getFileCategory();
	public ICategoryData getFolderCategory();
	
	public boolean isRecycleBin();
	
}
