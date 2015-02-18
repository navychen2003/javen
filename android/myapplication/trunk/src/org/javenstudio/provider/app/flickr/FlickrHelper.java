package org.javenstudio.provider.app.flickr;

import org.javenstudio.cocoka.net.http.HttpException;
import org.javenstudio.cocoka.net.http.fetch.FetchHelper;
import org.javenstudio.cocoka.net.http.fetch.HtmlCallback;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.common.parser.util.XmlParser;
import org.javenstudio.common.parser.xml.NodeXml;
import org.javenstudio.common.util.Logger;

final class FlickrHelper {
	private static final Logger LOG = Logger.getLogger(FlickrHelper.class);
	
	static final int MAX_RESULTS = 100;
	static final String QUERY_PARAMS = ""; //"&per_page=" + MAX_RESULTS + "&page=1";
	
	static final String REST_URL = "http://api.flickr.com/services/rest/?api_key=d683a4c48629ed724d94c064f27611b8";
	
	static final String GETRECENT_URL = REST_URL + "&method=flickr.photos.getRecent" + QUERY_PARAMS;
	static final String INTERESTINGNESS_URL = REST_URL + "&method=flickr.interestingness.getList" + QUERY_PARAMS;
	static final String PHOTOSEARCH_URL = REST_URL + "&method=flickr.photos.search" + QUERY_PARAMS + "&tags=";
	static final String PHOTOTAG_URL = REST_URL + "&method=flickr.photos.getRecent" + QUERY_PARAMS + "&tags=";
	static final String GROUPSEARCH_URL = REST_URL + "&method=flickr.groups.search" + QUERY_PARAMS + "&text=";
	
	static final String PHOTOSET_URL = REST_URL + "&method=flickr.photosets.getPhotos&photoset_id=";
	static final String PHOTO_INFO_URL = REST_URL + "&method=flickr.photos.getInfo&photo_id=";
	static final String PHOTO_EXIF_URL = REST_URL + "&method=flickr.photos.getExif&photo_id=";
	static final String PHOTO_COMMENTS_URL = REST_URL + "&method=flickr.photos.comments.getList&photo_id=";
	
	static final String USER_INFO_URL = REST_URL + "&method=flickr.people.getInfo&user_id=";
	static final String USER_PHOTOSETS_URL = REST_URL + "&method=flickr.photosets.getList&user_id=";
	static final String USER_CONTACTS_URL = REST_URL + "&method=flickr.contacts.getPublicList&user_id=";
	static final String USER_FAVORITES_URL = REST_URL + "&method=flickr.favorites.getPublicList&user_id=";
	static final String USER_GROUPS_URL = REST_URL + "&method=flickr.people.getPublicGroups&user_id=";
	
	static final String GROUP_INFO_URL = REST_URL + "&method=flickr.groups.getInfo&group_id=";
	static final String GROUP_PHOTOS_URL = REST_URL + "&method=flickr.groups.pools.getPhotos&group_id=";
	static final String GROUP_TOPICS_URL = REST_URL + "&method=flickr.groups.discuss.topics.getList&group_id=";
	static final String GROUP_REPLIES_URL = REST_URL + "&method=flickr.groups.discuss.replies.getList&topic_id=";
	
	static String getPhotoURL(YPhotoEntry entry) { 
		if (entry == null || isEmpty(entry.photoId) || isEmpty(entry.secret) || 
			isEmpty(entry.farmId) || isEmpty(entry.serverId))
			return null;
		
		return "http://farm" + entry.farmId + ".staticflickr.com/" + entry.serverId 
				+ "/" + entry.photoId + "_" + entry.secret + ".jpg";
	}
	
	static String getPhotoURL(YPhotoItemEntry entry) { 
		if (entry == null || isEmpty(entry.photoId) || isEmpty(entry.secret) || 
			isEmpty(entry.farmId) || isEmpty(entry.serverId))
			return null;
		
		return "http://farm" + entry.farmId + ".staticflickr.com/" + entry.serverId 
				+ "/" + entry.photoId + "_" + entry.secret + ".jpg";
	}
	
