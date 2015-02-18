package org.javenstudio.provider.media;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.media.photo.PhotoBinder;
import org.javenstudio.provider.media.photo.PhotoCursorFactory;
import org.javenstudio.provider.media.photo.PhotoDataSets;
import org.javenstudio.provider.media.photo.PhotoFactory;
import org.javenstudio.provider.media.photo.PhotoSource;

public abstract class MediaPhotoFactory implements PhotoFactory {

	@Override
	public PhotoSource createPhotoSource(Provider provider, PhotoSet set) { 
		return new MediaPhotoSource(provider, set, this);
	}
	
	@Override
	public PhotoDataSets createPhotoDataSets(PhotoSource source) { 
		return new PhotoDataSets(new PhotoCursorFactory());
	}
	
	@Override
	public PhotoBinder createPhotoBinder(PhotoSource source, ViewType.Type type) { 
		return new MediaPhotoBinderLarge((MediaPhotoSource)source);
	}

}
