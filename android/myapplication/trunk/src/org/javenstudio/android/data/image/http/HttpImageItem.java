package org.javenstudio.android.data.image.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.android.data.image.Image;
import org.javenstudio.common.util.Logger;

public class HttpImageItem {
	static final Logger LOG = Logger.getLogger(HttpImageItem.class);

	private static final Map<String, Pattern> sIgnorePatterns = 
			new HashMap<String, Pattern>();
	
	public static void addIgnorePattern(String pattern) { 
		if (pattern == null || pattern.length() == 0) 
			return;
		
		synchronized (sIgnorePatterns) { 
			try {
				sIgnorePatterns.put(pattern, Pattern.compile(pattern));
			} catch (Throwable ex) { 
				if (LOG.isErrorEnabled())
					LOG.error("compile pattern: " + pattern + " error: " + ex.toString(), ex);
			}
		}
	}
	
	public static boolean isIgnoreImage(String location) { 
		if (location == null || location.length() == 0) 
			return true;
		
		synchronized (sIgnorePatterns) { 
			for (Pattern p : sIgnorePatterns.values()) { 
				Matcher m = p.matcher(location);
				if (m.find()) 
					return true;
			}
		}
		
		return false;
	}
	
	private final String mSrc;
	private final Image mImage;
	private String mOriginal;
	private String mAlt, mTitle;
	private int mWidth, mHeight;
	
	public HttpImageItem(String src, String alt, String title, 
			String original, int width, int height) { 
		if (src == null || src.length() == 0) 
			throw new IllegalArgumentException("Image source cannot be empty!");

		mSrc = src;
		mOriginal = original;
		mAlt = alt;
		mTitle = title;
		mWidth = width;
		mHeight = height;
		
		mImage = HttpResource.getInstance().getImage(src);
	}
	
	public final String getSrc() { return mSrc; }
	public final Image getImage() { return mImage; }
	
	public final String getOriginal() { return mOriginal; }
	public void setOriginal(String s) { mOriginal = s; }
	
	public final String getAlt() { return mAlt; }
	public void setAlt(String s) { mAlt = s; }
	
	public final String getTitle() { return mTitle; }
	public void setTitle(String s) { mTitle = s; }
	
	public final int getWidth() { return mWidth; }
	public void setWidth(int size) { mWidth = size; }
	
	public final int getHeight() { return mHeight; }
	public void setHeight(int size) { mHeight = size; }
	
	@Override
	public boolean equals(Object o) { 
		if (o == this) return true;
		if (o == null || !(o instanceof HttpImageItem)) 
			return false;
		
		HttpImageItem other = (HttpImageItem)o;
		return mSrc.equals(other.mSrc);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{src=" + getSrc() 
				+ ",width=" + getWidth() + ",height=" + getHeight() + "}";
	}
	
	public static void requestDownload(Collection<HttpImageItem> images, boolean force) { 
		if (images == null) return;
		
		for (HttpImageItem image : images) {
			Image img = image != null ? image.getImage() : null;
			if (img != null && img instanceof HttpImage) 
				requestDownload((HttpImage)img, force);
		}
	}
	
	public static void requestDownload(HttpImageItem[] images, boolean force) { 
		if (images == null) return;
		
		for (HttpImageItem image : images) {
			Image img = image != null ? image.getImage() : null;
			if (img != null && img instanceof HttpImage) 
				requestDownload((HttpImage)img, force);
		}
	}
	
	public static void requestDownload(HttpImageItem image, boolean force) { 
		if (image == null) return;
		
		Image img = image != null ? image.getImage() : null;
		if (img != null && img instanceof HttpImage) 
			requestDownload((HttpImage)img, force);
	}
	
	public static void requestDownload(HttpImage image, boolean force) { 
		if (image != null) { 
			if (LOG.isDebugEnabled()) 
				LOG.debug("requestDownload: image=" + image + " force=" + force);
			
			if (image.getDownloadStatus() != HttpImage.DownloadStatus.DOWNLOADED || force) {
				image.checkDownload(force ? HttpImage.RequestType.FORCE : 
					HttpImage.RequestType.DOWNLOAD, true); 
			}
		}
	}
	
	public static void requestDownload(Image[] images, boolean force) { 
		if (images == null || images.length == 0) return;
		
		for (Image image : images) {
			Image img = image;
			if (img != null && img instanceof HttpImage) 
				requestDownload((HttpImage)img, force);
		}
	}
	
	public static boolean checkNotDownload(Image[] images) { 
		if (images == null || images.length == 0) return false;
		
		for (Image image : images) {
			Image img = image;
			if (img != null && img instanceof HttpImage) {
				if (checkNotDownload((HttpImage)img)) 
					return true;
			}
		}
		
		return false;
	}
	
	public static boolean checkNotDownload(Collection<HttpImageItem> images) { 
		if (images == null) return false;
		
		for (HttpImageItem image : images) {
			Image img = image != null ? image.getImage() : null;
			if (img != null && img instanceof HttpImage) {
				if (checkNotDownload((HttpImage)img)) 
					return true;
			}
		}
		
		return false;
	}
	
	public static boolean checkNotDownload(HttpImageItem[] images) { 
		if (images == null) return false;
		
		for (HttpImageItem image : images) {
			Image img = image != null ? image.getImage() : null;
			if (img != null && img instanceof HttpImage) {
				if (checkNotDownload((HttpImage)img)) 
					return true;
			}
		}
		
		return false;
	}
	
	public static boolean checkNotDownload(HttpImageItem image) { 
		if (image == null) return false;
		
		Image img = image != null ? image.getImage() : null;
		if (img != null && img instanceof HttpImage) {
			if (checkNotDownload((HttpImage)img)) 
				return true;
		}
		
		return false;
	}
	
	public static boolean checkNotDownload(HttpImage image) { 
		if (image != null) { 
			image.checkDownload(HttpImage.RequestType.NONE, true); 
			
			if (image.getDownloadStatus() != HttpImage.DownloadStatus.DOWNLOADED) 
				return true;
		}
		
		return false;
	}
	
}
