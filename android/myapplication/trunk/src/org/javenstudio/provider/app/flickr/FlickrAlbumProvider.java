package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.app.BaseAlbumProvider;
import org.javenstudio.provider.media.MediaAlbumFactory;
import org.javenstudio.provider.media.MediaAlbumSource;
import org.javenstudio.provider.media.album.AlbumBinder;
import org.javenstudio.provider.media.album.AlbumSource;

public class FlickrAlbumProvider extends BaseAlbumProvider {

	public FlickrAlbumProvider(String name, int iconRes, 
			AlbumSet set, PicasaAlbumFactory factory) { 
		super(name, iconRes, set, factory);
	}
	
	public static abstract class PicasaAlbumFactory extends MediaAlbumFactory {
		@Override
		public boolean showAlbum(PhotoSet photoSet) { 
			return photoSet != null && photoSet instanceof FlickrAlbum;
		}
		
		@Override
		public AlbumBinder createAlbumBinder(AlbumSource source, ViewType.Type type) { 
			if (type == ViewType.Type.LIST)
				return new FlickrAlbumBinderList((MediaAlbumSource)source);
			else if (type == ViewType.Type.SMALL)
				return new FlickrAlbumBinderSmall((MediaAlbumSource)source);
			
			return new FlickrAlbumBinderLarge((MediaAlbumSource)source);
		}
	}
	
}
