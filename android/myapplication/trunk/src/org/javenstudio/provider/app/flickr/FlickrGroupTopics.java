package org.javenstudio.provider.app.flickr;

import android.widget.ListAdapter;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.people.group.GroupActionProvider;

final class FlickrGroupTopics extends GroupActionProvider {

	public FlickrGroupTopics(FlickrGroup group, FlickrTopicProvider p) { 
		super(group, p, ResourceHelper.getResources().getString(R.string.label_action_topic));
	}
	
	@Override
	public boolean bindListView(IActivity activity) { 
		FlickrTopicProvider p = (FlickrTopicProvider)getProvider();
		if (p != null && activity != null) {
			FlickrTopicBinder binder = (FlickrTopicBinder)p.getBinder();
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
		FlickrTopicProvider p = (FlickrTopicProvider)getProvider();
		if (p != null && activity != null) {
			FlickrTopicBinder binder = (FlickrTopicBinder)p.getBinder();
			if (binder != null) 
				return binder.createListAdapter(activity);
		}

		return null;
	}
	
}
