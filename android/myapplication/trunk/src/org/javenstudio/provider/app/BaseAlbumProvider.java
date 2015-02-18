package org.javenstudio.provider.app;

import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.provider.media.AlbumProvider;
import org.javenstudio.provider.media.album.AlbumFactory;

public abstract class BaseAlbumProvider extends AlbumProvider {

	public BaseAlbumProvider(String name, int iconRes, 
			AlbumSet set, AlbumFactory factory) { 
		super(name, iconRes, set, factory);
	}
	
}
