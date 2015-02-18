package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.people.group.GroupBinder;
import org.javenstudio.provider.people.group.GroupItem;

final class FlickrGroupBinder extends GroupBinder {

	private final FlickrGroupInfoProvider mProvider;
	
	public FlickrGroupBinder(FlickrGroupInfoProvider p) { 
		mProvider = p;
	}
	
	@Override
	protected FlickrGroupInfoProvider getProvider() { 
		return mProvider;
	}
	
	@Override
	protected void requestDownload(IActivity activity, GroupItem item) { 
		super.requestDownload(activity, item);
		requestGroupInfo(activity, item);
	}
	
	private void requestGroupInfo(final IActivity activity, GroupItem item) {
		final FlickrGroup group = (FlickrGroup)item;
		YGroupInfoEntry info = group.getGroupInfoEntry();
		if (info == null) { 
			// disable other request
			group.setGroupInfoEntry(new YGroupInfoEntry());
			
			FlickrHelper.fetchGroupInfo(group.getGroupId(), 
				new YGroupInfoEntry.FetchListener() {
					@Override
					public void onGroupInfoFetching(String source) { 
						activity.postShowProgress(false);
					}
					@Override
					public void onGroupInfoFetched(YGroupInfoEntry entry) {
						if (entry != null && entry.groupId != null && 
							entry.groupId.length() > 0) {
							group.setGroupInfoEntry(entry);
							group.postUpdateViews();
						}
						activity.postHideProgress(false);
					}
				}, true);
		}
	}
	
}
