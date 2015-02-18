package org.javenstudio.android.data.media;

import android.net.Uri;

import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.data.DataException;
import org.javenstudio.android.data.IData;
import org.javenstudio.android.data.image.Image;

public interface Photo extends IData, SelectManager.SelectData {

	public String getName();
	public String getLocation();
	public String getAvatarLocation();
	
	public long getDateInMs();
	public MediaInfo getMediaInfo();
	public Uri getContentUri();
	public Uri getPlayUri();
	
	public Image getBitmapImage();
	public Image getAvatarImage();
	
	public boolean isLocalItem();
	public int getSupportedOperations();
	
	public boolean delete() throws DataException;
	public void notifyDirty();
	
}
