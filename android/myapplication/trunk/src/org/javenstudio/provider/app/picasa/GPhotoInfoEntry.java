package org.javenstudio.provider.app.picasa;

import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class GPhotoInfoEntry extends BaseEntry {

	public static interface FetchListener { 
		public void onPhotoInfoFetching(String source);
		public void onPhotoInfoFetched(GPhotoInfoEntry entry);
	}
	
	public static class Thumbnails {
		public String url;
		
		public String widthStr;
		public String heightStr;
		
		public int width;
		public int height;
	}
	
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
	
	public String viewCountStr;
	public int viewCount;
	
	public String exifFstop;
	public String exifMake;
	public String exifModel;
	public String exifExposure;
	public String exifFlash;
	public String exifFocallength;
	public String exifIso;
	public String exifTimeStr;
	public long exifTime;
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
	

    public static GPhotoInfoEntry parseEntry(Node node) { 
    	GPhotoInfoEntry entry = new GPhotoInfoEntry();
    	
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
    	
    	entry.viewCountStr = node.getFirstChildValue("gphoto:viewCount");
    	entry.viewCount = parseInt(entry.viewCountStr);
    	
    	Node exif = node.getFirstChild("exif:tags");
    	if (exif != null) { 
    		entry.exifFstop = exif.getFirstChildValue("exif:fstop");
    		entry.exifMake = exif.getFirstChildValue("exif:make");
    		entry.exifModel = exif.getFirstChildValue("exif:model");
    		entry.exifExposure = exif.getFirstChildValue("exif:exposure");
    		entry.exifFlash = exif.getFirstChildValue("exif:flash");
    		entry.exifFocallength = exif.getFirstChildValue("exif:focallength");
    		entry.exifIso = exif.getFirstChildValue("exif:iso");
    		entry.exifTimeStr = exif.getFirstChildValue("exif:time");
    		entry.exifImageUniqueID = exif.getFirstChildValue("exif:imageUniqueID");
    		entry.exifTime = parseLong(entry.exifTimeStr);
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
    			entry.thumbnails = new GPhotoInfoEntry.Thumbnails[thumbnails.length];
    			
	    		for (int i=0; i < thumbnails.length; i++) { 
	    			Node thumbnail = thumbnails[i];
	    			GPhotoInfoEntry.Thumbnails thumb = new GPhotoInfoEntry.Thumbnails();
	    			
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
	
}
