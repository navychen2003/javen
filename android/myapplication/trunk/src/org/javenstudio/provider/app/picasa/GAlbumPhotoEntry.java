package org.javenstudio.provider.app.picasa;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class GAlbumPhotoEntry extends BaseEntry {

	public static class Thumbnails {
		public String url;
		
		public String widthStr;
		public String heightStr;
		
		public int width;
		public int height;
	}
	
	public static class ResultInfo { 
		
		public String id;
		public String title;
		public String subtitle;
		
		public String userId;
		public String userName;
		public String nickName;
		
		public String totalResultsStr;
		public int totalResults;
		
		public String startIndexStr;
		public int startIndex;
		
		public String itemsPerPageStr;
		public int itemsPerPage;
	}
	
	public String user;
	
	public String id;
	public String photoId;
	public String albumId;
	
	public String updatedStr;
	public long updated;
	
	public String title;
	public String summary;
	
	public String contentType;
	public String contentSrc;
	
	public String version;
	public String position;
	
	public String widthStr;
	public String heightStr;
	
	public int width;
	public int height;
	
	public String sizeStr;
	public int size;
	
	public String client;
	public String imageVersion;
	
	public String commentCountStr;
	public int commentCount;
	
	public String exifFstop;
	public String exifMake;
	public String exifModel;
	public String exifExposure;
	public String exifFlash;
	public String exifFocallength;
	public String exifIso;
	public String exifTime;
	public String exifImageUniqueID;
	
	public String mediaType;
	public String mediaUrl;
	
	public String mediaWidthStr;
	public String mediaHeightStr;
	
	public int mediaWidth;
	public int mediaHeight;
	
	public String mediaDescription;
	public String mediaTitle;
	
	public Thumbnails[] thumbnails;
	

	public static ResultInfo parseInfo(Node node) { 
		if (node == null) return null;
		ResultInfo entry = new ResultInfo();
		
		entry.id = node.getFirstChildValue("id");
		entry.title = node.getFirstChildValue("title");
		entry.subtitle = node.getFirstChildValue("subtitle");
		
		entry.userId = node.getFirstChildValue("gphoto:user");
		entry.nickName = node.getFirstChildValue("gphoto:nickname");
		
		Node author = node.getFirstChild("author");
		if (author != null) { 
			entry.userName = author.getFirstChildValue("name");
		}
		
    	entry.totalResultsStr = node.getFirstChildValue("openSearch:totalResults");
    	entry.totalResults = parseInt(entry.totalResultsStr);
    	
    	entry.startIndexStr = node.getFirstChildValue("openSearch:startIndex");
    	entry.startIndex = parseInt(entry.startIndexStr);
    	
    	entry.itemsPerPageStr = node.getFirstChildValue("openSearch:itemsPerPage");
    	entry.itemsPerPage = parseInt(entry.itemsPerPageStr);
		
		return entry;
	}
	
    public static GAlbumPhotoEntry parseEntry(Node node) { 
    	GAlbumPhotoEntry entry = new GAlbumPhotoEntry();
    	
    	entry.updatedStr = node.getFirstChildValue("updated");
    	entry.updated = Utilities.parseTime(entry.updatedStr);
    	
    	entry.id = node.getFirstChildValue("id");
    	entry.title = node.getFirstChildValue("title");
    	entry.summary = node.getFirstChildValue("summary");
    	
    	Node content = node.getFirstChild("content");
    	if (content != null) { 
    		entry.contentType = content.getAttribute("type");
    		entry.contentSrc = content.getAttribute("src");
    	}
    	
    	entry.albumId = node.getFirstChildValue("gphoto:albumid");
    	entry.photoId = node.getFirstChildValue("gphoto:id");
    	
    	entry.version = node.getFirstChildValue("gphoto:version");
    	entry.position = node.getFirstChildValue("gphoto:position");
    	
    	entry.widthStr = node.getFirstChildValue("gphoto:width");
    	entry.heightStr = node.getFirstChildValue("gphoto:height");
    	
    	entry.width = parseInt(entry.widthStr);
    	entry.height = parseInt(entry.heightStr);
    	
    	entry.sizeStr = node.getFirstChildValue("gphoto:size");
    	entry.size = parseInt(entry.sizeStr);
    	
    	entry.client = node.getFirstChildValue("gphoto:client");
    	entry.imageVersion = node.getFirstChildValue("gphoto:imageVersion");
    	
    	entry.commentCountStr = node.getFirstChildValue("gphoto:commentCount");
    	entry.commentCount = parseInt(entry.commentCountStr);
    	
    	Node exif = node.getFirstChild("exif:tags");
    	if (exif != null) { 
    		entry.exifFstop = exif.getFirstChildValue("exif:fstop");
    		entry.exifMake = exif.getFirstChildValue("exif:make");
    		entry.exifModel = exif.getFirstChildValue("exif:model");
    		entry.exifExposure = exif.getFirstChildValue("exif:exposure");
    		entry.exifFlash = exif.getFirstChildValue("exif:flash");
    		entry.exifFocallength = exif.getFirstChildValue("exif:focallength");
    		entry.exifIso = exif.getFirstChildValue("exif:iso");
    		entry.exifTime = exif.getFirstChildValue("exif:time");
    		entry.exifImageUniqueID = exif.getFirstChildValue("exif:imageUniqueID");
    	}
    	
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
    			entry.thumbnails = new GAlbumPhotoEntry.Thumbnails[thumbnails.length];
    			
	    		for (int i=0; i < thumbnails.length; i++) { 
	    			Node thumbnail = thumbnails[i];
	    			GAlbumPhotoEntry.Thumbnails thumb = new GAlbumPhotoEntry.Thumbnails();
	    			
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
	
    static GAlbumPhotoEntry readEntry(DataInput in) throws IOException { 
    	if (in == null) return null;
    	
    	GAlbumPhotoEntry entry = new GAlbumPhotoEntry();
    	
    	entry.user = in.readUTF();
    	
    	entry.id = in.readUTF();
    	entry.photoId = in.readUTF();
    	entry.albumId = in.readUTF();
    	
    	entry.updatedStr = in.readUTF();
    	entry.updated = in.readLong();
    	
    	entry.title = in.readUTF();
    	entry.summary = in.readUTF();
    	
    	entry.contentType = in.readUTF();
    	entry.contentSrc = in.readUTF();
    	
    	entry.version = in.readUTF();
    	entry.position = in.readUTF();
    	
    	entry.widthStr = in.readUTF();
    	entry.heightStr = in.readUTF();
    	
    	entry.width = in.readInt();
    	entry.height = in.readInt();
    	
    	entry.sizeStr = in.readUTF();
    	entry.size = in.readInt();
    	
    	entry.client = in.readUTF();
    	entry.imageVersion = in.readUTF();
    	
    	entry.commentCountStr = in.readUTF();
    	entry.commentCount = in.readInt();
    	
    	entry.exifFstop = in.readUTF();
    	entry.exifMake = in.readUTF();
    	entry.exifModel = in.readUTF();
    	entry.exifExposure = in.readUTF();
    	entry.exifFlash = in.readUTF();
    	entry.exifFocallength = in.readUTF();
    	entry.exifIso = in.readUTF();
    	entry.exifTime = in.readUTF();
    	entry.exifImageUniqueID = in.readUTF();
    	
    	entry.mediaType = in.readUTF();
    	entry.mediaUrl = in.readUTF();
    	
    	entry.mediaWidthStr = in.readUTF();
    	entry.mediaHeightStr = in.readUTF();
    	
    	entry.mediaWidth = in.readInt();
    	entry.mediaHeight = in.readInt();
    	
    	entry.mediaDescription = in.readUTF();
    	entry.mediaTitle = in.readUTF();
    	
    	int thumbSize = in.readInt();
    	entry.thumbnails = thumbSize > 0 ? new GAlbumPhotoEntry.Thumbnails[thumbSize] : null;
    	
    	for (int i=0; i < thumbSize; i++) { 
    		GAlbumPhotoEntry.Thumbnails thumb = new GAlbumPhotoEntry.Thumbnails();
    		thumb.url = in.readUTF();
    		thumb.widthStr = in.readUTF();
    		thumb.heightStr = in.readUTF();
    		thumb.width = in.readInt();
    		thumb.height = in.readInt();
    		
    		entry.thumbnails[i] = thumb;
    	}
    	
    	return entry;
    }
    
    static void writeEntry(DataOutput out, GAlbumPhotoEntry entry) throws IOException { 
    	if (out == null || entry == null) return;
    	
    	out.writeUTF(getString(entry.user));
    	
    	out.writeUTF(getString(entry.id));
    	out.writeUTF(getString(entry.photoId));
    	out.writeUTF(getString(entry.albumId));
    	
    	out.writeUTF(getString(entry.updatedStr));
    	out.writeLong(entry.updated);
    	
    	out.writeUTF(getString(entry.title));
    	out.writeUTF(getString(entry.summary));
    	
    	out.writeUTF(getString(entry.contentType));
    	out.writeUTF(getString(entry.contentSrc));
    	
    	out.writeUTF(getString(entry.version));
    	out.writeUTF(getString(entry.position));
    	
    	out.writeUTF(getString(entry.widthStr));
    	out.writeUTF(getString(entry.heightStr));
    	
    	out.writeInt(entry.width);
    	out.writeInt(entry.height);
    	
    	out.writeUTF(getString(entry.sizeStr));
    	out.writeInt(entry.size);
    	
    	out.writeUTF(getString(entry.client));
    	out.writeUTF(getString(entry.imageVersion));
    	
    	out.writeUTF(getString(entry.commentCountStr));
    	out.writeInt(entry.commentCount);
    	
    	out.writeUTF(getString(entry.exifFstop));
    	out.writeUTF(getString(entry.exifMake));
    	out.writeUTF(getString(entry.exifModel));
    	out.writeUTF(getString(entry.exifExposure));
    	out.writeUTF(getString(entry.exifFlash));
    	out.writeUTF(getString(entry.exifFocallength));
    	out.writeUTF(getString(entry.exifIso));
    	out.writeUTF(getString(entry.exifTime));
    	out.writeUTF(getString(entry.exifImageUniqueID));
    	
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
    			GAlbumPhotoEntry.Thumbnails thumb = entry.thumbnails[i];
    			out.writeUTF(getString(thumb.url));
    			out.writeUTF(getString(thumb.widthStr));
    			out.writeUTF(getString(thumb.heightStr));
    			out.writeInt(thumb.width);
    			out.writeInt(thumb.height);
    		}
    	}
    }
    
}
