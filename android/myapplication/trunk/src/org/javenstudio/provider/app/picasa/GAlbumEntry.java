package org.javenstudio.provider.app.picasa;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;

import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class GAlbumEntry extends BaseEntry {

	public static class ResultInfo { 
		public String totalResultsStr;
		public int totalResults;
		
		public String startIndexStr;
		public int startIndex;
		
		public String itemsPerPageStr;
		public int itemsPerPage;
		
		public String id;
		public String userId;
		
		public String updatedStr;
		public long updated;
		
		public String title;
		public String subtitle;
		
		public String authorName;
		public String authorNickName;
		public String authorThumbnail;
	}
	
	public static class Thumbnails {
		public String url;
		
		public String widthStr;
		public String heightStr;
		
		public int width;
		public int height;
	}
	
	public String id;
	public String albumId;
	
	public String updatedStr;
	public Date updated;
	
	public String title;
	public String summary;
	
	public String authorName;
	
	public String name;
	public String numphotosStr;
	public int numphotos;
	
	public String user;
	public String nickName;
	
	public String commentCountStr;
	public int commentCount;
	
	public String albumType;
	
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
		ResultInfo entry = new ResultInfo();
		
		entry.totalResultsStr = node.getFirstChildValue("openSearch:totalResults");
    	entry.totalResults = parseInt(entry.totalResultsStr);
    	
    	entry.startIndexStr = node.getFirstChildValue("openSearch:startIndex");
    	entry.startIndex = parseInt(entry.startIndexStr);
    	
    	entry.itemsPerPageStr = node.getFirstChildValue("openSearch:itemsPerPage");
    	entry.itemsPerPage = parseInt(entry.itemsPerPageStr);
    	
    	entry.id = node.getFirstChildValue("id");
    	entry.userId = node.getFirstChildValue("gphoto:user");
    	
    	entry.updatedStr = node.getFirstChildValue("updated");
    	entry.updated = Utilities.parseTime(entry.updatedStr);
    	
    	entry.title = node.getFirstChildValue("title");
    	entry.subtitle = node.getFirstChildValue("subtitle");
    	
    	entry.authorName = node.getFirstChildValue("gphoto:nickname");
    	entry.authorNickName = node.getFirstChildValue("gphoto:nickname");
    	entry.authorThumbnail = node.getFirstChildValue("gphoto:thumbnail");
    	
    	entry.authorThumbnail = GPhotoHelper.normalizeAvatarLocation(entry.authorThumbnail);
		
		return entry;
	}
	
    public static GAlbumEntry parseEntry(Node node) { 
    	GAlbumEntry entry = new GAlbumEntry();
    	
    	entry.updatedStr = node.getFirstChildValue("updated");
    	entry.updated = Utilities.parseDate(entry.updatedStr);
    	
    	entry.id = node.getFirstChildValue("id");
    	entry.title = node.getFirstChildValue("title");
    	entry.summary = node.getFirstChildValue("summary");
    	
    	Node author = node.getFirstChild("author");
    	if (author != null) { 
    		entry.authorName = author.getFirstChildValue("name");
    	}
    	
    	entry.albumId = node.getFirstChildValue("gphoto:id");
    	entry.name = node.getFirstChildValue("gphoto:name");
    	entry.numphotosStr = node.getFirstChildValue("gphoto:numphotos");
    	entry.numphotos = parseInt(entry.numphotosStr);
    	
    	entry.user = node.getFirstChildValue("gphoto:user");
    	entry.nickName = node.getFirstChildValue("gphoto:nickname");
    	
    	entry.commentCountStr = node.getFirstChildValue("gphoto:commentCount");
    	entry.commentCount = parseInt(entry.commentCountStr);
    	
    	entry.albumType = node.getFirstChildValue("gphoto:albumType");
    	
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
    			entry.thumbnails = new GAlbumEntry.Thumbnails[thumbnails.length];
    			
	    		for (int i=0; i < thumbnails.length; i++) { 
	    			Node thumbnail = thumbnails[i];
	    			GAlbumEntry.Thumbnails thumb = new GAlbumEntry.Thumbnails();
	    			
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
	
    static GAlbumEntry readEntry(DataInput in) throws IOException { 
    	if (in == null) return null;
    	
    	GAlbumEntry entry = new GAlbumEntry();
    	
    	entry.id = in.readUTF();
    	entry.albumId = in.readUTF();
    	
    	entry.updatedStr = in.readUTF();
    	entry.updated = new Date(in.readLong());
    	
    	entry.title = in.readUTF();
    	entry.summary = in.readUTF();
    	
    	entry.authorName = in.readUTF();
    	
    	entry.name = in.readUTF();
    	entry.numphotosStr = in.readUTF();
    	entry.numphotos = in.readInt();
    	
    	entry.user = in.readUTF();
    	entry.nickName = in.readUTF();
    	
    	entry.commentCountStr = in.readUTF();
    	entry.commentCount = in.readInt();
    	
    	entry.albumType = in.readUTF();
    	
    	entry.mediaType = in.readUTF();
    	entry.mediaUrl = in.readUTF();
    	
    	entry.mediaWidthStr = in.readUTF();
    	entry.mediaHeightStr = in.readUTF();
    	
    	entry.mediaWidth = in.readInt();
    	entry.mediaHeight = in.readInt();
    	
    	entry.mediaDescription = in.readUTF();
    	entry.mediaTitle = in.readUTF();
    	
    	int thumbSize = in.readInt();
    	entry.thumbnails = thumbSize > 0 ? new GAlbumEntry.Thumbnails[thumbSize] : null;
    	
    	for (int i=0; i < thumbSize; i++) { 
    		GAlbumEntry.Thumbnails thumb = new GAlbumEntry.Thumbnails();
    		thumb.url = in.readUTF();
    		thumb.widthStr = in.readUTF();
    		thumb.heightStr = in.readUTF();
    		thumb.width = in.readInt();
    		thumb.height = in.readInt();
    		
    		entry.thumbnails[i] = thumb;
    	}
    	
    	return entry;
    }
    
    static void writeEntry(DataOutput out, GAlbumEntry entry) throws IOException { 
    	if (out == null || entry == null) return;
    	
    	out.writeUTF(getString(entry.id));
    	out.writeUTF(getString(entry.albumId));
    	
    	out.writeUTF(getString(entry.updatedStr));
    	out.writeLong(entry.updated != null ? entry.updated.getTime() : 0);
    	
    	out.writeUTF(getString(entry.title));
    	out.writeUTF(getString(entry.summary));
    	
    	out.writeUTF(getString(entry.authorName));
    	
    	out.writeUTF(getString(entry.name));
    	out.writeUTF(getString(entry.numphotosStr));
    	out.writeInt(entry.numphotos);
    	
    	out.writeUTF(getString(entry.user));
    	out.writeUTF(getString(entry.nickName));
    	
    	out.writeUTF(getString(entry.commentCountStr));
    	out.writeInt(entry.commentCount);
    	
    	out.writeUTF(getString(entry.albumType));
    	
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
    			GAlbumEntry.Thumbnails thumb = entry.thumbnails[i];
    			out.writeUTF(getString(thumb.url));
    			out.writeUTF(getString(thumb.widthStr));
    			out.writeUTF(getString(thumb.heightStr));
    			out.writeInt(thumb.width);
    			out.writeInt(thumb.height);
    		}
    	}
    }
    
}
