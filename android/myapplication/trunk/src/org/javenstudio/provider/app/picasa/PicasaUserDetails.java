package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.app.R;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.people.user.UserDetails;
import org.javenstudio.provider.people.user.UserDetailsProfile;

final class PicasaUserDetails extends UserDetails {

	public PicasaUserDetails(PicasaUser user) { 
		super(user, ResourceHelper.getResources().getString(R.string.label_action_about));
		
		addDetailsItem(new PicasaDetailsProfile(user));
		setDefault(true);
	}
	
	private static class PicasaDetailsProfile extends UserDetailsProfile { 
		public PicasaDetailsProfile(PicasaUser user) {
			addProfileItem(R.string.details_realname, user.getUserTitle());
		}
	}
	
}
