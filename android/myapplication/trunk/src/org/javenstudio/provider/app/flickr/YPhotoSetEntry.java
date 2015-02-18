package org.javenstudio.provider.app.flickr;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YPhotoSetEntry extends BaseEntry {

	public static class ResultInfo { 
		public int page;
		public int pages;
		public int perpage;
		public int total;
	}
	
	public String photosetId;
	public String serverId;
	public String farmId;
	
	public String primary;
	public String secret;
	
	public String photos;
	public String videos;
	
	public int countPhotos;
	public int countVideos;
	
	public String needs_interstitial;
	public String visibility_can_see_set;
	
	public String count_views;
	public String count_comments;
	
	public int countViews;
	public int countComments;
	
	public String can_comment;
	
	public String date_create;
	public String date_update;
	
	public Date dateCreate;
	public Date dateUpdate;
	
	public String title;
	public String description;
	
	
	static ResultInfo parseInfo(Node node) { 
		ResultInfo entry = new ResultInfo();
		
		entry.page = parseInt(node.getAttribute("page"));
		entry.pages = parseInt(node.getAttribute("pages"));
		entry.perpage = parseInt(node.getAttribute("perpage"));
		entry.total = parseInt(node.getAttribute("total"));
		
		return entry;
	}
	
	static YPhotoSetEntry parseEntry(Node node) { 
		YPhotoSetEntry entry = new YPhotoSetEntry();
		
		entry.photosetId = node.getAttribute("id");
		
		entry.primary = node.getAttribute("primary");
		entry.secret = node.getAttribute("secret");
		
		entry.serverId = node.getAttribute("server");
		entry.farmId = node.getAttribute("farm");
		
		entry.photos = node.getAttribute("photos");
		entry.videos = node.getAttribute("videos");
		
		entry.countPhotos = parseInt(entry.photos);
		entry.countVideos = parseInt(entry.videos);
		
		entry.needs_interstitial = node.getAttribute("needs_interstitial");
		entry.visibility_can_see_set = node.getAttribute("visibility_can_see_set");
		
		entry.count_views = node.getAttribute("count_views");
		entry.count_comments = node.getAttribute("count_comments");
		
		entry.countViews = parseInt(entry.count_views);
		entry.countComments = parseInt(entry.count_comments);
		
		entry.can_comment = node.getAttribute("can_comment");
		
		entry.date_create = node.getAttribute("date_create");
		entry.date_update = node.getAttribute("date_update");
		
		entry.dateCreate = new Date(parseLong(entry.date_create));
		entry.dateUpdate = new Date(parseLong(entry.date_update));
		
		entry.title = node.getFirstChildValue("title");
		entry.description = node.getFirstChildValue("description");
		
		return entry;
	}
	
	static YPhotoSetEntry readEntry(DataInput in) throws IOException { 
		if (in == null) return null;
		
		YPhotoSetEntry entry = new YPhotoSetEntry();
		
		entry.photosetId = in.readUTF();
		entry.serverId = in.readUTF();
		entry.farmId = in.readUTF();
		
		entry.primary = in.readUTF();
		entry.secret = in.readUTF();
		
		entry.photos = in.readUTF();
		entry.videos = in.readUTF();
		
		entry.countPhotos = in.readInt();
		entry.countVideos = in.readInt();
		
		entry.needs_interstitial = in.readUTF();
		entry.visibility_can_see_set = in.readUTF();
		
		entry.count_views = in.readUTF();
		entry.count_comments = in.readUTF();
		
		entry.countViews = in.readInt();
		entry.countComments = in.readInt();
		
		entry.can_comment = in.readUTF();
		
		entry.date_create = in.readUTF();
		entry.date_update = in.readUTF();
		
		entry.dateCreate = new Date(in.readLong());
		entry.dateUpdate = new Date(in.readLong());
		
		entry.title = in.readUTF();
		entry.description = in.readUTF();
		
		return entry;
	}
	
	static void writeEntry(DataOutput out, YPhotoSetEntry entry) throws IOException { 
		if (out == null || entry == null) return;
		
		out.writeUTF(getString(entry.photosetId));
		out.writeUTF(getString(entry.serverId));
		out.writeUTF(getString(entry.farmId));
		
		out.writeUTF(getString(entry.primary));
		out.writeUTF(getString(entry.secret));
		
		out.writeUTF(getString(entry.photos));
		out.writeUTF(getString(entry.videos));
		
		out.writeInt(entry.countPhotos);
		out.writeInt(entry.countVideos);
		
		out.writeUTF(getString(entry.needs_interstitial));
		out.writeUTF(getString(entry.visibility_can_see_set));
		
		out.writeUTF(getString(entry.count_views));
		out.writeUTF(getString(entry.count_comments));
		
		out.writeInt(entry.countViews);
		out.writeInt(entry.countComments);
		
		out.writeUTF(getString(entry.can_comment));
		
		out.writeUTF(getString(entry.date_create));
		out.writeUTF(getString(entry.date_update));
		
		out.writeLong(entry.dateCreate != null ? entry.dateCreate.getTime() : 0);
		out.writeLong(entry.dateUpdate != null ? entry.dateUpdate.getTime() : 0);
		
		out.writeUTF(getString(entry.title));
		out.writeUTF(getString(entry.description));
	}
	
}
