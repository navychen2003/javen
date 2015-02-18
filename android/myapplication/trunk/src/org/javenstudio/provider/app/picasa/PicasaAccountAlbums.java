package org.javenstudio.provider.app.picasa;

import android.content.Context;
import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.provider.account.AccountActionProvider;
import org.javenstudio.provider.media.album.AlbumBinder;

final class PicasaAccountAlbums extends AccountActionProvider {

	public PicasaAccountAlbums(Context context, PicasaAccount account, PicasaAlbumProvider p) { 
		super(account, p, context.getString(R.string.label_action_album));
		setDefault(true);
	}
	
	@Override
	public boolean bindListView(IActivity activity) { 
		PicasaAlbumProvider p = (PicasaAlbumProvider)getProvider();
		if (p != null && activity != null) {
			AlbumBinder binder = (AlbumBinder)p.getBinder();
			if (binder != null) {
				binder.bindListView(activity, 
						getTabItem().getListView(), getAdapter(activity));
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public ListAdapter getAdapter(IActivity activity) { 
		PicasaAlbumProvider p = (PicasaAlbumProvider)getProvider();
		if (p != null && activity != null) {
			AlbumBinder binder = (AlbumBinder)p.getBinder();
			if (binder != null) 
				return binder.createListAdapter(activity);
		}
		
		return null;
	}
	
}
