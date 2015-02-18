package org.javenstudio.android.information;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.widget.model.NavigationInfo;

public abstract class Information extends DataBinderItem 
		implements ImageListener, Image.ImageDetails {

	public static final String TITLE_KEY = "title"; 
	public static final String SUMMARY_KEY = "summary"; 
	public static final String AUTHOR_KEY = "author"; 
	public static final String DATE_KEY = "date"; 
	public static final String LINK_KEY = "link"; 
	public static final String CONTENT_KEY = "content"; 
	public static final String IMAGE_KEY = "image"; 
	
	public static final String EXTEND_IMAGE_KEY = "extend-image"; 
	
	public static final String ATTR_LOCATION = "reader.location";
	public static final String ATTR_HTML = "reader.html";
	public static final String ATTR_ICON = "reader.icon";
	public static final String ATTR_ICONRES = "reader.iconRes";
	
	public static final String ATTR_GROUPNAME = "groupName";
	public static final String ATTR_NAME = NavigationInfo.ATTR_NAME;
	public static final String ATTR_TITLE = NavigationInfo.ATTR_TITLE;
	public static final String ATTR_SUBTITLE = NavigationInfo.ATTR_SUBTITLE;
	public static final String ATTR_DROPDOWNTITLE = NavigationInfo.ATTR_DROPDOWNTITLE;
	public static final String ATTR_DEFAULTCHARSET = "defaultCharset";
	
	public static final String ATTR_LOCATIONS = "reader.locations";
	public static final String ATTR_GRIDSHOWLIST = "reader.gridshow.list";
	public static final String ATTR_CONTEXT = "reader.context";
	
	public static final int ITEMTYPE_PHOTO = 1;
	
	public static interface ImageFetchListener { 
		public void onImageFetched(Information item, Image image);
	}
	
	private Map<String, HttpImageItem> mImageMap = null;
	protected List<HttpImageItem> mImages = null;
	private ImageFetchListener mFetchListener = null;
	private int mFetchRequest = 0;
	
	private OnInformationClickListener mListener = null;
	private OnInformationClickListener mLongListener = null;
	private OnInformationClickListener mImageListener = null;
	private OnInformationClickListener mActionListener = null;
	
	protected Information() {} 
	
	private InformationDataSet mDataSet = null;
	
	void setDataSet(InformationDataSet dataSet) { mDataSet = dataSet; }
	InformationDataSet getDataSet() { return mDataSet; }
	
	@Override 
	public Object get(Object key) { 
		throw new UnsupportedOperationException(); 
	}
	
	public boolean isFetching() { return mFetchRequest > 0; }
	
	public void setOnClickListener(OnInformationClickListener l) { mListener = l; }
	public OnInformationClickListener getOnClickListener() { return mListener; }
	
	public void setOnLongClickListener(OnInformationClickListener l) { mLongListener = l; }
	public OnInformationClickListener getOnLongClickListener() { return mLongListener; }
	
	public void setImageClickListener(OnInformationClickListener l) { mImageListener = l; }
	public OnInformationClickListener getImageClickListener() { return mImageListener; }
	
	public void setActionClickListener(OnInformationClickListener l) { mActionListener = l; }
	public OnInformationClickListener getActionClickListener() { return mActionListener; }
	
	public void setImageFetchListener(ImageFetchListener l) { mFetchListener = l; }
	public ImageFetchListener getImageFetchListener() { return mFetchListener; }
	
	@Override
	public void getDetails(IMediaDetails details) {}
	
	public Information[] copyImageOnes() { return null; }
	
	synchronized void clearImageItems() { mImages.clear(); }
	
	synchronized void addImageItem(HttpImageItem img) { 
		if (img == null) return;
		
		if (mImages == null) { 
			mImages = new ArrayList<HttpImageItem>();
			mImageMap = new HashMap<String, HttpImageItem>();
		}
		
		if (mImageMap.containsKey(img.getSrc()))
			return;
		
		mImages.add(img);
		mImageMap.put(img.getSrc(), img);
		
		img.getImage().setImageDetails(this);
		img.getImage().addListener(this);
	}
	
	synchronized HttpImageItem removeImageItemAt(int index) { 
		if (mImages == null) return null;
		HttpImageItem item = index >= 0 && index < mImages.size() ? mImages.remove(index) : null;
		if (mImageMap != null) mImageMap.remove(item.getSrc());
		return item;
	}
	
	public synchronized HttpImageItem getImageItemAt(int index) { 
		if (mImages == null) return null;
		return index >= 0 && index < mImages.size() ? mImages.get(index) : null;
	}
	
	public synchronized int getImageCount() { 
		return mImages != null ? mImages.size() : 0;
	}
	
	//public synchronized Collection<HttpImageItem> getImageItems() { 
	//	return mImages;
	//}
	
	public synchronized HttpImageItem[] getImageItems(int count) { 
		if (count <= 0 || mImages == null) 
			return null;
		
		if (count > mImages.size()) 
			count = mImages.size();
		
		HttpImageItem[] items = new HttpImageItem[count];
		for (int i=0; i < count; i++) { 
			HttpImageItem item = getImageItemAt(i);
			items[i] = item;
		}
		
		return items;
	}
	
	@Override
	public synchronized void onImageEvent(final Image image, final ImageEvent event) { 
		if (image == null || event == null) 
			return;
		
		final String location = image.getLocation();
		synchronized (this) { 
			if (mImageMap == null || !mImageMap.containsKey(location))
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
			
			if (e.getEventType() == HttpEvent.EventType.FETCH_FINISH) { 
				ImageFetchListener listener = mFetchListener;
				if (listener != null) 
					listener.onImageFetched(this, image);
			}
		}
	}
	
	protected void onHttpImageEvent(final Image image, HttpEvent event) {}
	protected void onUpdateViewsOnVisible(boolean restartSlide) {}
	
	public synchronized Image getFirstImage(boolean existBitmap) { 
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
	
	public synchronized HttpImageItem getFirstImageItem(boolean existBitmap) { 
		HttpImageItem first = null;
		for (int i=0; i < getImageCount(); i++) { 
			HttpImageItem textImg = getImageItemAt(i);
			if (textImg != null) { 
				Image image = textImg.getImage();
				if (image != null) {
					if (!existBitmap || image.existBitmap())
						return textImg;
					if (first == null) 
						first = textImg;
				}
			}
		}
		return first;
	}
	
	public synchronized Image[] getAllImageList() { 
		InformationDataSet dataSet = mDataSet;
		InformationDataSets dataSets = (InformationDataSets)(dataSet != null ? 
				dataSet.getDataSets() : null);
		
		if (dataSets == null) 
			return getImageList();
		
		ArrayList<Image> images = new ArrayList<Image>();
		
		for (int k=0; k < dataSets.getCount(); k++) {
			Information info = dataSets.getInformationAt(k);
			if (info == null) continue;
			
			for (int i=0; i < info.getImageCount(); i++) { 
				HttpImageItem textImg = info.getImageItemAt(i);
				if (textImg != null) { 
					Image image = textImg.getImage();
					if (image != null && image.existBitmap())
						images.add(image);
				}
			}
		}
		
		return images.toArray(new Image[images.size()]);
	}
	
	public synchronized Image[] getImageList() { 
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
	
	public static int getItemIconRes(NavigationInfo info) { 
		int iconRes = 0;
		if (info.hasAttribute(Information.ATTR_ICONRES)) { 
			Object attr = info.getAttribute(Information.ATTR_ICONRES);
			if (attr != null && attr instanceof Number) { 
				int val = ((Number)attr).intValue();
				if (val != 0)
					iconRes = val;
			}
		}
		return iconRes;
	}
	
	public static Drawable getItemIcon(NavigationInfo info, int iconRes) { 
		if (info.hasAttribute(Information.ATTR_ICON)) { 
			Object attr = info.getAttribute(Information.ATTR_ICON);
			if (attr != null && attr instanceof Drawable) 
				return (Drawable)attr;
		}
		
		if (iconRes > 0 && info.hasAttribute(Information.ATTR_CONTEXT)) { 
			Object attr = info.getAttribute(Information.ATTR_CONTEXT);
			if (attr != null && attr instanceof Context)
				return ((Context)attr).getResources().getDrawable(iconRes);
		}
		
		return null;
	}
	
	public static Context getContext(NavigationInfo info) { 
		if (info.hasAttribute(Information.ATTR_CONTEXT)) { 
			Object attr = info.getAttribute(Information.ATTR_CONTEXT);
			if (attr != null && attr instanceof Context)
				return ((Context)attr);
		}
		
		return ResourceHelper.getContext();
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + getIdentity() 
				+ ",imageCount=" + getImageCount() + "}";
	}
	
}
