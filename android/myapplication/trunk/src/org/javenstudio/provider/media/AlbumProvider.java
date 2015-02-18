package org.javenstudio.provider.media;

import org.javenstudio.android.app.SelectMode;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.data.media.AlbumSet;
import org.javenstudio.provider.ProviderBase;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.media.album.AlbumFactory;
import org.javenstudio.provider.media.album.AlbumSource;

public abstract class AlbumProvider extends ProviderBase {

	protected final AlbumSource mSource;
	
	public AlbumProvider(String name, int iconRes, 
			AlbumSet set, AlbumFactory factory) { 
		super(name, iconRes);
		mSource = factory.createAlbumSource(this, set);
	}
	
	public AlbumSource getSource() { return mSource; }
	public AlbumSet getAlbumSet() { return mSource.getAlbumSet(); }
	
	public SelectMode.Callback getSelectCallback() { return mSource; }
	
	@Override
	public final ProviderBinder getBinder() { 
		return mSource.getBinder();
	}
	
	@Override
	public synchronized void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) { 
		callback.clearParams();
		
		mSource.reloadData(callback, type, reloadId);
	}
	
}
