package org.javenstudio.provider.app.picasa;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;

final class GPhotoHelper {
	private static final Logger LOG = Logger.getLogger(GPhotoHelper.class);

	//static final String RESULT_PARAMS = "start-index=1&max-results=100";
	static final String IMAGE_PARAMS = "imgmax=1600";
	static final String QUERY_PARAMS = "access=public";
	static final String USER_PARAMS = "access=public";
	
	static final String FEED_URL = "https://picasaweb.google.com/data/feed/";
	static final String ENTRY_URL = "https://picasaweb.google.com/data/entry/";
	
	static final String FEATURE_URL = FEED_URL + "api/featured?" + QUERY_PARAMS + "&" + IMAGE_PARAMS;
	static final String SEARCH_URL = FEED_URL + "api/all?kind=photo&" + QUERY_PARAMS + "&" + IMAGE_PARAMS + "&q=";
	static final String TAG_URL = FEED_URL + "api/all?kind=photo&" + QUERY_PARAMS + "&" + IMAGE_PARAMS + "&tag=";
	static final String ALBUMS_URL = FEED_URL + "api/user/%1$s?kind=album&" + USER_PARAMS;
	static final String ALBUM_PHOTOS_URL = FEED_URL + "api/user/%1$s/albumid/%2$s?kind=photo&" + USER_PARAMS + "&" + IMAGE_PARAMS;
	static final String COMMENTS_URL = FEED_URL + "api/user/%1$s?kind=comment&" + USER_PARAMS;
	
	static final String ACCOUNT_ALBUMS_URL = FEED_URL + "api/user/%1$s?kind=album";
	static final String ACCOUNT_DELETE_PHOTO_URL = ENTRY_URL + "api/user/%1$s/albumid/%2$s/photoid/%3$s";
	static final String ACCOUNT_DELETE_ALBUM_URL = ENTRY_URL + "api/user/%1$s/albumid/%2$s";
	static final String ACCOUNT_CREATE_ALBUM_URL = FEED_URL + "api/user/";
	
	
	static String normalizeAvatarLocation(String location) { 
		if (location != null) { 
			if (location.indexOf("/s32-c/") > 0)
				location = location.replaceAll("/s32-c/", "/s128-c/");
			else if (location.indexOf("/s48-c/") > 0) 
				location = location.replaceAll("/s48-c/", "/s128-c/");
			else if (location.indexOf("/s64-c/") > 0) 
				location = location.replaceAll("/s64-c/", "/s128-c/");
		}
		return location;
	}
	
	static String normalizePhotoLocation(String location) { 
		if (location != null && location.length() > 0) { 
			int pos1 = location.lastIndexOf('?');
			int pos2 = location.lastIndexOf("imgmax=");
			
			if (pos2 < 0) { 
				if (pos1 < 0) location += "?";
				location += "imgmax=1600";
			}
		}
		return location;
	}
	
	static String getPhotoCommentLocation(String entryId) { 
		if (entryId == null) return null;
		
		String location = entryId.replaceAll("/entry/", "/feed/");
		location += "?kind=comment"; // + "&" + QUERY_PARAMS;
		
		return location;
	}

	static String getCommentLocation(String userId) { 
		if (userId == null) return null;
		
		return String.format(COMMENTS_URL, userId);
	}
	
	static void fetchPhotoInfo(String location, 
			GPhotoInfoEntry.FetchListener listener, 
			boolean schedule) { 
		fetchPhotoInfo(location, listener, schedule, false);
	}
	
