package org.javenstudio.android.data.media;

import org.javenstudio.android.app.SelectAction;

public interface AlbumSet {

	public String getName();
	public MediaSet[] getMediaSets();
	
	public boolean isDeleteEnabled();
	public boolean isDirty();
	
	public void notifyDirty();
	
	public SelectAction getSelectAction();
	
}
