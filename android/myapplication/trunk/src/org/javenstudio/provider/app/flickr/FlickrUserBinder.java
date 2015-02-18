package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.people.user.UserBinder;
import org.javenstudio.provider.people.user.UserItem;

final class FlickrUserBinder extends UserBinder {

	private final FlickrUserInfoProvider mProvider;
	
	public FlickrUserBinder(FlickrUserInfoProvider p) { 
		mProvider = p;
	}
	
	@Override
	protected FlickrUserInfoProvider getProvider() { 
		return mProvider; 
	}
	
	@Override
	protected void requestDownload(final IActivity activity, UserItem item) { 
		super.requestDownload(activity, item);
		requestUserInfo(activity, item);
	}
	
	private void requestUserInfo(final IActivity activity, UserItem item) {
		final FlickrUser user = (FlickrUser)item;
		YUserInfoEntry info = user.getUserInfoEntry();
		if (info == null) { 
			// disable other request
			user.setUserInfoEntry(new YUserInfoEntry());
			
			FlickrHelper.fetchUserInfo(user.getUserId(), 
				new YUserInfoEntry.FetchListener() {
					@Override
					public void onUserInfoFetching(String source) { 
						activity.postShowProgress(false);
					}
					@Override
					public void onUserInfoFetched(YUserInfoEntry entry) {
						if (entry != null && entry.userId != null && 
							entry.userId.length() > 0) {
							user.setUserInfoEntry(entry);
							user.postUpdateViews();
						}
						activity.postHideProgress(false);
					}
				}, true);
		}
	}
	
}
