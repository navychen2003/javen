package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.provider.people.BaseGroupInfoItem;
import org.javenstudio.provider.people.group.GroupAction;

final class FlickrGroup extends BaseGroupInfoItem {

	private final FlickrPhotoProvider mPhotoProvider;
	private final FlickrTopicProvider mTopicProvider;
	
	private YGroupInfoEntry mInfoEntry = null;
	private GroupAction[] mActions = null;
	
	public FlickrGroup(FlickrGroupInfoProvider p, YGroupEntry data, int iconRes, 
			FlickrUserClickListener listener, FlickrPhotoProvider.FlickrPhotoFactory factory) { 
		super(p, data);
		mPhotoProvider = new FlickrPhotoProvider(data.groupId, iconRes, 
				FlickrPhotoSet.newGroupSet(p.getApplication(), data.groupId, iconRes), factory);
		mTopicProvider = new FlickrTopicProvider(
				data.groupId, data.groupId, iconRes, listener);
		
		getGroupInfoEntry();
	}
	
	public final YGroupEntry getGroupEntry() { return (YGroupEntry)getGroupData(); }
	public final String getGroupId() { return getGroupEntry().groupId; }
	
	public FlickrPhotoProvider getPhotoProvider() { return mPhotoProvider; }
	public FlickrTopicProvider getTopicProvider() { return mTopicProvider; }
	
	@Override
	public synchronized GroupAction[] getActionItems(IActivity activity) { 
		if (mActions == null) { 
			mActions = new GroupAction[] { 
					new FlickrGroupDetails(this), 
					new FlickrGroupPhotos(this, mPhotoProvider), 
					new FlickrGroupTopics(this, mTopicProvider), 
					//new GroupAction(this, activity.getActivity().getString(R.string.label_action_member))
				};
		}
		return mActions; 
	}
	
	@Override
	public GroupAction getSelectedAction() { 
		GroupAction[] actions = mActions;
		if (actions != null) { 
			for (GroupAction action : actions) { 
				if (action.isSelected())
					return action;
			}
		}
		return null; 
	}
	
	public synchronized YGroupInfoEntry getGroupInfoEntry() { 
		if (mInfoEntry == null) 
			fetchGroupInfoEntry(false);
		return mInfoEntry; 
	}
	
	public synchronized void fetchGroupInfoEntry(boolean refetch) { 
		FlickrHelper.fetchGroupInfo(getGroupId(), 
			new YGroupInfoEntry.FetchListener() {
				public void onGroupInfoFetching(String source) {}
				@Override
				public void onGroupInfoFetched(YGroupInfoEntry entry) {
					if (entry != null && entry.groupId != null && 
						entry.groupId.length() > 0) {
						setGroupInfoEntry(entry);
					}
				}
			}, false, refetch);
	}
	
	synchronized void setGroupInfoEntry(YGroupInfoEntry entry) { 
		mInfoEntry = entry;
	}
	
	@Override
	public int getMemberCount() { 
		YGroupInfoEntry entry = mInfoEntry;
		if (entry != null) 
			return entry.members;
		
		return super.getMemberCount();
	}
	
	@Override
	public int getTopicCount() { 
		YGroupInfoEntry entry = mInfoEntry;
		if (entry != null) 
			return entry.topiccount;
		
		return super.getTopicCount();
	}
	
	@Override
	public int getPhotoCount() { 
		YGroupInfoEntry entry = mInfoEntry;
		if (entry != null) 
			return entry.poolcount;
		
		return super.getPhotoCount();
	}
	
}
