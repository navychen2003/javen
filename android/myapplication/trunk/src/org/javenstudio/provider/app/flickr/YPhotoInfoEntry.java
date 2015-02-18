package org.javenstudio.provider.app.flickr;

import java.util.ArrayList;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YPhotoInfoEntry extends BaseEntry {

	public static interface FetchListener { 
		public void onPhotoInfoFetching(String source);
		public void onPhotoInfoFetched(YPhotoInfoEntry entry);
	}
	
	public static class PhotoTag { 
		public String id;
		public String author;
		public String raw;
		public String machine_tag;
		public String value;
	}
	
	public static class PhotoURL { 
		public String type;
		public String url;
	}
	
	public String photoId;
	public String ownerId;
	public String serverId;
	public String farmId;
	
	public String secret;
	public String originalsecret;
	public String originalformat;
	
	public String isfavorite;
	public String license;
	public String dateuploaded;
	public String safety_level;
	public String rotation;
	public String views;
	public String media;
	
	public String username;
	public String realname;
	public String location;
	public String iconserver;
	public String iconfarm;
	public String path_alias;
	
	public String title;
	public String description;
	
	public String ispublic;
	public String isfriend;
	public String isfamily;
	
	public String postedStr;
	public String lastupdateStr;
	
	public long posted;
	public long lastupdate;
	
	public String taken;
	public String takengranularity;
	
	public String cancomment;
	public String canaddmeta;
	
	public String public_cancomment;
	public String public_canaddmeta;
	
	public String candownload;
	public String canblog;
	public String canprint;
	public String canshare;
	
	public String comments;
	public int countComment;
	
	public String notes;
	public String haspeople;
	
	public PhotoTag[] tags;
	public PhotoURL[] urls;
	
	static YPhotoInfoEntry parseEntry(Node node) { 
		YPhotoInfoEntry entry = new YPhotoInfoEntry();
		
		entry.photoId = node.getAttribute("id");
		entry.secret = node.getAttribute("secret");
		entry.serverId = node.getAttribute("server");
		entry.farmId = node.getAttribute("farm");
		entry.dateuploaded = node.getAttribute("dateuploaded");
		entry.isfavorite = node.getAttribute("isfavorite");
		entry.license = node.getAttribute("license");
		entry.safety_level = node.getAttribute("safety_level");
		entry.rotation = node.getAttribute("rotation");
		entry.originalsecret = node.getAttribute("originalsecret");
		entry.originalformat = node.getAttribute("originalformat");
		entry.views = node.getAttribute("views");
		entry.media = node.getAttribute("media");
		
		Node owner = node.getFirstChild("owner");
		if (owner != null) { 
			entry.ownerId = owner.getAttribute("nsid");
			entry.username = owner.getAttribute("username");
			entry.realname = owner.getAttribute("realname");
			entry.location = owner.getAttribute("location");
			entry.iconserver = owner.getAttribute("iconserver");
			entry.iconfarm = owner.getAttribute("iconfarm");
			entry.path_alias = owner.getAttribute("path_alias");
		}
		
		entry.title = node.getFirstChildValue("title");
		entry.description = node.getFirstChildValue("description");
		
		Node visibility = node.getFirstChild("visibility");
		if (visibility != null) { 
			entry.ispublic = visibility.getAttribute("ispublic");
			entry.isfriend = visibility.getAttribute("isfriend");
			entry.isfamily = visibility.getAttribute("isfamily");
		}
		
		Node dates = node.getFirstChild("dates");
		if (dates != null) { 
			entry.postedStr = dates.getAttribute("posted");
			entry.lastupdateStr = dates.getAttribute("lastupdate");
			entry.taken = dates.getAttribute("taken");
			entry.takengranularity = dates.getAttribute("takengranularity");
			entry.posted = parseLong(entry.postedStr) * 1000;
			entry.lastupdate = parseLong(entry.lastupdateStr) * 1000;
		}
		
		Node editability = node.getFirstChild("editability");
		if (editability != null) { 
			entry.cancomment = editability.getAttribute("cancomment");
			entry.canaddmeta = editability.getAttribute("canaddmeta");
		}
		
		Node publiceditability = node.getFirstChild("publiceditability");
		if (publiceditability != null) { 
			entry.public_cancomment = publiceditability.getAttribute("cancomment");
			entry.public_canaddmeta = publiceditability.getAttribute("canaddmeta");
		}
		
		Node usage = node.getFirstChild("usage");
		if (usage != null) { 
			entry.candownload = usage.getAttribute("candownload");
			entry.canblog = usage.getAttribute("canblog");
			entry.canprint = usage.getAttribute("canprint");
			entry.canshare = usage.getAttribute("canshare");
		}
		
		entry.comments = node.getFirstChildValue("comments");
		entry.countComment = parseInt(entry.comments);
		entry.notes = node.getFirstChildValue("notes");
		
		Node people = node.getFirstChild("people");
		if (people != null) { 
			entry.haspeople = people.getAttribute("haspeople");
		}
		
		Node tags = node.getFirstChild("tags");
		if (tags != null) { 
			ArrayList<PhotoTag> tagList = new ArrayList<PhotoTag>();
			
			for (int i=0; i < tags.getChildCount(); i++) { 
				Node tag = tags.getChildAt(i);
				if (tag == null) continue;
				
				PhotoTag pt = new PhotoTag();
				pt.id = tag.getAttribute("id");
				pt.author = tag.getAttribute("author");
				pt.raw = tag.getAttribute("raw");
				pt.value = tag.getValue();
				
				tagList.add(pt);
			}
			
			entry.tags = tagList.toArray(new PhotoTag[tagList.size()]);
		}
		
		Node urls = node.getFirstChild("urls");
		if (urls != null) { 
			ArrayList<PhotoURL> urlList = new ArrayList<PhotoURL>();
			
			for (int i=0; i < urls.getChildCount(); i++) { 
				Node url = urls.getChildAt(i);
				if (url == null) continue;
				
				PhotoURL pu = new PhotoURL();
				pu.type = url.getAttribute("type");
				pu.url = url.getValue();
				
				urlList.add(pu);
			}
			
			entry.urls = urlList.toArray(new PhotoURL[urlList.size()]);
		}
		
		return entry;
	}
	
}
