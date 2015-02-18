package org.javenstudio.provider.publish.information;

import org.javenstudio.android.SimpleHtmlImgParser;
import org.javenstudio.android.data.image.http.HttpImageItem;
import org.javenstudio.common.util.Logger;

final class RssItem extends TextItem {
	private static final Logger LOG = Logger.getLogger(RssItem.class);

	private final RssProvider mProvider;
	
	public RssItem(RssProvider provider) { 
		mProvider = provider;
	}
	
	public RssProvider getProvider() { return mProvider; }
	
	protected void parseImg(String s) { 
		if (s == null || s.length() == 0) 
			return;
		
		SimpleHtmlImgParser parser = new SimpleHtmlImgParser() { 
				@Override
				protected void handleImg(String src, String alt, String title, 
						String original, String dataoriginal, int width, int height) { 
					onHandleImg(src, alt, title, original, dataoriginal, width, height);
				}
			};
		
		parser.parse(s);
	}
	
	protected void onHandleImg(String src, String alt, String title, 
			String original, String dataoriginal, int width, int height) { 
		String newSrc = SimpleHtmlImgParser.normalizeHref(src, getProvider().getLocation());
		String newOriginal = SimpleHtmlImgParser.normalizeHref(original, getProvider().getLocation());
		String newDataOriginal = SimpleHtmlImgParser.normalizeHref(dataoriginal, getProvider().getLocation());
		
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
	
}
