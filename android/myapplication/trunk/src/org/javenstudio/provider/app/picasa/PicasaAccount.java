package org.javenstudio.provider.app.picasa;

import org.javenstudio.android.account.SystemUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.provider.account.AccountActionTab;
import org.javenstudio.provider.app.BaseAccountInfoItem;

final class PicasaAccount extends BaseAccountInfoItem {

	private final PicasaAlbumProvider mAlbumProvider;
	private final PicasaCommentProvider mCommentProvider;
	
	private HttpImage mAvatar = null;
	private AccountActionTab[] mActions = null;
	
	public PicasaAccount(PicasaAccountProvider p, SystemUser account, int iconRes, 
			PicasaUserClickListener listener, PicasaAlbumProvider.PicasaAlbumFactory factory) { 
		super(p, account);
		String userId = account.getUserId();
		String userName = account.getUserTitle();
		mCommentProvider = new PicasaCommentProvider(userId, userName, iconRes, listener);
		mAlbumProvider = new PicasaAlbumProvider(userName, iconRes, 
				PicasaAlbumSet.newAccountAlbumSet(p.getApplication(), account, iconRes), factory);
	}
	
	public PicasaCommentProvider getCommentProvider() { return mCommentProvider; }
	public PicasaAlbumProvider getAlbumProvider() { return mAlbumProvider; }
	
	@Override
	public synchronized AccountActionTab[] getActionItems(IActivity activity) { 
		if (mActions == null) { 
			mActions = new AccountActionTab[] { 
					new PicasaAccountDetails(this), 
					new PicasaAccountAlbums(activity.getActivity(), this, mAlbumProvider),
					new PicasaAccountComments(activity.getActivity(), this, mCommentProvider)
				};
		}
		return mActions; 
	}
	
	@Override
	public AccountActionTab getSelectedAction() { 
		AccountActionTab[] actions = mActions;
		if (actions != null) { 
			for (AccountActionTab action : actions) { 
				if (action.isSelected())
					return action;
			}
		}
		return null; 
	}
	
	@Override
	public synchronized Image getAvatarImage() { 
		if (mAvatar == null) { 
			GAlbumEntry.ResultInfo resultInfo = 
					((PicasaAlbumSet)mAlbumProvider.getAlbumSet()).getResultInfo();
			if (resultInfo != null) { 
				String avatarURL = resultInfo.authorThumbnail;
				if (avatarURL != null && avatarURL.length() > 0) { 
					mAvatar = HttpResource.getInstance().getImage(avatarURL);
					mAvatar.addListener(this);
					
					HttpImageItem.requestDownload(mAvatar, false);
				}
			}
		}
		return mAvatar; 
	}
	
	@Override
	public String getAvatarLocation() { 
		Image avatarImage = getAvatarImage();
		if (avatarImage != null) return avatarImage.getLocation();
		return null; 
	}
	
	@Override
	public String getUserTitle() { 
		GAlbumEntry.ResultInfo resultInfo = 
				((PicasaAlbumSet)mAlbumProvider.getAlbumSet()).getResultInfo();
		if (resultInfo != null) { 
			String name = resultInfo.authorNickName;
			if (name != null && name.length() > 0) 
				return name;
		}
		return super.getUserTitle();
	}
	
}