	static void fetchPhotoInfo(String location, 
			final GPhotoInfoEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (location == null || listener == null) return;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
					listener.onPhotoInfoFetching(source);
				}
				@Override
				public void onHtmlFetched(String content) {
					onPhotoInfoFetched(content, listener); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onPhotoInfoFetched(null, listener); 
				}
			};
		
		cb.setRefetchContent(refetch);
		cb.setSaveContent(true);
		
		FetchHelper.removeFailed(location);
		
		if (LOG.isDebugEnabled())
			LOG.debug("fetchPhotoInfo: " + location + " schedule=" + schedule);
		
		if (schedule) {
			cb.setFetchContent(true);
			FetchHelper.scheduleFetchHtml(location, cb);
			
		} else {
			cb.setFetchContent(false);
			FetchHelper.fetchHtml(location, cb);
		}
	}
	
	static void onPhotoInfoFetched(String content, 
			GPhotoInfoEntry.FetchListener listener) { 
		GPhotoInfoEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("entry"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				entry = GPhotoInfoEntry.parseEntry(xml);
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new GPhotoInfoEntry();
    	if (listener != null) 
    		listener.onPhotoInfoFetched(entry);
	}
	
	static void fetchPhotoComment(String location, 
			GPhotoCommentEntry.FetchListener listener, 
			boolean schedule) { 
		fetchPhotoComment(location, listener, schedule, false);
	}
	
	static void fetchPhotoComment(String location, 
			final GPhotoCommentEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (location == null || listener == null) return;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
					listener.onPhotoCommentFetching(source);
				}
				@Override
				public void onHtmlFetched(String content) {
					onPhotoCommentFetched(content, listener); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onPhotoCommentFetched(null, listener); 
				}
			};
		
		cb.setRefetchContent(refetch);
		cb.setSaveContent(true);
		
		FetchHelper.removeFailed(location);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("fetchPhotoComment: " + location + " schedule=" 
					+ schedule + " refetch=" + refetch);
		}
		
		if (schedule) {
			cb.setFetchContent(true);
			FetchHelper.scheduleFetchHtml(location, cb);
			
		} else {
			cb.setFetchContent(refetch);
			FetchHelper.fetchHtml(location, cb);
		}
	}
	
	static void onPhotoCommentFetched(String content, 
			GPhotoCommentEntry.FetchListener listener) { 
		GPhotoCommentEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("feed"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				entry = GPhotoCommentEntry.parseEntry(xml, listener);
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new GPhotoCommentEntry();
    	if (listener != null) 
    		listener.onPhotoCommentFetched(entry);
	}
	
	static void fetchComment(String location, 
			GCommentEntry.FetchListener listener, 
			boolean schedule) { 
		fetchComment(location, listener, schedule, false);
	}
	
	static void fetchComment(String location, 
			final GCommentEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (location == null || listener == null) return;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
					listener.onCommentFetching(source);
				}
				@Override
				public void onHtmlFetched(String content) {
					onCommentFetched(content, listener); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onCommentFetched(null, listener); 
				}
			};
		
		cb.setRefetchContent(refetch);
		cb.setSaveContent(true);
		
		FetchHelper.removeFailed(location);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("fetchComment: " + location + " schedule=" 
					+ schedule + " refetch=" + refetch);
		}
		
		if (schedule) {
			cb.setFetchContent(true);
			FetchHelper.scheduleFetchHtml(location, cb);
			
		} else {
			cb.setFetchContent(refetch);
			FetchHelper.fetchHtml(location, cb);
		}
	}
	
	static void onCommentFetched(String content, 
			GCommentEntry.FetchListener listener) { 
		GCommentEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("feed"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				entry = GCommentEntry.parseEntry(xml, listener);
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new GCommentEntry();
    	if (listener != null) 
    		listener.onCommentFetched(entry);
	}
	
	
	static final String[] TAGS = {
		"wedding",
		"trip",
		"day",
		"new",
		"pics",
		"christmas",
		"party",
		"family",
		"photos",
		"birthday",
		"pictures",
		"vacation",
		"park",
		"year",
		"beach",
		"summer",
		"people",
		"city",
		"fotos",
		"camera",
		"photography",
		"lake",
		"house",
		"cruise",
		"nature",
		"show",
		"photo",
		"halloween",
		"tour",
		"visit",
		"october",
		"winter",
		"valley",
		"bday",
		"water",
		"south",
		"december",
		"fall",
		"landscape",
		"flowers",
		"europe",
		"italy",
		"usa",
		"paris",
		"china",
		"france",
		"india",
		"new york",
		"hawaii"
	};
	
}
