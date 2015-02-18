package org.javenstudio.provider.media.album;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.data.media.PhotoSet;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;
import org.javenstudio.provider.Provider;
import org.javenstudio.provider.media.IMediaAlbum;

public class AlbumItem extends DataBinderItem implements ImageListener {
	private static final Logger LOG = Logger.getLogger(AlbumItem.class);
	
	private final AlbumSource mSource;
	private final IMediaAlbum mAlbum;
	
	private AlbumDrawable.DrawableList mDrawables = null;
	private Image[] mSlideImages = null;
	
	private float mSlidePhotoSpace = 0.0f;
	private int mSlideIndex = 0;
	private int mFetchRequest = 0;
	
	public AlbumItem(AlbumSource source, IMediaAlbum album) { 
		if (source == null || album == null) throw new NullPointerException();
		mSource = source;
		mAlbum = album;
	}
	
	public final AlbumSource getSource() { return mSource; }
	public final IMediaAlbum getAlbum() { return mAlbum; }
	
	public Provider getProvider() { return mSource.getProvider(); }
	public AlbumDataSets getDataSets() { return mSource.getAlbumDataSets(); }
	
	public boolean isFetching() { return mFetchRequest > 0; }
	public boolean isActionModeEnabled() { return false; } //getProvider().isActionModeEnabled(); }
	
	protected void onUpdateViewsOnVisible() {}
	
	public boolean isSelected(IActivity activity) { 
		if (activity == null) return false;
		
		PhotoSet data = getAlbum().getPhotoSet();
		SelectManager manager = getSource().getSelectManager(); 
		
		if (manager != null) 
			return manager.isSelectedItem(data);
		
		return false;
	}
	
	public void setSelected(IActivity activity, boolean selected) { 
		if (activity == null) return;
		
		PhotoSet data = getAlbum().getPhotoSet();
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
		AlbumDataSets dataSets = getDataSets();
		
		if (context == null || dataSets == null) 
			return;
		
		boolean binded = true;
		int rebindCount = 0;
		
		for (int i=0; i < dataSets.getCount(); i++) { 
			AlbumDataSet dataSet = dataSets.getAlbumDataSet(i);
			if (dataSet == null) continue;
			
			AlbumItem item = dataSet.getAlbumItem();
			if (item == null) continue;
			
			AlbumBinder binder = (AlbumBinder)getProvider().getBinder();
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
	
	private synchronized AlbumDrawable.DrawableList getDrawableList() { 
		getSlideImages();
		
		if (mDrawables == null) { 
			AlbumDrawable.DrawableList drawables = new AlbumDrawable.DrawableList() {
				private final Map<String, Drawable> mMap = new HashMap<String, Drawable>();
				
				@Override
				public int getCount() {
					Image[] images = mSlideImages;
					return images != null ? images.length : 0;
				}

				@Override
				public Drawable getDrawableAt(int index, int width, int height) {
					if (index < 0 || index >= getCount()) 
						return null;
					
					String key = "" + index + "-" + width + "-" + height;
					Drawable d = mMap.get(key);
					if (d == null) {
						Image[] images = mSlideImages;
						if (images != null && index >= 0 && index < images.length) { 
							Image image = images[index];
							if (image != null && width > 0 && height > 0) 
								d = image.getThumbnailDrawable(width, height);
						}
						
						if (d == null) {
							//int res = getItemDrawableRes(IMAGE_BACKGROUND);
							//if (res != 0) 
							//	d = ResourceHelper.getResources().getDrawable(res);
						}
						
						if (d != null) 
							mMap.put(key, d);
					}
					
					return d;
				}

				@Override
				public boolean contains(Drawable d) {
					return d != null ? mMap.values().contains(d) : false;
				}
			};
			
			mDrawables = drawables;
		}
		
		return mDrawables;
	}
	
	public synchronized Image[] getSlideImages() { 
		if (mSlideImages == null) { 
			ArrayList<Image> list = new ArrayList<Image>(); 
			
			Image[] images = getAlbum().getAlbumImages(4);
			for (int i=0; images != null && i < images.length; i++) { 
				Image image = images[i];
				if (image == null) continue;
				
				boolean found = false;
				for (Image img : list) { 
					if (img == image) { 
						found = true; break;
					}
				}
				
				if (!found) { list.add(image); image.addListener(this); }
				if (list.size() >= 4) break;
			}
			
			mSlideImages = list.toArray(new Image[list.size()]);
			mDrawables = null;
			mSlideIndex = 0;
		}
		
		return mSlideImages;
	}
	
	public synchronized Image getCurrentSlideImage() { 
		Image[] images = getSlideImages();
		int index = mSlideIndex;
		
		if (index >= 0 && images != null && index < images.length)
			return images[index];
		
		return null;
	}
	
	@Override
	public boolean onVisibleChanged(boolean visible) { 
		boolean changed = super.onVisibleChanged(visible);
		synchronized (this) { 
			if (mAlbum != null)
				mAlbum.setBitmapVisible(visible);
			
			Image[] images = mSlideImages;
			for (int i=0; images != null && i < images.length; i++) { 
				Image image = images[i];
				if (image != null) 
					image.setBitmapVisible(visible);
			}
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
	
	public void setPhotoSpace(float space) { mSlidePhotoSpace = space; }
	public float getPhotoSpace() { return mSlidePhotoSpace; }
	
	public Drawable getAlbumDrawable(final int width, final int height) { 
		float space = getPhotoSpace();
		AlbumDrawable fd = new AlbumDrawable(getDrawableList(), width, height, space);
		return fd;
	}
	
	@Override
	public void onImageEvent(Image image, ImageEvent event) {
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			Image[] images = mSlideImages;
			boolean found = false;
			
			for (int i=0; images != null && i < images.length; i++) { 
				Image img = images[i];
				if (img != null && location.equals(img.getLocation())) { 
					found = true; break;
				}
			}
			
			if (!found) return;
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
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					setCachedImageDrawable(null);
					onUpdateViewsOnVisible(true);
				}
			});
	}
	
	protected void onUpdateViewsOnVisible(boolean restartSlide) { 
		AlbumBinder binder = getAlbumBinder();
		if (binder != null) binder.onUpdateImages(this, restartSlide);
	}
	
	private AlbumBinder getAlbumBinder() { 
		final AlbumBinder binder = (AlbumBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return null;
		
		return binder;
	}
	
	protected boolean isOverlayVisible() { 
		final Image[] bitmapImages = mSlideImages;
		if (bitmapImages != null)
			return HttpImageItem.checkNotDownload(bitmapImages);
		
		return false;
	}
	
	protected boolean onOverlayClick() { return requestDownload(); }
	
	protected boolean requestDownload() { 
		if (!isFetching()) { 
			final Image[] bitmapImages = mSlideImages;
			if (bitmapImages != null)
				HttpImageItem.requestDownload(bitmapImages, true);
		}
		return true;
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + getIdentity() 
				+ ",source=" + getSource() + ",photoSet=" + getAlbum().getPhotoSet() 
				+ "}";
	}
	
}
