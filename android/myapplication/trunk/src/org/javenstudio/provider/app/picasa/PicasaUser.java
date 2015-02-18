package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.provider.people.BaseUserInfoItem;
import org.javenstudio.provider.people.user.UserAction;

final class PicasaUser extends BaseUserInfoItem {

	private final PicasaAlbumProvider mAlbumProvider;
	private final PicasaCommentProvider mCommentProvider;
	private final HttpImage mAvatar;
	
	private UserAction[] mActions = null;
	
	public PicasaUser(PicasaUserInfoProvider p, String userId, 
			String userName, String avatarURL, int iconRes, 
			PicasaUserClickListener listener, PicasaAlbumProvider.PicasaAlbumFactory factory) { 
		super(p, userId, userName);
		mCommentProvider = new PicasaCommentProvider(userId, userName, iconRes, listener);
		mAlbumProvider = new PicasaAlbumProvider(userName, iconRes, 
				PicasaAlbumSet.newAlbumSet(p.getAccountApp(), userId, userName, avatarURL, iconRes), factory);
		mAvatar = HttpResource.getInstance().getImage(avatarURL);
		mAvatar.addListener(this);
	}
	
	public PicasaCommentProvider getCommentProvider() { return mCommentProvider; }
	public PicasaAlbumProvider getAlbumProvider() { return mAlbumProvider; }
	public Image getAvatarImage() { return mAvatar; }
	
	@Override
	public synchronized UserAction[] getActionItems(IActivity activity) { 
		if (mActions == null) { 
			mActions = new UserAction[] { 
					new PicasaUserDetails(this), 
					new PicasaUserAlbums(this, mAlbumProvider), 
					new PicasaUserComments(this, mCommentProvider)
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
	
}
