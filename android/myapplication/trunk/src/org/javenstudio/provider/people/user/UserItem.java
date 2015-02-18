package org.javenstudio.provider.people.user;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.account.AppUser;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.provider.ProviderActionTab;

public abstract class UserItem extends DataBinderItem 
		implements AppUser, ProviderActionTab.TabItem, ImageListener {

	private static final String HEADER_VIEW = "headerView";
	private static final String LIST_VIEW = "listView";
	
	private final String mUserId;
	private final String mUserName;
	
	private int mFetchRequest = 0;
	
	public UserItem(String userId) { 
		this(userId, null);
	}
	
	public UserItem(String userId, String userName) { 
		mUserId = userId;
		mUserName = userName;
	}
	
	public String getUserId() { return mUserId; }
	public String getUserTitle() { return mUserName; }
	
	public int getStatisticCount(int type) { return 0; }
	public UserAction[] getActionItems(IActivity activity) { return null; }
	public UserAction getSelectedAction() { return null; }
	
	public String getAvatarLocation() { return null; }
	public Image getAvatarImage() { return null; }
	public Image getBackgroundImage() { return null; }
	
	public Drawable getProviderIcon() { return null; }
	public boolean isFetching() { return mFetchRequest > 0; }
	
	void setHeaderView(Activity activity, View view) { setBindView(activity, view, HEADER_VIEW); }
	public final View getHeaderView() { return getBindView(HEADER_VIEW); }
	
	void setListView(Activity activity, View view) { setBindView(activity, view, LIST_VIEW); }
	public final View getListView() { return getBindView(LIST_VIEW); }
	
	public Drawable getAvatarRoundDrawable(int size, int padding) { 
		Image image = getAvatarImage();
		if (image != null) { 
			//int size = ResourceHelper.getResources().getDimensionPixelSize(R.dimen.headerinfo_avatar_size);
			return image.getRoundThumbnailDrawable(size, size, padding, padding, padding, padding);
		}
		return null;
	}
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!location.equals(getAvatarLocation())) 
				return;
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
		postUpdateViews();
	}
	
	public void postUpdateViews() {
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					setCachedImageDrawable(null);
					onUpdateViews();
				}
			});
	}
	
	protected abstract void onUpdateViews();
	
	public Drawable getBackgroundDrawable(final int width, final int height) { 
		Drawable background = null;
		
		Image backgroundImage = getBackgroundImage();
		if (backgroundImage != null) 
			background = backgroundImage.getThumbnailDrawable(width, height);
		
		return background;
	}
	
}
