package org.javenstudio.provider.app.flickr;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YUserInfoEntry extends BaseEntry {

	public static interface FetchListener { 
		public void onUserInfoFetching(String source);
		public void onUserInfoFetched(YUserInfoEntry entry);
	}
	
	public String userId;
	public String nsid;
	public String ispro;
	public String iconserver;
	public String iconfarm;
	public String path_alias;
	public String datecreateStr;
	public long datecreate;
	
	public String username;
	public String realname;
	public String location;
	public String description;
	
	public String photosurl;
	public String profileurl;
	public String mobileurl;
	
	public String photos_firstdatetaken;
	public String photos_firstdate;
	public String photos_countStr;
	public int photos_count;
	
	static YUserInfoEntry parseEntry(Node node) { 
		YUserInfoEntry entry = new YUserInfoEntry();
		
		entry.userId = node.getAttribute("id");
		entry.nsid = node.getAttribute("nsid");
		entry.ispro = node.getAttribute("ispro");
		entry.iconserver = node.getAttribute("iconserver");
		entry.iconfarm = node.getAttribute("iconfarm");
		entry.path_alias = node.getAttribute("path_alias");
		
		entry.datecreateStr = node.getAttribute("datecreate");
		entry.datecreate = parseLong(entry.datecreateStr) * 1000;
		
		entry.username = node.getFirstChildValue("username");
		entry.realname = node.getFirstChildValue("realname");
		entry.location = node.getFirstChildValue("location");
		entry.description = node.getFirstChildValue("description");
		
		entry.photosurl = node.getFirstChildValue("photosurl");
		entry.profileurl = node.getFirstChildValue("profileurl");
		entry.mobileurl = node.getFirstChildValue("mobileurl");
		
		Node photos = node.getFirstChild("photos");
		if (photos != null) { 
			entry.photos_firstdatetaken = photos.getFirstChildValue("firstdatetaken");
			entry.photos_firstdate = photos.getFirstChildValue("firstdate");
			entry.photos_countStr = photos.getFirstChildValue("count");
			entry.photos_count = parseInt(entry.photos_countStr);
		}
		
		return entry;
	}
	
}
