package org.javenstudio.provider.account.dashboard;

import android.graphics.drawable.Drawable;

import org.javenstudio.provider.library.IPhotoData;
import org.javenstudio.provider.library.ISectionInfoData;

public interface IHistorySectionData extends IPhotoData, ISectionInfoData {

	public String getUserTitle();
	public Drawable getUserAvatarRoundDrawable(int size, int padding);
	
	public String getTitle();
	public String getContentType();
	public String getTitleInfo();
	
	public String getSectionImageURL();
	public int getSectionImageWidth();
	public int getSectionImageHeight();
	
	public long getAccessTime();
	
}
