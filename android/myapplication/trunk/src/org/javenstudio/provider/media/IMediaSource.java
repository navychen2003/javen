package org.javenstudio.provider.media;

import android.app.Activity;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderBinder;
import org.javenstudio.provider.ProviderCallback;

public interface IMediaSource {

	public Provider getProvider();
	public ProviderBinder getBinder();
	
	public void reloadData(ProviderCallback callback, ReloadType type, long reloadId);
	
	public String getName();
	public String getTitle(IActivity activity);
	public String getSubTitle(IActivity activity);
	
	public int getIconRes();
	public boolean isDefault();
	
	public void onCreateAlbum(Activity activity);
	
}
