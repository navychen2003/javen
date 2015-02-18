package org.javenstudio.android.information;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import org.javenstudio.android.SimpleHtmlImgParser;
import org.javenstudio.android.app.AppResources;
import org.javenstudio.android.app.R;
import org.javenstudio.android.data.image.Image;
import org.javenstudio.android.data.image.http.HttpEvent;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.android.reader.ReaderHelper;
import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.cocoka.data.IMediaDetails;
import org.javenstudio.cocoka.util.MediaUtils;
import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.common.util.Logger;

public class InformationOne extends Information 
		implements InformationOperation.IInformation {
	static final Logger LOG = Logger.getLogger(InformationOne.class);

	private static final String CONTENURI_PREFIX = "content://information/item/";
	
	public static interface Item { 
		public InformationBinder getBinder();
		public Object getAttribute(String key);
		public void setAttribute(String key, Object val);
	}
	
	private final Map<String, List<Object>> mFields = 
			new HashMap<String, List<Object>>();
	
	private final String mLocation;
	private final String mContentUri;
	private final Item mItem;
	
	private int mSummaryLength = 0;
	private int mContentLength = 0;
	
	public InformationOne(Item item, String location) { 
		this(item, location, newContentUri(location));
	}
	
	public InformationOne(Item item, String location, String contentUri) { 
		if (item == null || location == null) throw new NullPointerException();
		mLocation = location;
		mContentUri = contentUri;
		mItem = item;
	}
	
	public Object getAttribute(String key) { 
		return mItem.getAttribute(key);
	}
	
	public void setAttribute(String key, Object val) { 
		mItem.setAttribute(key, val);
	}
	
	private static String newContentUri(String location) { 
		return CONTENURI_PREFIX + Utilities.toMD5(location);
	}
	
	private String copyLocation() { 
		return newContentUri(getLocation() + "#" + getIdentity()) 
				+ "/" + System.currentTimeMillis() + "/" + getLocation();
	}
	
	public final InformationOne copyOne() { 
		String location = copyLocation();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("copyOne: id=" + getIdentity() + " location=" 
					+ getLocation() + " copyLocation=" + location);
		}
		
		InformationOne one = new InformationOne(mItem, location);
		one.mFields.putAll(this.mFields);
		one.mSummaryLength = this.mSummaryLength;
		one.mContentLength = this.mContentLength;
		
		for (int i=0; i < getImageCount(); i++) { 
			HttpImageItem item = getImageItemAt(i);
			one.addImageItem(item);
		}
		
		return one;
	}
	
	public final InformationOne[] copyOnes(boolean includeExtend) { 
		ArrayList<InformationOne> list = new ArrayList<InformationOne>();
		
		String location = copyLocation();
		if (location != null) { 
			InformationOne one = new InformationOne(mItem, location);
			one.mFields.putAll(this.mFields);
			one.mSummaryLength = this.mSummaryLength;
			one.mContentLength = this.mContentLength;
			
			HttpImageItem item = getImageItemAt(0);
			one.addImageItem(item);
			list.add(one);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("copyOnes: id=" + getIdentity() + " location=" 
						+ getLocation() + " copyLocation=" + one.getLocation() 
						+ " copyImg=" + (item != null ? item.getImage() : ""));
			}
		}
		
		int count = 0;
		
		for (int i=1; i < getImageCount(); i++) { 
			HttpImageItem item = getImageItemAt(i);
			if (item == null) continue;
			count ++;
			
			InformationOne one = new InformationOne(mItem, location + "#" + count);
			one.setTitle(item.getTitle());
			one.setSummary(item.getAlt());
			one.addImageItem(item);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("copyOnes: id=" + getIdentity() + " location=" 
						+ getLocation() + " copyLocation=" + one.getLocation() 
						+ " copyImg=" + item.getImage());
			}
			
			list.add(one);
		}
		
		Object extendImage = includeExtend ? getField(EXTEND_IMAGE_KEY) : null;
		if (extendImage != null) { 
			InformationOne extendOne = new InformationOne(mItem, location);
			extendOne.setImage(extendImage.toString());
			
			for (int i=0; i < extendOne.getImageCount(); i++) { 
				HttpImageItem item = extendOne.getImageItemAt(i);
				if (item == null) continue;
				count ++;
				
				InformationOne one = new InformationOne(mItem, location + "#" + count);
				one.setTitle(item.getTitle());
				one.setSummary(item.getAlt());
				one.addImageItem(item);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("copyOnes: id=" + getIdentity() + " location=" 
							+ getLocation() + " copyLocation=" + one.getLocation() 
							+ " copyImg=" + item.getImage());
				}
				
				list.add(one);
			}
		}
		
		return list.toArray(new InformationOne[list.size()]);
	}
	
	@Override
	public synchronized InformationOne[] copyImageOnes() { 
		if (getImageCount() <= 1) return null;
		
		ArrayList<InformationOne> list = new ArrayList<InformationOne>();
		
		String location = copyLocation();
		int count = 0;
		
		while (getImageCount() > 1) { 
			HttpImageItem item = removeImageItemAt(1);
			if (item == null) break;
			count ++;
			
			InformationOne one = new InformationOne(mItem, location + "#" + count);
			one.setTitle(item.getTitle());
			one.setSummary(item.getAlt());
			one.addImageItem(item);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("copyImageOnes: id=" + getIdentity() + " location=" 
						+ getLocation() + " copyLocation=" + one.getLocation() 
						+ " copyImg=" + item.getImage());
			}
			
			list.add(one);
		}
		
		return list.toArray(new InformationOne[list.size()]);
	}
	
	public final String getLocation() { return mLocation; }
	public final int getSummaryLength() { return mSummaryLength; }
	public final int getContentLength() { return mContentLength; }
	
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
		setField(SUMMARY_KEY, s); 
		String text = parseImg(s); 
		mSummaryLength = text != null ? text.length() : 0;
	}
	
	public void addSummary(String s) { 
		addFieldValue(SUMMARY_KEY, s); 
		String text = parseImg(s); 
		mSummaryLength += text != null ? text.length() : 0;
	}
	
	public String getContent() { return (String)getField(CONTENT_KEY); } 
	public String getContentUri() { return mContentUri; }
	
	public String[] getContents() { 
		return getFieldValuesAsType(CONTENT_KEY, String.class); 
	}
	
	public void setContent(String s) { 
		setField(CONTENT_KEY, s); 
		String text = parseImg(s); 
		mContentLength = text != null ? text.length() : 0;
	}
	
	public void addContent(String s) { 
		addFieldValue(CONTENT_KEY, s); 
		String text = parseImg(s); 
		mContentLength += text != null ? text.length() : 0;
	}
	
	public String getImage() { return (String)getField(IMAGE_KEY); } 
	
	public void setImage(String s) { 
		setField(IMAGE_KEY, s); 
		parseImg(s); 
	}
	
	public void addImage(String s) { 
		addFieldValue(IMAGE_KEY, s); 
		parseImg(s); 
	}
	
	@Override
	public void getDetails(IMediaDetails details) { 
		details.add(R.string.details_title, InformationHelper.formatTitleSpanned(getTitle()));
		details.add(R.string.details_author, getAuthor());
		details.add(R.string.details_date, getDate());
		
		String text = getSummary();
		if (text == null || text.length() == 0 || getSummaryLength() <= 0) 
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
		if (text == null || text.length() == 0 || getSummaryLength() <= 0) 
			text = getContent();
		
		if (link == null || link.length() == 0) 
			link = getLocation();
		
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
		//	LOG.debug("getShareIntent: location=" + getLocation());
		
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
	
	protected String parseImg(String s) { 
		if (s == null || s.length() == 0) 
			return null;
		
		SimpleHtmlImgParser parser = new SimpleHtmlImgParser() { 
				@Override
				protected void handleImg(String src, String alt, String title, 
						String original, String dataoriginal, int width, int height) { 
					String newSrc = SimpleHtmlImgParser.normalizeHref(src, getLocation());
					String newOriginal = SimpleHtmlImgParser.normalizeHref(original, getLocation());
					String newDataOriginal = SimpleHtmlImgParser.normalizeHref(dataoriginal, getLocation());
					
					newSrc = ReaderHelper.normalizeInformationLocation(newSrc);
					newOriginal = ReaderHelper.normalizeInformationLocation(newOriginal);
					newDataOriginal = ReaderHelper.normalizeInformationLocation(newDataOriginal);
					
					if (newSrc != null && HttpImageItem.isIgnoreImage(newSrc)) 
						newSrc = null;
					
					if (newOriginal != null && HttpImageItem.isIgnoreImage(newOriginal))
						newOriginal = null;
					
					if (newDataOriginal != null && HttpImageItem.isIgnoreImage(newDataOriginal))
						newDataOriginal = null;
					
					String source = newDataOriginal;
					if (source == null || source.length() == 0) 
						source = newOriginal;
					if (source == null || source.length() == 0) 
						source = newSrc;
					
					boolean added = false;
					if (source != null && source.length() > 0) {
						HttpImageItem item = new HttpImageItem(source, alt, title, original, width, height);
						addImageItem(item);
						added = true;
					}
					
					if (LOG.isDebugEnabled() && src != null && src.length() > 0) 
						LOG.debug("handleImg: source=" + source + " src=" + src + " added=" + added);
				}
			};
		
		return ParseUtils.trim(parser.parse(s, true));
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
	
	@Override
	protected void onHttpImageEvent(final Image image, HttpEvent event) { 
		ResourceHelper.getHandler().post(new Runnable() {
				@Override
				public void run() { 
					setCachedImageDrawable(null);
					onUpdateViewsOnVisible(true);
				}
			});
	}
	
	@Override
	public void onUpdateViewsOnVisible(boolean restartSlide) { 
		final InformationBinder binder = mItem.getBinder();
		final View view = getBindView();
		
		if (binder == null || view == null) 
			return;
		
		binder.onUpdateInformationImages(this, restartSlide);
	}
	
	@Override
	public void bindHeaderView(View view) { 
		if (view == null) return;
		
		//int res = getItemDrawableRes(IMAGE_BACKGROUND);
		//if (res != 0) view.setBackgroundResource(res);
	}
	
	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{id=" + getIdentity() 
				+ ",location=" + getLocation() + ",imageCount=" + getImageCount() + "}";
	}
	
}
