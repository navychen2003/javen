package org.javenstudio.provider.media.album;

import android.app.Activity;

import org.javenstudio.android.app.ViewType;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.provider.Provider;

public interface AlbumFactory {

	public AlbumSource createAlbumSource(Provider provider, AlbumSet set);
	
	public AlbumDataSets createAlbumDataSets(AlbumSource source);
	public AlbumBinder createAlbumBinder(AlbumSource source, ViewType.Type type);
	
	public void onCreateAlbum(Activity activity, AlbumSet albumSet);
	public boolean showAlbum(PhotoSet photoSet);
	
	public ViewType getViewType();
	
}
