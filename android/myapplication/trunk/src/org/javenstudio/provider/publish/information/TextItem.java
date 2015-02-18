package org.javenstudio.provider.publish.information;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.DataBinderItem;
import org.javenstudio.android.data.DataBinderListener;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.ImageEvent;
import org.javenstudio.android.data.image.ImageListener;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.information.InformationHelper;
import org.javenstudio.android.information.InformationOperation;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.common.util.Logger;

public abstract class TextItem extends DataBinderItem 
		implements ImageListener, Image.ImageDetails, InformationOperation.IInformation {
	private static final Logger LOG = Logger.getLogger(TextItem.class);
	
	public static final String TITLE_KEY = "title"; 
	public static final String SUMMARY_KEY = "summary"; 
	public static final String AUTHOR_KEY = "author"; 
	public static final String DATE_KEY = "date"; 
	public static final String LINK_KEY = "link"; 
	public static final String CONTENT_KEY = "content"; 
	
	public static interface ImageFetchListener { 
		public void onImageFetched(TextItem item, Image image);
	}
	
	private TextDataSet mDataSet = null;
	
	void setDataSet(TextDataSet dataSet) { mDataSet = dataSet; }
	public TextDataSet getDataSet() { return mDataSet; }
	
	private final Map<String, List<Object>> mFields = 
			new HashMap<String, List<Object>>();
	
	private Map<String, HttpImageItem> mImageMap = null;
	private List<HttpImageItem> mImages = null;
	private ImageFetchListener mFetchListener = null;
	private int mFetchRequest = 0;
	
	public abstract TextProvider getProvider();
	public boolean isFetching() { return mFetchRequest > 0; }
	
	public void setImageFetchListener(ImageFetchListener l) { mFetchListener = l; }
	public ImageFetchListener getImageFetchListener() { return mFetchListener; }
	
	@Override 
	public final Object get(Object key) { 
		if (TITLE_KEY.equals(key)) { 
			return getTitle(); 
		} else if (SUMMARY_KEY.equals(key)) { 
			return getSummary(); 
		} else if (AUTHOR_KEY.equals(key)) { 
			return getAuthor(); 
		} else if (DATE_KEY.equals(key)) { 
			return getDate(); 
		} else if (LINK_KEY.equals(key)) { 
			return getLink(); 
		} else if (CONTENT_KEY.equals(key)) { 
			return getContent(); 
		} else if (key != null && key instanceof String) {
			return getField((String)key); 
		}
		return null;
	}
	
	public synchronized Object getField(String key) { 
		List<Object> values = mFields.get(key);
		if (values != null && values.size() > 0) 
			return values.get(0);
		
		return null;
	}
	
	public synchronized void setField(String key, Object value) { 
		mFields.remove(key);
		addFieldValue(key, value);
	}
	
	public synchronized void addFieldValue(String key, Object value) { 
		if (key == null || value == null) 
			return;
		
		List<Object> values = mFields.get(key);
		if (values == null) { 
			values = new ArrayList<Object>();
			mFields.put(key, values);
		}
		
		values.add(value);
	}
	
	public synchronized Object[] getFieldValues(String key) { 
		List<Object> values = mFields.get(key);
		if (values != null) 
			return values.toArray(new Object[0]);
		
		return null;
	}
	
	public synchronized <T> T[] getFieldValuesAsType(String key, Class<T> clazz) { 
		List<Object> values = mFields.get(key);
		if (values != null) {
			@SuppressWarnings("unchecked") 
			T[] newArray = (T[]) Array.newInstance(clazz, values.size());
			return values.toArray(newArray);
		}
		
		return null;
	}
	
	public String getTitle() { return (String)getField(TITLE_KEY); } 
	public void setTitle(String s) { setField(TITLE_KEY, s); } 
	
	public String getAuthor() { return (String)getField(AUTHOR_KEY); } 
	public void setAuthor(String s) { setField(AUTHOR_KEY, s); } 
	
	public String getDate() { return (String)getField(DATE_KEY); } 
	public void setDate(String s) { setField(DATE_KEY, s); } 
	
	public String getLink() { return (String)getField(LINK_KEY); } 
	public void setLink(String s) { setField(LINK_KEY, s); } 
	
	public String getSummary() { return (String)getField(SUMMARY_KEY); } 
	
	public String[] getSummarys() { 
		return getFieldValuesAsType(SUMMARY_KEY, String.class); 
	}
	
	public void setSummary(String s) { 
		setField(SUMMARY_KEY, s); parseImg(s); 
	}
	
	public void addSummary(String s) { 
		addFieldValue(SUMMARY_KEY, s); parseImg(s); 
	}
	
	public String getContent() { return (String)getField(CONTENT_KEY); } 
	
	public String[] getContents() { 
		return getFieldValuesAsType(CONTENT_KEY, String.class); 
	}
	
	public void setContent(String s) { 
		setField(CONTENT_KEY, s); parseImg(s); 
	}
	
	public void addContent(String s) { 
		addFieldValue(CONTENT_KEY, s); parseImg(s); 
	}
	
	@Override
	public void getDetails(IMediaDetails details) { 
		details.add(R.string.details_title, InformationHelper.formatTitleSpanned(getTitle()));
		details.add(R.string.details_date, getDate());
		
		String text = getSummary();
		if (text == null || text.length() == 0) 
			text = getContent();
		
		details.add(R.string.details_description, 
				InformationHelper.formatContentSpanned(text));
	}
	
	@Override
	public String getShareText() { 
		String title = getTitle();
		String author = getAuthor();
		String date = getDate();
		String link = getLink();
		
		String text = getSummary();
		if (text == null || text.length() == 0) 
			text = getContent();
		
		//if (link == null || link.length() == 0) 
		//	link = getLocation();
		
		AppResources.Fields fields = new AppResources.Fields();
		fields.addField("title", title);
		fields.addField("content", text);
		fields.addField("author", author);
		fields.addField("date", date);
		fields.addField("link", link);
		fields.addField("source", null);
		
		return AppResources.getInstance().getShareInformation(fields);
	}
	
	@Override
	public String getShareType() { 
		return MediaUtils.MIME_TYPE_ALL;
	}
	
	public Uri getShareStreamUri() { 
		Image image = getFirstImage(true);
		if (image != null) return image.getContentUri();
		return null;
	}
	
	public Intent getShareIntent() { 
		//if (LOG.isDebugEnabled())
		//	LOG.debug("getShareIntent: item=" + this);
		
		String shareType = getShareType();
		String shareText = getShareText();
		Uri shareStream = null;
		
		Image image = getFirstImage(true);
		if (image != null) { 
			shareStream = image.getContentUri();
			shareType = image.getShareType();
		}
    	
		if ((shareText == null || shareText.length() == 0) && shareStream == null) 
			return null;
		
		Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(shareType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		
		if (shareText != null)
			intent.putExtra(Intent.EXTRA_TEXT, shareText);
		
		if (shareStream != null)
			intent.putExtra(Intent.EXTRA_STREAM, shareStream);
		
		if (LOG.isDebugEnabled())
			LOG.debug("getShareIntent: intent=" + intent);
		
		return intent;
	}
	
	protected void parseImg(String s) {}
	
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
	public synchronized void onImageEvent(final Image image, ImageEvent event) { 
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
		final TextBinder binder = (TextBinder)getProvider().getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return;
		
		binder.onUpdateImages(this, restartSlide);
	}
	
	@Override
	public boolean onVisibleChanged(boolean visible) { 
		boolean changed = super.onVisibleChanged(visible);
		synchronized (this) { 
			if (mImages != null) {
				for (HttpImageItem image : mImages) { 
					if (image != null) 
						image.getImage().setBitmapVisible(visible);
				}
			}
		}
		return changed;
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
	
	@Override
	public void bindHeaderView(View view) { 
		if (view == null) return;
		
		DataBinderListener listener = getProvider().getBindListener();
		if (listener != null && listener.onBindHeaderView(this, view))
			return;
	}
	
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
	
	public synchronized Image[] getAllImages() { 
		TextDataSet dataSet = mDataSet;
		TextDataSets dataSets = (TextDataSets)(dataSet != null ? 
				dataSet.getDataSets() : null);
		
		if (dataSets == null) 
			return getImageList();
		
		ArrayList<Image> images = new ArrayList<Image>();
		
		for (int k=0; k < dataSets.getCount(); k++) {
			TextItem item = dataSets.getTextItemAt(k);
			if (item == null) continue;
			
			for (int i=0; i < item.getImageCount(); i++) { 
				HttpImageItem textImg = item.getImageItemAt(i);
				if (textImg != null) { 
					Image image = textImg.getImage();
					if (image != null && image.existBitmap())
						images.add(image);
				}
			}
		}
		
		return images.toArray(new Image[images.size()]);
	}
	
}
