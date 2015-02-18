package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.provider.people.BaseUserInfoItem;
import org.javenstudio.provider.people.user.UserAction;

final class FlickrUser extends BaseUserInfoItem {

	private final FlickrAlbumProvider mAlbumProvider;
	private final FlickrContactProvider mContactProvider;
	private final FlickrPhotoProvider mFavoriteProvider;
	private final FlickrGroupListProvider mGroupProvider;
	
	private YUserInfoEntry mInfoEntry = null;
	private HttpImage mAvatar = null;
	private UserAction[] mActions = null;
	
	public FlickrUser(FlickrUserInfoProvider p, String userId, int iconRes, 
			FlickrUserClickListener listener, FlickrAlbumProvider.PicasaAlbumFactory albumFactory, 
			FlickrPhotoProvider.FlickrPhotoFactory photoFactory) { 
		super(p, userId);
		mContactProvider = new FlickrContactProvider(userId, userId, iconRes, listener);
		mAlbumProvider = new FlickrAlbumProvider(userId, iconRes, 
				FlickrAlbumSet.newAlbumSet(p.getApplication(), userId, iconRes), albumFactory);
		mFavoriteProvider = new FlickrPhotoProvider(userId, iconRes, 
				new FlickrFavoriteSet(p.getApplication(), userId, iconRes), photoFactory);
		mGroupProvider = FlickrGroupListProvider.newUserProvider(userId, userId, iconRes);
		
		getUserInfoEntry();
	}
	
	public FlickrAlbumProvider getAlbumProvider() { return mAlbumProvider; }
	public FlickrContactProvider getContactProvider() { return mContactProvider; }
	public FlickrPhotoProvider getFavoriteProvider() { return mFavoriteProvider; }
	public FlickrGroupListProvider getGroupProvider() { return mGroupProvider; }
	
	@Override
	public synchronized UserAction[] getActionItems(IActivity activity) { 
		if (mActions == null) { 
			mActions = new UserAction[] { 
					new FlickrUserDetails(this), 
					new FlickrUserAlbums(this, mAlbumProvider), 
					new FlickrUserFavorites(this, mFavoriteProvider), 
					new FlickrUserGroups(this, mGroupProvider), 
					new FlickrUserContacts(this, mContactProvider)
				};
		}
		return mActions; 
	}
	
	@Override
	public UserAction getSelectedAction() { 
		UserAction[] actions = mActions;
		if (actions != null) { 
			for (UserAction action : actions) { 
				if (action.isSelected())
					return action;
			}
		}
		return null; 
	}
	
	public synchronized YUserInfoEntry getUserInfoEntry() { 
		if (mInfoEntry == null) 
			fetchUserInfoEntry(false);
		return mInfoEntry; 
	}
	
	public synchronized void fetchUserInfoEntry(boolean refetch) { 
		FlickrHelper.fetchUserInfo(getUserId(), 
			new YUserInfoEntry.FetchListener() {
				public void onUserInfoFetching(String source) {}
				@Override
				public void onUserInfoFetched(YUserInfoEntry entry) {
					if (entry != null && entry.userId != null && 
						entry.userId.length() > 0) {
						setUserInfoEntry(entry);
					}
				}
			}, false, refetch);
	}
	
	synchronized void setUserInfoEntry(YUserInfoEntry entry) { 
		mInfoEntry = entry;
		
		if (entry != null && mAvatar == null) { 
			String iconURL = FlickrHelper.getIconURL(
					entry.userId, entry.iconfarm, entry.iconserver);
			
			if (iconURL != null)
				mAvatar = HttpResource.getInstance().getImage(iconURL);
		}
	}
	
	public String getAvatarLocation() { 
		//getUserInfoEntry();
		return mAvatar != null ? mAvatar.getLocation() : null; 
	}
	
	@Override
	public Image getAvatarImage() { 
		//getUserInfoEntry();
		return mAvatar; 
	}
	
	@Override
	public String getUserTitle() {
		YUserInfoEntry entry = mInfoEntry; //getUserInfoEntry();
		if (entry != null) 
			return entry.username;
		
		return null;
	}
	
	@Override
	public int getStatisticCount(int type) { 
		YUserInfoEntry entry = mInfoEntry; //getUserInfoEntry();
		if (entry != null) {
			if (type == COUNT_PHOTO)
				return entry.photos_count;
		}
		
		return 0; 
	}
	
}
