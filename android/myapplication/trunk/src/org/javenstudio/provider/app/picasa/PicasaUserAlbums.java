package org.javenstudio.provider.app.picasa;

import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.media.album.AlbumBinder;
import org.javenstudio.provider.people.user.UserActionProvider;

final class PicasaUserAlbums extends UserActionProvider {

	public PicasaUserAlbums(PicasaUser user, PicasaAlbumProvider p) { 
		super(user, p, ResourceHelper.getResources().getString(R.string.label_action_album));
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
