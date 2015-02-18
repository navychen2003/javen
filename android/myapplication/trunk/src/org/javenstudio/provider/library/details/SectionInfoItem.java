package org.javenstudio.provider.library.details;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.account.AccountApp;
import org.javenstudio.android.account.AccountUser;
import org.javenstudio.android.app.FileOperation;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.ProviderActionTab;
import org.javenstudio.provider.library.ISectionInfoData;

public abstract class SectionInfoItem extends DataBinderItem 
		implements ProviderActionTab.TabItem, ImageListener {
	private static final Logger LOG = Logger.getLogger(SectionInfoItem.class);

	private static final String HEADER_VIEW = "headerView";
	private static final String ACTION_VIEW = "actionView";
	private static final String LIST_VIEW = "listView";
	
	private final AccountApp mAccountApp;
	private final AccountUser mAccountUser;
	
	private int mFetchRequest = 0;
	
	public SectionInfoItem(AccountApp app, AccountUser user) { 
		if (app == null || user == null) throw new NullPointerException();
		mAccountApp = app;
		mAccountUser = user;
	}
	
	public abstract Provider getProvider();
	public abstract ISectionInfoData getSectionData();
	
	public AccountApp getAccountApp() { return mAccountApp; }
	public AccountUser getAccountUser() { return mAccountUser; }
	
	public int getStatisticCount(int type) { return 0; }
	public SectionActionTab[] getActionItems(IActivity activity) { return null; }
	public SectionActionTab getSelectedAction() { return null; }
	
	public String getSectionId() { return getSectionData().getId(); }
	public String getSectionName() { return getSectionData().getName(); }
	public String getSectionTitle() { return getSectionName(); }
	public Drawable getSectionIcon() { return getSectionData().getTypeIcon(); }
	
	public boolean onItemDownload(IActivity activity) { return false; }
	
	public String getPosterLocation() { return null; }
	public String getBackgroundLocation() { return null; }
	
	public Image getPosterImage() { return null; }
	public Image getBackgroundImage() { return null; }
	
	public int getImageWidth() { return 0; }
	public int getImageHeight() { return 0; }
	
	public Drawable getProviderIcon() { return null; }
	public boolean isFetching() { return mFetchRequest > 0; }
	
	void setHeaderView(Activity activity, View view) { setBindView(activity, view, HEADER_VIEW); }
	public final View getHeaderView() { return getBindView(HEADER_VIEW); }
	
	void setActionView(Activity activity, View view) { setBindView(activity, view, ACTION_VIEW); }
	public final View getActionView() { return getBindView(ACTION_VIEW); }
	
	void setListView(Activity activity, View view) { setBindView(activity, view, LIST_VIEW); }
	public final View getListView() { return getBindView(LIST_VIEW); }
	
	public boolean supportChangePoster() {
		ISectionInfoData data= getSectionData();
		if (data != null) return data.supportOperation(FileOperation.Operation.CHANGEPOSTER);
		return false;
	}
	
	protected boolean hasImageLocation(String location) {
		if (location == null) return false;
		
		String avatarLocation = getPosterLocation();
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
	
	public boolean isImageDownloaded() {
		Image image = getPosterImage();
		if (image != null && image instanceof HttpImage) {
			boolean notdown = HttpImageItem.checkNotDownload((HttpImage)image);
			if (notdown) return false;
			return true;
		}
		return true;
	}
	
	public Drawable getImageDrawable(int width, int height) { 
		Image image = getPosterImage();
		if (image != null) {
			return image.getThumbnailDrawable(width, height);
		}
		return null;
	}
	
	public Drawable getPosterDrawable(int size, int padding) { 
		Image image = getPosterImage();
		if (image != null) { 
			return image.getThumbnailDrawable(size, size, 
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
