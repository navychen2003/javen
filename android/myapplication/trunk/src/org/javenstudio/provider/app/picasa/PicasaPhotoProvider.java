package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.app.BasePhotoProvider;
import org.javenstudio.provider.media.MediaPhotoFactory;
import org.javenstudio.provider.media.MediaPhotoSource;
import org.javenstudio.provider.media.photo.PhotoBinder;
import org.javenstudio.provider.media.photo.PhotoSource;

public class PicasaPhotoProvider extends BasePhotoProvider {

	public PicasaPhotoProvider(String name, int iconRes, 
			PhotoSet set, PicasaPhotoFactory factory) { 
		super(name, iconRes, set, factory);
	}
	
	public static abstract class PicasaPhotoFactory extends MediaPhotoFactory {
		@Override
		public PhotoBinder createPhotoBinder(PhotoSource source, ViewType.Type type) { 
			if (type == ViewType.Type.LIST)
				return new PicasaPhotoBinderList((MediaPhotoSource)source);
			else if (type == ViewType.Type.SMALL)
				return new PicasaPhotoBinderSmall((MediaPhotoSource)source);
			
			return new PicasaPhotoBinderLarge((MediaPhotoSource)source);
		}
	}

}
