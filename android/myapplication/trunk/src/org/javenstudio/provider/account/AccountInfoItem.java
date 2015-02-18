package org.javenstudio.provider.account;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderActionTab;

public abstract class AccountInfoItem extends DataBinderItem 
		implements ProviderActionTab.TabItem, ImageListener {
	private static final Logger LOG = Logger.getLogger(AccountInfoItem.class);

	private static final String HEADER_VIEW = "headerView";
	private static final String ACTION_VIEW = "actionView";
	private static final String LIST_VIEW = "listView";
	
	private final AccountApp mAccountApp;
	private final AccountUser mAccountUser;
	
	private int mFetchRequest = 0;
	
	public AccountInfoItem(AccountApp app, AccountUser user) { 
		if (app == null || user == null) throw new NullPointerException();
		mAccountApp = app;
		mAccountUser = user;
	}
	
	public abstract Provider getProvider();
	
	public AccountApp getAccountApp() { return mAccountApp; }
	public AccountUser getAccountUser() { return mAccountUser; }
	
	public String getAccountName() { return getAccountUser().getAccountName(); }
	public String getAccountFullname() { return getAccountUser().getAccountFullname(); }
	public String getHostDisplayName() { return getAccountUser().getHostDisplayName(); }
	public String getUserTitle() { return getAccountUser().getUserTitle(); }
	public String getUserEmail() { return getAccountUser().getUserEmail(); }
	public String getNickName() { return getAccountUser().getNickName(); }
	
	public int getStatisticCount(int type) { return 0; }
	public AccountActionTab[] getActionItems(IActivity activity) { return null; }
	public AccountActionTab getSelectedAction() { return null; }
	
	public String getAvatarLocation() { return null; }
	public String getBackgroundLocation() { return null; }
	
	public Image getAvatarImage() { return null; }
	public Image getBackgroundImage() { return null; }
	
	public Drawable getProviderIcon() { return null; }
	public boolean isFetching() { return mFetchRequest > 0; }
	
	void setHeaderView(Activity activity, View view) { setBindView(activity, view, HEADER_VIEW); }
	public final View getHeaderView() { return getBindView(HEADER_VIEW); }
	
	void setActionView(Activity activity, View view) { setBindView(activity, view, ACTION_VIEW); }
	public final View getActionView() { return getBindView(ACTION_VIEW); }
	
	void setListView(Activity activity, View view) { setBindView(activity, view, LIST_VIEW); }
	public final View getListView() { return getBindView(LIST_VIEW); }
	
	protected boolean hasImageLocation(String location) {
		if (location == null) return false;
		
		String avatarLocation = getAvatarLocation();
		if (avatarLocation != null && location.equals(avatarLocation))
			return true;
		
		String backgroundLocation = getBackgroundLocation();
		if (backgroundLocation != null && location.equals(backgroundLocation))
			return true;
		
		return false;
	}
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!hasImageLocation(location)) return;
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
	
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		if (image == null) return;
		postUpdateView();
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					image.invalidateDrawables();
				}
			});
	}
	
	public final void postUpdateView() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					setCachedImageDrawable(null);
					onUpdateViewOnVisible(false);
				}
			});
	}
	
	protected void onUpdateViewOnVisible(boolean restartSlide) {}
	
	public Drawable getImageDrawable(final int width, final int height) { 
		return null;
	}
	
	public Drawable getAvatarRoundDrawable(int size, int padding) { 
		Image image = getAvatarImage();
		if (image != null) { 
			return image.getRoundThumbnailDrawable(size, size, 
					padding, padding, padding, padding);
		}
		return null;
	}
	
	public Drawable getBackgroundDrawable(final int width, final int height) { 
		Drawable background = null;
		
		Image backgroundImage = getBackgroundImage();
		if (backgroundImage != null) 
			background = backgroundImage.getThumbnailDrawable(width, height);
		
		return background;
	}
	
}
