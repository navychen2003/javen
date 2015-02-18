package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.provider.media.album.AlbumSource;
import org.javenstudio.provider.people.BaseUserInfoProvider;
import org.javenstudio.provider.people.user.UserBinder;

public class PicasaUserInfoProvider extends BaseUserInfoProvider {

	private final AccountApp mApplication;
	private final PicasaUser mUser;
	private final PicasaUserBinder mBinder;
	
	public PicasaUserInfoProvider(AccountApp app, String userId, 
			String userName, String avatarURL, int iconRes, 
			PicasaUserClickListener listener, PicasaAlbumProvider.PicasaAlbumFactory factory) { 
		super(userId, iconRes);
		mApplication = app;
		mUser = new PicasaUser(this, userId, userName, avatarURL, iconRes, listener, factory);
		mBinder = new PicasaUserBinder(this);
	}
	
	public AccountApp getAccountApp() { return mApplication; }
	public PicasaUser getUserItem() { return mUser; }
	
	public PicasaCommentProvider getCommentProvider() { return getUserItem().getCommentProvider(); }
	public PicasaAlbumProvider getAlbumProvider() { return getUserItem().getAlbumProvider(); }
	public AlbumSource getAlbumSource() { return getAlbumProvider().getSource(); }

	@Override
	public UserBinder getBinder() {
		return mBinder;
	}
	
}
