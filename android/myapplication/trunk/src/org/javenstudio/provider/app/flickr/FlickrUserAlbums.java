package org.javenstudio.provider.app.flickr;

import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.media.album.AlbumBinder;
import org.javenstudio.provider.people.user.UserActionProvider;

final class FlickrUserAlbums extends UserActionProvider {

	public FlickrUserAlbums(FlickrUser user, FlickrAlbumProvider p) { 
		super(user, p, ResourceHelper.getResources().getString(R.string.label_action_album));
	}
	
	@Override
	public boolean bindListView(IActivity activity) { 
		FlickrAlbumProvider p = (FlickrAlbumProvider)getProvider();
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
		FlickrAlbumProvider p = (FlickrAlbumProvider)getProvider();
		if (p != null && activity != null) {
			AlbumBinder binder = (AlbumBinder)p.getBinder();
			if (binder != null) 
				return binder.createListAdapter(activity);
		}

		return null;
	}
	
}
