package org.javenstudio.provider.app.local;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.media.MediaAlbumFactory;
import org.javenstudio.provider.media.MediaAlbumSource;
import org.javenstudio.provider.media.album.AlbumBinder;
import org.javenstudio.provider.media.album.AlbumSource;

public class LocalAlbumSource extends MediaAlbumSource {

	public LocalAlbumSource(Provider provider, AlbumSet set, 
			LocalAlbumFactory factory) { 
		super(provider, set, factory);
	}
	
	public static abstract class LocalAlbumFactory extends MediaAlbumFactory {
		@Override
		public AlbumBinder createAlbumBinder(AlbumSource source, ViewType.Type type) { 
			if (type == ViewType.Type.LIST)
				return new LocalAlbumBinderList((MediaAlbumSource)source);
			else if (type == ViewType.Type.SMALL)
				return new LocalAlbumBinderSmall((MediaAlbumSource)source);
			
			return new LocalAlbumBinderLarge((MediaAlbumSource)source);
		}
	}
	
}
