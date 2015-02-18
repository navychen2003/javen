package org.javenstudio.provider.library;

public interface ISectionList {

	public ILibraryData getLibrary();
	public ISectionFolder getParent();
	
	public String getName();
	public int getTotalCount();
	public int getSectionSetCount();
	public ISectionSet getSectionSetAt(int index);
	
	public boolean isRecycleBin();
	public boolean isSearchResult();
	
	public void setFirstVisibleItem(IVisibleData data);
	public IVisibleData getFirstVisibleItem();
	
	public ICategoryData getFileCategory();
	public ICategoryData getFolderCategory();
	
	public long getRefreshTime();
	public void clearSectionSet();
	
}
