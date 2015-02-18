package org.javenstudio.provider.people;

import org.javenstudio.provider.media.photo.PhotoItem;
import org.javenstudio.provider.people.user.UserInfoProvider;

public abstract class BaseUserInfoProvider extends UserInfoProvider {

	private PhotoItem mPhotoItem = null;
	
	public BaseUserInfoProvider(String name, int iconRes) { 
		super(name, iconRes);
	}
	
	public void setPhotoItem(PhotoItem item) { mPhotoItem = item; }
	public PhotoItem getPhotoItem() { return mPhotoItem; }
	
}
