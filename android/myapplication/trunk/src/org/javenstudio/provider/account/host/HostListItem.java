package org.javenstudio.provider.account.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.android.app.SlideDrawable;
import org.javenstudio.android.data.DataBinder;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.util.Logger;

public abstract class HostListItem extends DataBinderItem 
		implements ImageListener {
	private static final Logger LOG = Logger.getLogger(HostListItem.class);

	public static interface ImageItem {
		public HttpImageItem getHttpImageItem();
	}
	
	public static interface ImageFetchListener { 
		public void onImageFetched(HostListItem item, Image image);
	}
	
	private final Object mImageLock = new Object();
	private Map<String, ImageItem> mImageMap = null;
	private List<ImageItem> mImageList = null;
	private ImageFetchListener mFetchListener = null;
	private int mFetchRequest = 0;
	
	public abstract int getViewRes();
	public abstract void bindView(Activity activity, View view);
	public abstract void updateView(View view, boolean restartSlide);
	
	public boolean isEnabled() { return false; }
	public boolean isFetching() { return mFetchRequest > 0; }
	
	public void setImageFetchListener(ImageFetchListener l) { mFetchListener = l; }
	public ImageFetchListener getImageFetchListener() { return mFetchListener; }
	
	public void requestDownload(Activity activity) {
		DataBinder.requestDownload(activity, getShowImageItems(), true);
	}
	
	public void addImageItem(String imageURL, int imageWidth, int imageHeight) {
		if (imageURL != null && imageURL.length() > 0) {
			final HttpImageItem imageItem = new HttpImageItem(imageURL, null, null, null, 
					imageWidth, imageHeight);
			
			addImageItem(new ImageItem() {
					@Override
					public HttpImageItem getHttpImageItem() {
						return imageItem;
					}
					@Override
					public String toString() {
						return "ImageItem{item=" + imageItem + "}";
					}
				});
		}
	}
	
	public void clearImageItems() { 
		synchronized (mImageLock) {
			mImageList.clear(); 
		}
	}
	
	public boolean addImageItem(ImageItem img) { 
		if (img == null) return false;
		
		synchronized (mImageLock) {
			if (mImageList == null) { 
				mImageList = new ArrayList<ImageItem>();
				mImageMap = new HashMap<String, ImageItem>();
			}
			
			if (mImageMap.containsKey(img.getHttpImageItem().getSrc()))
				return false;
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("addImageItem: item=" + img + " item=" + this 
						+ " count=" + mImageList.size());
			}
			
			mImageList.add(img);
			mImageMap.put(img.getHttpImageItem().getSrc(), img);
			
			//img.getHttpImageItem().getImage().setImageDetails(this);
			img.getHttpImageItem().getImage().addListener(this);
			
			return true;
		}
	}
	
	public ImageItem removeImageItemAt(int index) { 
		synchronized (mImageLock) {
			if (mImageList == null) return null;
			
			ImageItem item = index >= 0 && index < mImageList.size() ? 
					mImageList.remove(index) : null;
					
			if (mImageMap != null) 
				mImageMap.remove(item.getHttpImageItem().getSrc());
			
			return item;
		}
	}
	
	public HttpImageItem getImageItemAt(int index) { 
		synchronized (mImageLock) {
			if (mImageList == null) return null;
			return index >= 0 && index < mImageList.size() ? 
					mImageList.get(index).getHttpImageItem() : null;
		}
	}
	
	public int getImageCount() { 
		synchronized (mImageLock) {
			return mImageList != null ? mImageList.size() : 0;
		}
	}
	
	public HttpImageItem[] getImageItems(int count) { 
		synchronized (mImageLock) {
			if (count <= 0 || mImageList == null) 
				return null;
			
			if (count > mImageList.size()) 
				count = mImageList.size();
			
			HttpImageItem[] items = new HttpImageItem[count];
			for (int i=0; i < count; i++) { 
				HttpImageItem item = getImageItemAt(i);
				items[i] = item;
			}
			
			return items;
		}
	}
	
	public Image[] getImageList() { 
		ArrayList<Image> images = new ArrayList<Image>();
		
		for (int i=0; i < getImageCount(); i++) { 
			HttpImageItem textImg = getImageItemAt(i);
			if (textImg != null) { 
				Image image = textImg.getImage();
				if (image != null && image.existBitmap())
					images.add(image);
			}
		}
		
		return images.toArray(new Image[images.size()]);
	}
	
	public Image getFirstImage(boolean existBitmap) { 
		Image first = null;
		for (int i=0; i < getImageCount(); i++) { 
			HttpImageItem textImg = getImageItemAt(i);
			if (textImg != null) { 
				Image image = textImg.getImage();
				if (image != null) {
					if (!existBitmap || image.existBitmap())
						return image;
					if (first == null) 
						first = image;
				}
			}
		}
		return first;
	}
	
	@Override
	public void onImageEvent(final Image image, ImageEvent event) { 
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (mImageLock) { 
			if (mImageMap == null || !mImageMap.containsKey(location))
				return;
		}
		
		if (event instanceof HttpEvent) { 
			HttpEvent e = (HttpEvent)event;
			synchronized (mImageLock) {
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
			}
			
			onHttpImageEvent(image, e);
			
			if (e.getEventType() == HttpEvent.EventType.FETCH_FINISH) { 
				ImageFetchListener listener = mFetchListener;
				if (listener != null) 
					listener.onImageFetched(this, image);
			}
		}
	}
	
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		if (LOG.isDebugEnabled()) {
			LOG.debug("onHttpImageEvent: image=" + image + " event=" + event 
					+ " item=" + this);
		}
		
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					setCachedImageDrawable(null);
					onUpdateViewOnVisible(true);
				}
			});
	}
	
	public void onUpdateViewOnVisible(boolean restartSlide) {
		final View view = getBindView();
		if (view == null || view.getTag() != this)
			return;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("onUpdateViewOnVisible: item=" + this + " view=" + view 
					+ " restartSlide=" + restartSlide);
		}
		
		updateView(view, restartSlide);
	}
	
	@Override
	public boolean onVisibleChanged(boolean visible) { 
		boolean changed = super.onVisibleChanged(visible);
		if (changed && LOG.isDebugEnabled()) {
			LOG.debug("onVisibleChanged: visible=" + visible + " changed=" + changed 
					+ " item=" + this);
		}
		
		synchronized (this) { 
			if (mImageList != null) {
				for (ImageItem image : mImageList) { 
					if (image != null) 
						image.getHttpImageItem().getImage().setBitmapVisible(visible);
				}
			}
		}
		return changed;
	}
	
	protected int getShowImageCount() { 
		return 1; //getImageCount() <= 1 ? 1 : 2;
	}
	
	public HttpImageItem[] getShowImageItems() { 
		return getImageItems(getShowImageCount());
	}
	
	public Drawable getItemDrawable() {
		return getItemDrawable(getImageViewWidth(), getImageViewHeight());
	}
	
	public Drawable getItemDrawable(final int width, final int height) {
		Drawable d = getCachedImageDrawable();
		if (d != null) return d;
		
		if (getImageCount() <= 0) return null;
		
		final int showCount = getShowImageCount();
		if (showCount <= 1) {
			Image image = getFirstImage(false); 
			if (image != null) { 
				return image.getThumbnailDrawable(width, height);
			}
			
			return null;
		}
		
		SlideDrawable fd = new SlideDrawable(new SlideDrawable.Callback() {
				private int mIndex = 0;
				private int mIndexMax = showCount;
				@Override
				public Drawable next() {
					if (mIndex >= 0 && mIndex < mIndexMax && mIndex < getImageCount()) { 
						HttpImageItem image = getImageItemAt(mIndex++); 
						if (image == null) return next();
						return image.getImage().getThumbnailDrawable(width, height);
					}
					return null;
				}
			});
		
		return fd;
	}
	
}
