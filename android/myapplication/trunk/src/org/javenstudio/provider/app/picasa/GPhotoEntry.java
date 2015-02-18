package org.javenstudio.provider.app.picasa;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class GPhotoEntry extends BaseEntry {

	public static class Thumbnails {
		public String url;
		
		public String widthStr;
		public String heightStr;
		
		public int width;
		public int height;
	}
	
	public static class ResultInfo { 
		
		public String id;
		
		public String totalResultsStr;
		public int totalResults;
		
		public String startIndexStr;
		public int startIndex;
		
		public String itemsPerPageStr;
		public int itemsPerPage;
	}
	
	public String id;
	public String photoId;
	
	public String updatedStr;
	public long updated;
	
	public String title;
	public String summary;
	
	public String contentType;
	public String contentSrc;
	
	public String authorType;
	public String authorName;
	public String authorNickName;
	public String authorThumbnail;
	public String authorUser;
	
	public String version;
	
	public String widthStr;
	public String heightStr;
	
	public int width;
	public int height;
	
	public String sizeStr;
	public int size;
	
	public String timestamp;
	public String imageVersion;
	
	public String commentingEnabled;
	public String commentCountStr;
	
	public int commentCount;
	
	public String mediaType;
	public String mediaUrl;
	
	public String mediaWidthStr;
	public String mediaHeightStr;
	
	public int mediaWidth;
	public int mediaHeight;
	
	public String mediaDescription;
	public String mediaTitle;
	
	public String albumId;
	public String albumTitle;
	
	public Thumbnails[] thumbnails;
	

	public static ResultInfo parseInfo(Node node) { 
		if (node == null) return null;
		ResultInfo entry = new ResultInfo();
		
		entry.id = node.getFirstChildValue("id");
		
    	entry.totalResultsStr = node.getFirstChildValue("openSearch:totalResults");
    	entry.totalResults = parseInt(entry.totalResultsStr);
    	
    	entry.startIndexStr = node.getFirstChildValue("openSearch:startIndex");
    	entry.startIndex = parseInt(entry.startIndexStr);
    	
    	entry.itemsPerPageStr = node.getFirstChildValue("openSearch:itemsPerPage");
    	entry.itemsPerPage = parseInt(entry.itemsPerPageStr);
		
		return entry;
	}
	
    public static GPhotoEntry parseEntry(Node node) { 
    	if (node == null) return null;
    	GPhotoEntry entry = new GPhotoEntry();
    	
    	entry.id = node.getFirstChildValue("id");
    	entry.photoId = node.getFirstChildValue("gphoto:id");
    	
    	entry.albumId = node.getFirstChildValue("gphoto:albumid");
    	entry.albumTitle = node.getFirstChildValue("gphoto:albumtitle");
    	
    	entry.updatedStr = node.getFirstChildValue("updated");
    	entry.updated = Utilities.parseTime(entry.updatedStr);
    	
    	entry.title = node.getFirstChildValue("title");
    	entry.summary = node.getFirstChildValue("summary");
    	
    	Node content = node.getFirstChild("content");
    	if (content != null) { 
    		entry.contentType = content.getAttribute("type");
    		entry.contentSrc = content.getAttribute("src");
    	}
    	
    	Node author = node.getFirstChild("author");
    	if (author != null) { 
    		entry.authorType = author.getAttribute("type");
    		entry.authorName = author.getFirstChildValue("name");
    		entry.authorNickName = author.getFirstChildValue("gphoto:nickname");
    		entry.authorThumbnail = author.getFirstChildValue("gphoto:thumbnail");
    		entry.authorUser = author.getFirstChildValue("gphoto:user");
    		
    		entry.authorThumbnail = GPhotoHelper.normalizeAvatarLocation(entry.authorThumbnail);
    	}
    	
    	entry.version = node.getFirstChildValue("gphoto:version");
    	
    	entry.widthStr = node.getFirstChildValue("gphoto:width");
    	entry.heightStr = node.getFirstChildValue("gphoto:height");
    	
    	entry.width = parseInt(entry.widthStr);
    	entry.height = parseInt(entry.heightStr);
    	
    	entry.sizeStr = node.getFirstChildValue("gphoto:size");
    	entry.size = parseInt(entry.sizeStr);
    	
    	entry.timestamp = node.getFirstChildValue("gphoto:timestamp");
    	entry.imageVersion = node.getFirstChildValue("gphoto:imageVersion");
    	
    	entry.commentingEnabled = node.getFirstChildValue("gphoto:commentingEnabled");
    	entry.commentCountStr = node.getFirstChildValue("gphoto:commentCount");
    	entry.commentCount = parseInt(entry.commentCountStr);
    	
    	Node mediaGroup = node.getFirstChild("media:group");
    	if (mediaGroup != null) { 
    		Node mediaContent = mediaGroup.getFirstChild("media:content");
    		if (mediaContent != null) { 
    			entry.mediaType = mediaContent.getAttribute("type");
    			entry.mediaUrl = mediaContent.getAttribute("url");
    			entry.mediaWidthStr = mediaContent.getAttribute("width");
    			entry.mediaHeightStr = mediaContent.getAttribute("height");
    			
				entry.mediaWidth = parseInt(entry.mediaWidthStr);
				entry.mediaHeight = parseInt(entry.mediaHeightStr);
    		}
    		
    		entry.mediaDescription = mediaGroup.getFirstChildValue("media:description");
    		entry.mediaTitle = mediaGroup.getFirstChildValue("media:title");
    		
    		Node[] thumbnails = mediaGroup.getChildList("media:thumbnail");
    		if (thumbnails != null && thumbnails.length > 0) {
    			entry.thumbnails = new GPhotoEntry.Thumbnails[thumbnails.length];
    			
	    		for (int i=0; i < thumbnails.length; i++) { 
	    			Node thumbnail = thumbnails[i];
	    			GPhotoEntry.Thumbnails thumb = new GPhotoEntry.Thumbnails();
	    			
	    			thumb.url = thumbnail.getAttribute("url");
	    			thumb.widthStr = thumbnail.getAttribute("width");
	    			thumb.heightStr = thumbnail.getAttribute("height");
					
	    			thumb.width = parseInt(thumb.widthStr);
	    			thumb.height = parseInt(thumb.heightStr);
	    			
	    			entry.thumbnails[i] = thumb;
	    		}
    		}
    	}
    	
    	return entry;
    }
	
    static GPhotoEntry readEntry(DataInput in) throws IOException { 
    	if (in == null) return null;
    	
    	GPhotoEntry entry = new GPhotoEntry();
    	
    	entry.id = in.readUTF();
    	entry.photoId = in.readUTF();
    	
    	entry.albumId = in.readUTF();
    	entry.albumTitle = in.readUTF();
    	
    	entry.updatedStr = in.readUTF();
    	entry.updated = in.readLong();
    	
    	entry.title = in.readUTF();
    	entry.summary = in.readUTF();
    	
    	entry.contentType = in.readUTF();
    	entry.contentSrc = in.readUTF();
    	
    	entry.authorType = in.readUTF();
    	entry.authorName = in.readUTF();
    	entry.authorNickName = in.readUTF();
    	entry.authorThumbnail = in.readUTF();
    	entry.authorUser = in.readUTF();
    	
    	entry.version = in.readUTF();
    	
    	entry.widthStr = in.readUTF();
    	entry.heightStr = in.readUTF();
    	
    	entry.width = in.readInt();
    	entry.height = in.readInt();
    	
    	entry.sizeStr = in.readUTF();
    	entry.size = in.readInt();
    	
    	entry.timestamp = in.readUTF();
    	entry.imageVersion = in.readUTF();
    	
    	entry.commentingEnabled = in.readUTF();
    	entry.commentCountStr = in.readUTF();
    	entry.commentCount = in.readInt();
    	
    	entry.mediaType = in.readUTF();
    	entry.mediaUrl = in.readUTF();
    	
    	entry.mediaWidthStr = in.readUTF();
    	entry.mediaHeightStr = in.readUTF();
    	
    	entry.mediaWidth = in.readInt();
    	entry.mediaHeight = in.readInt();
    	
    	entry.mediaDescription = in.readUTF();
    	entry.mediaTitle = in.readUTF();
    	
    	int thumbSize = in.readInt();
    	entry.thumbnails = thumbSize > 0 ? new GPhotoEntry.Thumbnails[thumbSize] : null;
    	
    	for (int i=0; i < thumbSize; i++) { 
    		GPhotoEntry.Thumbnails thumb = new GPhotoEntry.Thumbnails();
    		thumb.url = in.readUTF();
    		thumb.widthStr = in.readUTF();
    		thumb.heightStr = in.readUTF();
    		thumb.width = in.readInt();
    		thumb.height = in.readInt();
    		
    		entry.thumbnails[i] = thumb;
    	}
    	
    	return entry;
    }
    
    static void writeEntry(DataOutput out, GPhotoEntry entry) throws IOException { 
    	if (out == null || entry == null) return;
    	
    	out.writeUTF(getString(entry.id));
    	out.writeUTF(getString(entry.photoId));
    	
    	out.writeUTF(getString(entry.albumId));
    	out.writeUTF(getString(entry.albumTitle));
    	
    	out.writeUTF(getString(entry.updatedStr));
    	out.writeLong(entry.updated);
    	
    	out.writeUTF(getString(entry.title));
    	out.writeUTF(getString(entry.summary));
    	
    	out.writeUTF(getString(entry.contentType));
    	out.writeUTF(getString(entry.contentSrc));
    	
    	out.writeUTF(getString(entry.authorType));
    	out.writeUTF(getString(entry.authorName));
    	out.writeUTF(getString(entry.authorNickName));
    	out.writeUTF(getString(entry.authorThumbnail));
    	out.writeUTF(getString(entry.authorUser));
    	
    	out.writeUTF(getString(entry.version));
    	
    	out.writeUTF(getString(entry.widthStr));
    	out.writeUTF(getString(entry.heightStr));
    	
    	out.writeInt(entry.width);
    	out.writeInt(entry.height);
    	
    	out.writeUTF(getString(entry.sizeStr));
    	out.writeInt(entry.size);
    	
    	out.writeUTF(getString(entry.timestamp));
    	out.writeUTF(getString(entry.imageVersion));
    	
    	out.writeUTF(getString(entry.commentingEnabled));
    	out.writeUTF(getString(entry.commentCountStr));
    	out.writeInt(entry.commentCount);
    	
    	out.writeUTF(getString(entry.mediaType));
    	out.writeUTF(getString(entry.mediaUrl));
    	
    	out.writeUTF(getString(entry.mediaWidthStr));
    	out.writeUTF(getString(entry.mediaHeightStr));
    	
    	out.writeInt(entry.mediaWidth);
    	out.writeInt(entry.mediaHeight);
    	
    	out.writeUTF(getString(entry.mediaDescription));
    	out.writeUTF(getString(entry.mediaTitle));
    	
    	out.writeInt(entry.thumbnails != null ? entry.thumbnails.length : 0);
    	if (entry.thumbnails != null) { 
    		for (int i=0; i < entry.thumbnails.length; i++) { 
    			GPhotoEntry.Thumbnails thumb = entry.thumbnails[i];
    			out.writeUTF(getString(thumb.url));
    			out.writeUTF(getString(thumb.widthStr));
    			out.writeUTF(getString(thumb.heightStr));
    			out.writeInt(thumb.width);
    			out.writeInt(thumb.height);
    		}
    	}
    }
    
}
