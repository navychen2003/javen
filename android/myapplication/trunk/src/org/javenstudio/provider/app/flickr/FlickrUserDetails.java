package org.javenstudio.provider.app.flickr;

import android.content.Context;
import android.view.View;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.people.user.UserDetails;
import org.javenstudio.provider.people.user.UserDetailsProfile;

final class FlickrUserDetails extends UserDetails {

	private final FlickrUser mUser;
	
	public FlickrUserDetails(FlickrUser user) { 
		super(user, ResourceHelper.getResources().getString(R.string.label_action_about));
		mUser = user;
		
		addDetailsItem(new FlickrDetailsProfile());
		setDefault(true);
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {
		if (type == ReloadType.FORCE) { 
			mUser.fetchUserInfoEntry(true);
			postUpdateViews();
		}
	}
	
	private class FlickrDetailsProfile extends UserDetailsProfile { 
		private YUserInfoEntry mInfo = null;
		
		public FlickrDetailsProfile() {}
		
		@Override
		public View getView(Context context, View convertView, boolean dropDown) { 
			initProfileItems();
			return super.getView(context, convertView, dropDown);
		}
		
		private void initProfileItems() {
			YUserInfoEntry info = mUser.getUserInfoEntry();
			if (info != null && info != mInfo) { 
				clearProfileItems();
				
				addProfileItem(R.string.details_realname, info.realname);
				addProfileItem(R.string.details_user_location, info.location);
				addProfileItem(R.string.details_createdate, 
						Utilities.formatDate(info.datecreate));
				addProfileItem(R.string.details_description, 
						InformationHelper.formatContentSpanned(info.description));
				
				mInfo = info;
			}
		}
	}
	
}
