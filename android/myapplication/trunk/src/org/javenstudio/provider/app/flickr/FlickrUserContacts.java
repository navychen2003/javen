package org.javenstudio.provider.app.flickr;

import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.people.user.UserActionProvider;

final class FlickrUserContacts extends UserActionProvider {

	public FlickrUserContacts(FlickrUser user, FlickrContactProvider p) { 
		super(user, p, ResourceHelper.getResources().getString(R.string.label_action_contact));
	}
	
	@Override
	public boolean bindListView(IActivity activity) { 
		FlickrContactProvider p = (FlickrContactProvider)getProvider();
		if (p != null && activity != null) {
			FlickrContactBinder binder = (FlickrContactBinder)p.getBinder();
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
		FlickrContactProvider p = (FlickrContactProvider)getProvider();
		if (p != null && activity != null) {
			FlickrContactBinder binder = (FlickrContactBinder)p.getBinder();
			if (binder != null) 
				return binder.createListAdapter(activity);
		}

		return null;
	}
	
}
