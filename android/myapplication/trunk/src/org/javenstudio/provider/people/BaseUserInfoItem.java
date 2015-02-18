package org.javenstudio.provider.people;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.provider.media.photo.PhotoItem;
import org.javenstudio.provider.people.user.UserInfoItem;

public abstract class BaseUserInfoItem extends UserInfoItem {

	public BaseUserInfoItem(BaseUserInfoProvider p, String userId) { 
		this(p, userId, null);
	}
	
	public BaseUserInfoItem(BaseUserInfoProvider p, String userId, String userName) { 
		super(p, userId, userName);
	}
	
	@Override
	public Image getBackgroundImage() { 
		PhotoItem item = ((BaseUserInfoProvider)getProvider()).getPhotoItem();
		if (item != null) {
			Image image = item.getPhoto().getBitmapImage();
			if (image != null && image.existBitmap())
				return image;
		}
		
		return null; 
	}
	
}
