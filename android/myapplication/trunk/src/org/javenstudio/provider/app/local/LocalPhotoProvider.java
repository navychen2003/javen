package org.javenstudio.provider.app.local;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.app.BasePhotoProvider;
import org.javenstudio.provider.media.MediaPhotoFactory;
import org.javenstudio.provider.media.MediaPhotoSource;
import org.javenstudio.provider.media.photo.PhotoBinder;
import org.javenstudio.provider.media.photo.PhotoSource;

public class LocalPhotoProvider extends BasePhotoProvider {

	public LocalPhotoProvider(String name, int iconRes, 
			PhotoSet set, LocalPhotoFactory factory) { 
		super(name, iconRes, set, factory);
	}
	
	public static abstract class LocalPhotoFactory extends MediaPhotoFactory {
		@Override
		public PhotoBinder createPhotoBinder(PhotoSource source, ViewType.Type type) { 
			if (type == ViewType.Type.LIST)
				return new LocalPhotoBinderList((MediaPhotoSource)source);
			else if (type == ViewType.Type.SMALL)
				return new LocalPhotoBinderSmall((MediaPhotoSource)source);
			
			return new LocalPhotoBinderLarge((MediaPhotoSource)source);
		}
	}

}
