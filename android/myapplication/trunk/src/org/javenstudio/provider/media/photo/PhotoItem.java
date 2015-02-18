package org.javenstudio.provider.media.photo;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.app.ActionHelper;
import org.javenstudio.android.app.IActivity;
import org.javenstudio.android.app.SelectManager;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.DataBinderListener;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImage;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.media.Photo;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.media.IMediaPhoto;

public class PhotoItem extends DataBinderItem implements ImageListener {
	private static final Logger LOG = Logger.getLogger(PhotoItem.class);
	
	private final PhotoSource mSource;
	private final IMediaPhoto mPhoto;
	private int mFetchRequest = 0;
	
	public PhotoItem(PhotoSource source, IMediaPhoto photo) { 
		if (source == null || photo == null) throw new NullPointerException();
		mSource = source;
		mPhoto = photo;;
		setImageListener();
	}
	
	public void setImageListener() {
		mPhoto.getBitmapImage().addListener(this);
		
		Image avatar = mPhoto.getAvatarImage();
		if (avatar != null) 
			avatar.addListener(this);
	}
	
	private PhotoDataSet mDataSet = null;
	
	void setDataSet(PhotoDataSet dataSet) { mDataSet = dataSet; }
	public PhotoDataSet getDataSet() { return mDataSet; }
	
	public final PhotoSource getSource() { return mSource; }
	public final IMediaPhoto getPhoto() { return mPhoto; }
	
	public Provider getProvider() { return mSource.getProvider(); }
	public PhotoDataSets getDataSets() { return mSource.getPhotoDataSets(); }
	
	public boolean isFetching() { return mFetchRequest > 0; }
	public boolean isActionModeEnabled() { return false; } //getProvider().isActionModeEnabled(); }
	
	public boolean isSelected(IActivity activity) { 
		if (activity == null) return false;
		
		Photo data = getPhoto().getPhotoData();
		SelectManager manager = getSource().getSelectManager(); 
		
		if (manager != null) 
			return manager.isSelectedItem(data);
		
		return false;
	}
	
	public void setSelected(IActivity activity, boolean selected) { 
		if (activity == null) return;
		
		Photo data = getPhoto().getPhotoData();
		SelectManager manager = getSource().getSelectManager(); 
		
		if (manager != null) 
			manager.setSelectedItem(data, selected);
	}
	
	public boolean isActionMode(IActivity activity) { 
		if (activity == null || !isActionModeEnabled()) 
			return false;
		
		ActionHelper helper = activity.getActionHelper();
		return helper != null ? helper.isSelectMode() : false;
	}
	
	public boolean enterActionMode(IActivity activity) { 
		if (activity == null || !isActionModeEnabled()) 
			return false;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("enterActionMode: itemId=" + getIdentity());
		
		ActionHelper helper = activity.getActionHelper();
		if (helper != null && !helper.isSelectMode()) { 
			//boolean result = helper.startSelectMode();
			//if (result) { 
			//	setSelected(activity, true); 
			//	rebindItems(activity); 
			//}
			//return result;
		}
		
		return false;
	}
	
	@SuppressWarnings("unused")
	private void rebindItems(IActivity activity) { 
		if (activity == null) return;
		
		Context context = activity.getActivity();
		PhotoDataSets dataSets = getDataSets();
		
		if (context == null || dataSets == null) 
			return;
		
		boolean binded = true;
		int rebindCount = 0;
		
		for (int i=0; i < dataSets.getCount(); i++) { 
			PhotoDataSet dataSet = dataSets.getPhotoDataSet(i);
			if (dataSet == null) continue;
			
			PhotoItem item = dataSet.getPhotoItem();
			if (item == null) continue;
			
			PhotoBinder binder = (PhotoBinder)getProvider().getBinder();
			View view = item.getBindView();
			
			if (binder == null || view == null) { 
				if (item.isVisible()) { 
					binded = false;
					break; 
				} else 
					continue;
			}
			
			binder.bindItemView(activity, item, view);
			rebindCount ++;
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("rebindItems: rebindCount=" + rebindCount + " binded=" + binded);
		
		if (!binded || rebindCount <= 0) 
			activity.setContentFragment();
	}
	
	@Override
	public boolean onVisibleChanged(boolean visible) { 
		boolean changed = super.onVisibleChanged(visible);
		synchronized (this) { 
			if (mPhoto != null)
				mPhoto.setBitmapVisible(visible);
		}
		return changed;
	}
	
	@Override
	public void bindHeaderView(View view) { 
		if (view == null) return;
		
		DataBinderListener listener = getProvider().getBindListener();
		if (listener != null && listener.onBindHeaderView(this, view))
			return;
	}

	@Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (!location.equals(getPhoto().getPhotoData().getLocation()) && 
				!location.equals(getPhoto().getPhotoData().getAvatarLocation())) {
				return;
			}
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
					onUpdateViewsOnVisible(true);
				}
			});
	}
	
	protected void onUpdateViewsOnVisible(boolean restartSlide) { 
		PhotoBinder binder = getPhotoBinder();
		if (binder != null) binder.onUpdateImages(this, restartSlide);
	}
	
	private PhotoBinder getPhotoBinder() { 
		final PhotoBinder binder = (PhotoBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}
	
	public Drawable getDrawable(final int width, final int height) { 
		return getPhoto().getBitmapImage()
				.getThumbnailDrawable(width, height);
	}
	
	protected boolean isOverlayVisible() { 
		final Image bitmapImage = getPhoto().getBitmapImage();
		if (bitmapImage instanceof HttpImage)
			return HttpImageItem.checkNotDownload((HttpImage)bitmapImage);
		
		return false;
	}
	
	protected boolean onOverlayClick() { 
		if (!isFetching()) { 
			final Image bitmapImage = getPhoto().getBitmapImage();
			if (bitmapImage instanceof HttpImage)
				HttpImageItem.requestDownload((HttpImage)bitmapImage, true);
		}
		return true;
	}
	
	public IMediaPhoto[] getAllPhotos() { 
		PhotoDataSet dataSet = mDataSet;
		PhotoDataSets dataSets = (PhotoDataSets)(dataSet != null ? 
				dataSet.getDataSets() : null);
		
		if (dataSets == null) 
			return new IMediaPhoto[] { getPhoto() };
		
		ArrayList<IMediaPhoto> photos = new ArrayList<IMediaPhoto>();
		
		for (int k=0; k < dataSets.getCount(); k++) {
			PhotoItem item = dataSets.getPhotoItemAt(k);
			if (item == null) continue;
			
			photos.add(item.getPhoto());
		}
		
		return photos.toArray(new IMediaPhoto[photos.size()]);
	}
	
}
