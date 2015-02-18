package org.javenstudio.provider.app.flickr;

import org.javenstudio.android.data.DataApp;
import org.javenstudio.provider.media.album.AlbumSource;
import org.javenstudio.provider.media.photo.PhotoSource;
import org.javenstudio.provider.people.BaseUserInfoProvider;
import org.javenstudio.provider.people.user.UserBinder;

public class FlickrUserInfoProvider extends BaseUserInfoProvider {

	private final DataApp mApplication;
	private final FlickrUser mUser;
	private final FlickrUserBinder mBinder;
	
	public FlickrUserInfoProvider(DataApp app, String userId, int iconRes, 
			FlickrUserClickListener listener, FlickrAlbumProvider.PicasaAlbumFactory albumFactory, 
			FlickrPhotoProvider.FlickrPhotoFactory photoFactory) { 
		super(userId, iconRes);
		mApplication = app;
		mUser = new FlickrUser(this, userId, iconRes, listener, albumFactory, photoFactory);
		mBinder = new FlickrUserBinder(this);
	}
	
	public DataApp getApplication() { return mApplication; }
	public FlickrUser getUserItem() { return mUser; }
	
	public FlickrAlbumProvider getAlbumProvider() { return getUserItem().getAlbumProvider(); }
	public AlbumSource getAlbumSource() { return getAlbumProvider().getSource(); }
	
	public FlickrPhotoProvider getFavoriteProvider() { return getUserItem().getFavoriteProvider(); }
	public PhotoSource getFavoriteSource() { return getFavoriteProvider().getSource(); }

	public FlickrContactProvider getContactProvider() { return getUserItem().getContactProvider(); }
	public FlickrGroupListProvider getGroupProvider() { return getUserItem().getGroupProvider(); }
	
	@Override
	public UserBinder getBinder() {
		return mBinder;
	}
	
}
