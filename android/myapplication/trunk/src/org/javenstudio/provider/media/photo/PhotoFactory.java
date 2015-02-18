package org.javenstudio.provider.media.photo;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.Provider;

public interface PhotoFactory {

	public PhotoSource createPhotoSource(Provider provider, PhotoSet set);
	
	public PhotoDataSets createPhotoDataSets(PhotoSource source);
	public PhotoBinder createPhotoBinder(PhotoSource source, ViewType.Type type);
	
	public ViewType getViewType();
	
}
