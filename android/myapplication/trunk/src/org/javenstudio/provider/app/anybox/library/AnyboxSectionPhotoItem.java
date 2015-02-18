package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.provider.app.anybox.AnyboxSectionFile;
import org.javenstudio.provider.library.SectionPhotoItem;

public class AnyboxSectionPhotoItem extends SectionPhotoItem {

	private HttpImage mAvatar = null;
	private String mAvatarURL = null;
	
	public AnyboxSectionPhotoItem(AnyboxPhotoSet photoSet, 
			AnyboxSectionFile data) {
		super(photoSet, data);
	}
	
	@Override
	public AnyboxSectionFile getPhotoData() {
		return (AnyboxSectionFile)super.getPhotoData();
	}
	
	@Override
	public String getOwnerTitle() { 
		String owner = getPhotoData().getOwner(); 
		String username = getPhotoData().getUser().getAccountName();
		String usertitle = getPhotoData().getUser().getUserTitle();
		if (owner == null || owner.length() == 0 || owner.equals(username))
			return usertitle;
		return owner;
	}
	
	@Override
	public HttpImage getOwnerAvatarImage() { 
		String owner = getPhotoData().getOwner(); 
		String username = getPhotoData().getUser().getAccountName();
		if (owner == null || owner.length() == 0 || owner.equals(username)) {
			return getAvatarImage();
		}
		return null; 
	}
	
	@Override
	public String getOwnerAvatarLocation() {
		return mAvatarURL;
	}
	
	private synchronized HttpImage getAvatarImage() { 
		if (mAvatar == null) { 
			String imageURL = getPhotoData().getUser().getAvatarURL(192);
			if (imageURL != null && imageURL.length() > 0) { 
				mAvatarURL = imageURL;
				mAvatar = HttpResource.getInstance().getImage(imageURL);
				mAvatar.addListener(this);
				
				HttpImageItem.requestDownload(mAvatar, false);
			}
		}
		return mAvatar; 
	}
	
}