	static String getPhotoURL(YPhotoSetEntry entry) { 
		if (entry == null || isEmpty(entry.primary) || isEmpty(entry.secret) || 
			isEmpty(entry.farmId) || isEmpty(entry.serverId))
			return null;
		
		return "http://farm" + entry.farmId + ".staticflickr.com/" + entry.serverId 
				+ "/" + entry.primary + "_" + entry.secret + ".jpg";
	}
	
	static String getPhotoURL(YFavoriteEntry entry) { 
		if (entry == null || isEmpty(entry.photoId) || isEmpty(entry.secret) || 
			isEmpty(entry.farmId) || isEmpty(entry.serverId))
			return null;
		
		return "http://farm" + entry.farmId + ".staticflickr.com/" + entry.serverId 
				+ "/" + entry.photoId + "_" + entry.secret + ".jpg";
	}
	
	static String getIconURL(String userid, String farmid, String serverid) { 
		if (isEmpty(userid) || isEmpty(farmid) || isEmpty(serverid))
			return null;
		
		return "http://farm" + farmid + ".staticflickr.com/" + serverid + "/buddyicons/" + userid + ".jpg";
	}
	
	static boolean isEmpty(String str) { 
		return str == null || str.length() == 0;
	}
	
	static void fetchPhotoInfo(String photoId, 
			YPhotoInfoEntry.FetchListener listener, 
			boolean schedule) { 
		fetchPhotoInfo(photoId, listener, schedule, false);
	}
	
	static void fetchPhotoInfo(String photoId, 
			final YPhotoInfoEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (photoId == null || listener == null) return;
		
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
		
		String location = PHOTO_INFO_URL + photoId;
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
			YPhotoInfoEntry.FetchListener listener) { 
		YPhotoInfoEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				for (int i=0; i < xml.getChildCount(); i++) { 
					Node rspChild = xml.getChildAt(i);
					if (rspChild != null && "photo".equalsIgnoreCase(rspChild.getName())) {
						entry = YPhotoInfoEntry.parseEntry(rspChild);
						break;
					}
				}
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new YPhotoInfoEntry();
    	if (listener != null) 
    		listener.onPhotoInfoFetched(entry);
	}

	static void fetchPhotoExif(String photoId, 
			YPhotoExifEntry.FetchListener listener, 
			boolean schedule) { 
		fetchPhotoExif(photoId, listener, schedule, false);
	}
	
