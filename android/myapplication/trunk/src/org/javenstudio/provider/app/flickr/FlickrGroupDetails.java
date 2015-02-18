package org.javenstudio.provider.app.flickr;

import android.content.Context;
import android.view.View;

import org.javenstudio.android.app.R;
import org.javenstudio.android.data.ReloadType;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.provider.ProviderCallback;
import org.javenstudio.provider.people.group.GroupDetails;
import org.javenstudio.provider.people.group.GroupDetailsProfile;

final class FlickrGroupDetails extends GroupDetails {

	private final FlickrGroup mGroup;
	
	public FlickrGroupDetails(FlickrGroup group) { 
		super(group, ResourceHelper.getResources().getString(R.string.label_action_about));
		mGroup = group;
		
		addDetailsItem(new FlickrDetailsProfile());
		addDetailsItem(new FlickrDetailsDesc());
		addDetailsItem(new FlickrDetailsRule());
		setDefault(true);
	}
	
	@Override
	public void reloadOnThread(ProviderCallback callback, 
			ReloadType type, long reloadId) {
		if (type == ReloadType.FORCE) { 
			mGroup.fetchGroupInfoEntry(true);
			postUpdateViews();
		}
	}
	
	private class FlickrDetailsProfile extends GroupDetailsProfile { 
		private YGroupInfoEntry mInfo = null;
		
		public FlickrDetailsProfile() {}
		
		@Override
		public View getView(Context context, View convertView, boolean dropDown) { 
			initProfileItems();
			return super.getView(context, convertView, dropDown);
		}
		
		private void initProfileItems() {
			YGroupInfoEntry info = mGroup.getGroupInfoEntry();
			if (info != null && info != mInfo) { 
				clearProfileItems();
				
				addProfileItem(R.string.details_groupname, info.name);
				addProfileItem(R.string.details_group_blastdate, 
						Utilities.formatDate(info.date_blast_added));
				addProfileItem(R.string.details_group_blast, 
						InformationHelper.formatContentSpanned(info.blast));
				
				mInfo = info;
			}
		}
	}
	
	private class FlickrDetailsDesc extends GroupDetailsProfile { 
		private YGroupInfoEntry mInfo = null;
		
		public FlickrDetailsDesc() {}
		
		public String getTitle() { 
			return ResourceHelper.getResources().getString(R.string.label_title_groupdesc); 
		}
		
		@Override
		public View getView(Context context, View convertView, boolean dropDown) { 
			initProfileItems();
			return super.getView(context, convertView, dropDown);
		}
		
		private void initProfileItems() {
			YGroupInfoEntry info = mGroup.getGroupInfoEntry();
			if (info != null && info != mInfo) { 
				clearProfileItems();
				
				addProfileItem(InformationHelper.formatContentSpanned(info.description));
				
				mInfo = info;
			}
		}
	}
	
	private class FlickrDetailsRule extends GroupDetailsProfile { 
		private YGroupInfoEntry mInfo = null;
		
		public FlickrDetailsRule() {}
		
		public String getTitle() { 
			return ResourceHelper.getResources().getString(R.string.label_title_grouprule); 
		}
		
		@Override
		public View getView(Context context, View convertView, boolean dropDown) { 
			initProfileItems();
			return super.getView(context, convertView, dropDown);
		}
		
		private void initProfileItems() {
			YGroupInfoEntry info = mGroup.getGroupInfoEntry();
			if (info != null && info != mInfo) { 
				clearProfileItems();
				
				addProfileItem(InformationHelper.formatContentSpanned(info.rules));
				
				mInfo = info;
			}
		}
	}
	
}
