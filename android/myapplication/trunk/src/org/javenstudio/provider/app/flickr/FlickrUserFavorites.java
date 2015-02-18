package org.javenstudio.provider.app.flickr;

import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.media.photo.PhotoBinder;
import org.javenstudio.provider.people.user.UserActionProvider;

final class FlickrUserFavorites extends UserActionProvider {

	public FlickrUserFavorites(FlickrUser user, FlickrPhotoProvider p) { 
		super(user, p, ResourceHelper.getResources().getString(R.string.label_action_favorite));
	}
	
	@Override
	public boolean bindListView(IActivity activity) { 
		FlickrPhotoProvider p = (FlickrPhotoProvider)getProvider();
		if (p != null && activity != null) {
			PhotoBinder binder = (PhotoBinder)p.getBinder();
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
		FlickrPhotoProvider p = (FlickrPhotoProvider)getProvider();
		if (p != null && activity != null) {
			PhotoBinder binder = (PhotoBinder)p.getBinder();
			if (binder != null) 
				return binder.createListAdapter(activity);
		}

		return null;
	}
	
}
