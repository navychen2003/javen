package org.javenstudio.android.data.media;

import java.util.List;

import org.javenstudio.android.app.SelectAction;
import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.IData;
import org.javenstudio.android.data.image.Image;

public interface PhotoSet extends IData, SelectManager.SelectData {

	public String getName();
	public MediaInfo getMediaInfo();
	public MediaSet getMediaSet();
	
	public boolean isSearchable();
	public boolean isDeleteEnabled();
	public String getSearchText();
	public String[] getPhotoTags();
	
	public int getItemCount();
	public List<MediaItem> getItemList(int start, int count);
	
	public Image[] getAlbumImages(int count);
	public boolean isDirty();
	
	public boolean delete() throws DataException;
	public void notifyDirty();
	
	public SelectAction getSelectAction();
	
}
