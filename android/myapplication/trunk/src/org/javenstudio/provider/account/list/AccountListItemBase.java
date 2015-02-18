package org.javenstudio.provider.account.list;

import android.graphics.drawable.Drawable;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.android.entitydb.content.AccountData;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class AccountListItemBase extends AccountListItem 
		implements ImageListener {
	private static final Logger LOG = Logger.getLogger(AccountListItemBase.class);
	
	private final AccountApp mApp;
	private final AccountData mAccount;
	
	private HttpImage mImage = null;
	private String mImageURL = null;
	private int mFetchRequest = 0;
	
	public AccountListItemBase(AccountApp app, AccountData account) {
		if (app == null || account == null) throw new NullPointerException();
    	mAccount = account;
    	mApp = app;
    }
    
    public AccountData getAccount() { return mAccount; }
    public AccountApp getAccountApp() { return mApp; }

    protected abstract AccountHelper.OnRemoveListener getRemoveListener();
    
    @Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!isImageLocation(location)) return;
			if (LOG.isDebugEnabled())
				LOG.debug("onImageEvent: location=" + location + " event=" + event);
		}
		
		if (event instanceof HttpEvent) { 
			HttpEvent e = (HttpEvent)event;
			switch (e.getEventType()) { 
			case FETCH_START: 
				if (mFetchRequest < 0) mFetchRequest = 0;
				mFetchRequest ++;
				break;
			default: 
				mFetchRequest --;
				if (mFetchRequest < 0) mFetchRequest = 0;
				break;
			}
			
			onHttpImageEvent(image, e);
		}
	}
    
	public Drawable getAvatarDrawable(int size, int padding) {
		HttpImage image = getImage();
		if (image != null) return image.getRoundThumbnailDrawable(size, size);
		return null;
	}
	
	private synchronized HttpImage getImage() {
		if (mImage == null) {
			String imageURL = getAccountApp().getAccountAvatarURL(getAccount(), 192);
			
			if (imageURL != null && imageURL.length() > 0) { 
				mImageURL = imageURL;
				mImage = HttpResource.getInstance().getImage(imageURL);
				mImage.addListener(this);
				
				HttpImageItem.requestDownload(mImage, false);
			}
		}
		return mImage;
	}
	
	protected boolean isImageLocation(String location) {
		if (location != null && location.length() > 0) {
			String imageURL = mImageURL;
			if (imageURL != null && imageURL.equals(location))
				return true;
		}
		return false;
	}
    
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		if (image == null) return;
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					image.invalidateDrawables();
				}
			});
	}
    
	protected void onAccountClick(IActivity activity) {
		if (activity == null) return;
		AccountHelper.onAccountLogin(activity.getActivity(), 
				getAccountApp(), getAccount());
	}
	
	protected void onAccountRemove(final IActivity activity) {
		if (activity == null) return;
		AccountHelper.onAccountRemove(activity.getActivity(), 
			getAccountApp(), getAccount(), getRemoveListener());
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "-" + mIdentity 
				+ "{account=" + getAccount() + "}";
	}
	
}
