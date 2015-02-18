package org.javenstudio.provider.app.anybox.library;

import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.provider.app.anybox.AnyboxHistory;
import org.javenstudio.provider.library.SectionPhotoItem;

public class AnyboxDashboardPhotoItem extends SectionPhotoItem {

	public AnyboxDashboardPhotoItem(AnyboxPhotoSet photoSet, 
			AnyboxHistory.SectionData data) {
		super(photoSet, data);
	}
	
	@Override
	public AnyboxHistory.SectionData getPhotoData() {
		return (AnyboxHistory.SectionData)super.getPhotoData();
	}
	
	@Override
	public String getOwnerTitle() {
		return getPhotoData().getUserTitle();
	}
	
	@Override
	public HttpImage getOwnerAvatarImage() { 
		return getPhotoData().getAvatarImage();
	}
	
	@Override
	public String getOwnerAvatarLocation() {
		HttpImage image = getOwnerAvatarImage();
		return image != null ? image.getLocation() : null;
	}
	
}
