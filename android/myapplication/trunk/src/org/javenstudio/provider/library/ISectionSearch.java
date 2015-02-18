package org.javenstudio.provider.library;

public interface ISectionSearch extends ISectionList {

	public ISectionSearch[] getSearches();
	public String getQueryText();
	public String getType();
	
	public int getTotalCount();
	public int getSectionSetCount();
	
	public ISectionSet getSectionSetAt(int index);
	public ISectionSet[] getSectionList();
	public void getSectionThumbnails(IThumbnailCallback cb);
	
	public void setFirstVisibleItem(IVisibleData data);
	public IVisibleData getFirstVisibleItem();
	
	public ICategoryData getFileCategory();
	public ICategoryData getFolderCategory();
	
}