	static void fetchPhotoExif(String photoId, 
			final YPhotoExifEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (photoId == null || listener == null) return;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
					listener.onPhotoExifFetching(source);
				}
				@Override
				public void onHtmlFetched(String content) {
					onPhotoExifFetched(content, listener); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onPhotoExifFetched(null, listener); 
				}
			};
		
		cb.setRefetchContent(refetch);
		cb.setSaveContent(true);
		
		String location = PHOTO_EXIF_URL + photoId;
		FetchHelper.removeFailed(location);
		
		if (LOG.isDebugEnabled())
			LOG.debug("fetchPhotoExif: " + location + " schedule=" + schedule);
		
		if (schedule) {
			cb.setFetchContent(true);
			FetchHelper.scheduleFetchHtml(location, cb);
			
		} else {
			cb.setFetchContent(false);
			FetchHelper.fetchHtml(location, cb);
		}
	}
	
	static void onPhotoExifFetched(String content, 
			YPhotoExifEntry.FetchListener listener) { 
		YPhotoExifEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				for (int i=0; i < xml.getChildCount(); i++) { 
					Node rspChild = xml.getChildAt(i);
					if (rspChild != null && "photo".equalsIgnoreCase(rspChild.getName())) {
						entry = YPhotoExifEntry.parseEntry(rspChild);
						break;
					}
				}
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new YPhotoExifEntry();
    	if (listener != null) 
    		listener.onPhotoExifFetched(entry);
	}
	
	static void fetchPhotoComment(String photoId, 
			YPhotoCommentEntry.FetchListener listener, 
			boolean schedule) { 
		fetchPhotoComment(photoId, listener, schedule, false);
	}
	
	static void fetchPhotoComment(String photoId, 
			final YPhotoCommentEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (photoId == null || listener == null) return;
		
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
		
		String location = PHOTO_COMMENTS_URL + photoId;
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
			YPhotoCommentEntry.FetchListener listener) { 
		YPhotoCommentEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				for (int i=0; i < xml.getChildCount(); i++) { 
					Node rspChild = xml.getChildAt(i);
					if (rspChild != null && "comments".equalsIgnoreCase(rspChild.getName())) {
						entry = YPhotoCommentEntry.parseEntry(rspChild, listener);
						break;
					}
				}
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new YPhotoCommentEntry();
    	if (listener != null) 
    		listener.onPhotoCommentFetched(entry);
	}
	
	static void fetchUserInfo(String userId, 
			YUserInfoEntry.FetchListener listener, 
			boolean schedule) { 
		fetchUserInfo(userId, listener, schedule, false);
	}
	
	static void fetchUserInfo(String userId, 
			final YUserInfoEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (userId == null || listener == null) return;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
					listener.onUserInfoFetching(source);
				}
				@Override
				public void onHtmlFetched(String content) {
					onUserInfoFetched(content, listener); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onUserInfoFetched(null, listener); 
				}
			};
		
		cb.setRefetchContent(refetch);
		cb.setSaveContent(true);
		
		String location = USER_INFO_URL + userId;
		FetchHelper.removeFailed(location);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("fetchUserInfo: " + location + " schedule=" 
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
	
	static void onUserInfoFetched(String content, 
			YUserInfoEntry.FetchListener listener) { 
		YUserInfoEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				for (int i=0; i < xml.getChildCount(); i++) { 
					Node rspChild = xml.getChildAt(i);
					if (rspChild != null && "person".equalsIgnoreCase(rspChild.getName())) {
						entry = YUserInfoEntry.parseEntry(rspChild);
						break;
					}
				}
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new YUserInfoEntry();
    	if (listener != null) 
    		listener.onUserInfoFetched(entry);
	}
	
	static void fetchGroupInfo(String groupId, 
			YGroupInfoEntry.FetchListener listener, 
			boolean schedule) { 
		fetchGroupInfo(groupId, listener, schedule, false);
	}
	
	static void fetchGroupInfo(String groupId, 
			final YGroupInfoEntry.FetchListener listener, 
			boolean schedule, boolean refetch) { 
		if (groupId == null || listener == null) return;
		
		HtmlCallback cb = new HtmlCallback() {
				@Override
				public void onStartFetching(String source) { 
					super.onStartFetching(source);
					listener.onGroupInfoFetching(source);
				}
				@Override
				public void onHtmlFetched(String content) {
					onGroupInfoFetched(content, listener); 
				}
				@Override
				public void onHttpException(HttpException e) { 
					onGroupInfoFetched(null, listener); 
				}
			};
		
		cb.setRefetchContent(refetch);
		cb.setSaveContent(true);
		
		String location = GROUP_INFO_URL + groupId;
		FetchHelper.removeFailed(location);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("fetchGroupInfo: " + location + " schedule=" 
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
	
	static void onGroupInfoFetched(String content, 
			YGroupInfoEntry.FetchListener listener) { 
		YGroupInfoEntry entry = null;
    	try { 
    		if (content != null && content.length() > 0) {
				NodeXml.Handler handler = new NodeXml.Handler("rsp"); 
				XmlParser parser = new XmlParser(handler); 
				parser.parse(content); 
				
				NodeXml xml = handler.getEntity(); 
				for (int i=0; i < xml.getChildCount(); i++) { 
					Node rspChild = xml.getChildAt(i);
					if (rspChild != null && "group".equalsIgnoreCase(rspChild.getName())) {
						entry = YGroupInfoEntry.parseEntry(rspChild);
						break;
					}
				}
    		}
    	} catch (Throwable e) { 
			if (LOG.isErrorEnabled())
				LOG.error("parse error: " + e.toString(), e); 
		}
    	
    	if (entry == null) entry = new YGroupInfoEntry();
    	if (listener != null) 
    		listener.onGroupInfoFetched(entry);
	}
	
}
