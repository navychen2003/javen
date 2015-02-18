package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.app.BasePhotoProvider;
import org.javenstudio.provider.media.MediaPhotoFactory;
import org.javenstudio.provider.media.MediaPhotoSource;
import org.javenstudio.provider.media.photo.PhotoBinder;
import org.javenstudio.provider.media.photo.PhotoSource;

public class FlickrPhotoProvider extends BasePhotoProvider {

	public FlickrPhotoProvider(String name, int iconRes, 
			PhotoSet set, FlickrPhotoFactory factory) { 
		super(name, iconRes, set, factory);
	}
	
	public static abstract class FlickrPhotoFactory extends MediaPhotoFactory {
		@Override
		public PhotoBinder createPhotoBinder(PhotoSource source, ViewType.Type type) { 
			if (type == ViewType.Type.LIST)
				return new FlickrPhotoBinderList((MediaPhotoSource)source);
			else if (type == ViewType.Type.SMALL)
				return new FlickrPhotoBinderSmall((MediaPhotoSource)source);
			
			return new FlickrPhotoBinderLarge((MediaPhotoSource)source);
		}
	}
	
}
