package org.javenstudio.provider.media;

import android.app.Activity;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.media.album.AlbumBinder;
import org.javenstudio.provider.media.album.AlbumCursorFactory;
import org.javenstudio.provider.media.album.AlbumDataSets;
import org.javenstudio.provider.media.album.AlbumFactory;
import org.javenstudio.provider.media.album.AlbumSource;

public abstract class MediaAlbumFactory implements AlbumFactory {

	@Override
	public AlbumSource createAlbumSource(Provider provider, AlbumSet set) {
		return new MediaAlbumSource(provider, set, this);
	}
	
	@Override
	public AlbumDataSets createAlbumDataSets(AlbumSource source) { 
		return new AlbumDataSets(new AlbumCursorFactory());
	}
	
	@Override
	public AlbumBinder createAlbumBinder(AlbumSource source, ViewType.Type type) { 
		return new MediaAlbumBinderLarge((MediaAlbumSource)source);
	}
	
	@Override
	public void onCreateAlbum(Activity activity, AlbumSet albumSet) {
	}
	
	@Override
	public boolean showAlbum(PhotoSet photoSet) { 
		return photoSet != null && photoSet.getItemCount() > 0;
	}
	
}
