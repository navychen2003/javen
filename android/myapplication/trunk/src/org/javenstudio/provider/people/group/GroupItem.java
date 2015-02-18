package org.javenstudio.provider.people.group;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.image.http.HttpResource;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.ProviderActionTab;

public abstract class GroupItem extends DataBinderItem 
		implements ProviderActionTab.TabItem, ImageListener {
	private static final Logger LOG = Logger.getLogger(GroupItem.class);
	
	private static final String HEADER_VIEW = "headerView";
	private static final String LIST_VIEW = "listView";
	
	private final IGroupData mData;
	
	private HttpImage mAvatar = null;
	private int mFetchRequest = 0;
	
	public GroupItem(IGroupData data) { 
		mData = data;
		
		if (data == null) throw new NullPointerException();
	}
	
	public IGroupData getGroupData() { return mData; }
	
	public GroupAction[] getActionItems(IActivity activity) { return null; }
	public GroupAction getSelectedAction() { return null; }
	
	public CharSequence getSummary() { 
		return getGroupData().getSubTitle(); 
	}
	
	public String getTitle() { 
		return getGroupData().getGroupName(); 
	}
	
	public String getName() { 
		return ResourceHelper.getResources().getString(R.string.label_group_name); 
	}
	
	public String getGroupName() { 
		return getGroupData().getGroupName(); 
	}
	
	public int getMemberCount() { 
		return getGroupData().getMemberCount();
	}
	
	public int getTopicCount() { 
		return getGroupData().getTopicCount();
	}
	
	public int getPhotoCount() { 
		return getGroupData().getPhotoCount();
	}
	
	public View.OnClickListener getGroupClickListener() { 
		return getGroupData().getGroupClickListener(); 
	}
	
	public Drawable getProviderIcon() { 
		return getGroupData().getProviderIcon(); 
	}
	
	public String getCreateDate() { return null; }
	public boolean isFetching() { return mFetchRequest > 0; }
	
	void setHeaderView(Activity activity, View view) { setBindView(activity, view, HEADER_VIEW); }
	public final View getHeaderView() { return getBindView(HEADER_VIEW); }
	
	void setListView(Activity activity, View view) { setBindView(activity, view, LIST_VIEW); }
	public final View getListView() { return getBindView(LIST_VIEW); }
	
	public Image getBackgroundImage() { return null; }
	
	public synchronized Image getAvatarImage() { 
		if (mAvatar == null) {
			mAvatar = HttpResource.getInstance().getImage(
					getGroupData().getAvatarLocation());
			
			if (mAvatar != null)
				mAvatar.addListener(this);
			
			HttpImageItem.requestDownload(mAvatar, false);
		}
		
		return mAvatar;
	}
	
	public Drawable getGroupDrawable(int width, int height) { 
		Image avatar = getAvatarImage();
		if (avatar != null) 
			return avatar.getThumbnailDrawable(width, height);
		
		return null;
	}
	
	public Drawable getGroupRoundDrawable(int size, int padding) { 
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
			if (mAvatar == null || !location.equals(mAvatar.getLocation())) 
				return;
		}
		
		if (event instanceof HttpEvent) { 
			HttpEvent e = (HttpEvent)event;
			
			if (LOG.isDebugEnabled())
				LOG.debug("onImageEvent: entry=" + this + " event=" + e.getEventType());
			
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
					onUpdateViewsOnVisible(false);
				}
			});
	}
	
	protected abstract void onUpdateViewsOnVisible(boolean restartSlide);
	
	public Drawable getBackgroundDrawable(final int width, final int height) { 
		Drawable background = null;
		
		Image backgroundImage = getBackgroundImage();
		if (backgroundImage != null) 
			background = backgroundImage.getThumbnailDrawable(width, height);
		
		return background;
	}
	
}
