package org.javenstudio.provider.app.anybox.user;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.provider.account.AccountActionTab;
import org.javenstudio.provider.app.BaseAccountInfoItem;
import org.javenstudio.provider.app.anybox.AnyboxAccount;

public class AnyboxAccountItem extends BaseAccountInfoItem {

	public static interface AccountFactory 
			extends AnyboxAccountDetails.DetailsFactory {
	}
	
	private final AnyboxAccount mUser;
	private final AccountFactory mFactory;
	
	private HttpImage mAvatar = null;
	private HttpImage mBackground = null;
	private AccountActionTab[] mActions = null;
	
	public AnyboxAccountItem(AnyboxAccountProvider p, 
			AnyboxAccount account, AccountFactory factory) { 
		super(p, account);
		mUser = account;
		mFactory = factory;
	}
	
	public AnyboxAccount getUser() { return mUser; }
	
	protected AnyboxAccountDetails createAccountDetails(IActivity activity) {
		return new AnyboxAccountDetails(this, mFactory);
	}
	
	@Override
	public synchronized AccountActionTab[] getActionItems(IActivity activity) { 
		if (mActions == null) { 
			mActions = new AccountActionTab[] { 
					createAccountDetails(activity)
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
			String imageURL = ((AnyboxAccount)getAccountUser()).getAvatarURL(192);
			if (imageURL != null && imageURL.length() > 0) { 
				mAvatar = HttpResource.getInstance().getImage(imageURL);
				mAvatar.addListener(this);
				
				HttpImageItem.requestDownload(mAvatar, false);
			}
		}
		return mAvatar; 
	}
	
	@Override
	public synchronized Image getBackgroundImage() { 
		if (mBackground == null) { 
			String imageURL = ((AnyboxAccount)getAccountUser()).getBackgroundURL(1080, 1240);
			if (imageURL != null && imageURL.length() > 0) { 
				mBackground = HttpResource.getInstance().getImage(imageURL);
				mBackground.addListener(this);
				
				HttpImageItem.requestDownload(mBackground, false);
			}
		}
		return mBackground; 
	}
	
	@Override
	public String getAvatarLocation() { 
		Image avatarImage = getAvatarImage();
		if (avatarImage != null) return avatarImage.getLocation();
		return null; 
	}
	
	@Override
	public String getBackgroundLocation() { 
		Image bgImage = getBackgroundImage();
		if (bgImage != null) return bgImage.getLocation();
		return null; 
	}
	
}
