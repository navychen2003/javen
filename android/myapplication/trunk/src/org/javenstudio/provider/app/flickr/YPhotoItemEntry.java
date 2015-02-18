package org.javenstudio.provider.app.flickr;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YPhotoItemEntry extends BaseEntry {

	public static class PhotoSetInfo { 
		public String photosetId;
		public String primary;
		public String ownerId;
		public String ownername;
		
		public int page;
		public int perpage;
		public int pages;
		public int total;
	}
	
	public String photoId;
	public String serverId;
	public String farmId;
	
	public String secret;
	public String title;
	
	public String isfamily;
	
	
	static PhotoSetInfo parseInfo(Node node) { 
		PhotoSetInfo info = new PhotoSetInfo();
		
		info.photosetId = node.getAttribute("id");
		info.primary = node.getAttribute("primary");
		info.ownerId = node.getAttribute("owner");
		info.ownername = node.getAttribute("ownername");
		
		info.page = parseInt(node.getAttribute("page"));
		info.perpage = parseInt(node.getAttribute("perpage"));
		info.pages = parseInt(node.getAttribute("pages"));
		info.total = parseInt(node.getAttribute("total"));
		
		return info;
	}
	
	static YPhotoItemEntry parseEntry(Node node) { 
		YPhotoItemEntry entry = new YPhotoItemEntry();
		
		entry.photoId = node.getAttribute("id");
		entry.secret = node.getAttribute("secret");
		entry.serverId = node.getAttribute("server");
		entry.farmId = node.getAttribute("farm");
		entry.title = node.getAttribute("title");
		entry.isfamily = node.getAttribute("isfamily");
		
		return entry;
	}
	
	static YPhotoItemEntry readEntry(DataInput in) throws IOException { 
		if (in == null) return null;
		
		YPhotoItemEntry entry = new YPhotoItemEntry();
		
		entry.photoId = in.readUTF();
		entry.serverId = in.readUTF();
		entry.farmId = in.readUTF();
		
		entry.secret = in.readUTF();
		entry.title = in.readUTF();
		
		entry.isfamily = in.readUTF();
		
		return entry;
	}
	
	static void writeEntry(DataOutput out, YPhotoItemEntry entry) throws IOException { 
		if (out == null || entry == null) return;
		
		out.writeUTF(getString(entry.photoId));
		out.writeUTF(getString(entry.serverId));
		out.writeUTF(getString(entry.farmId));
		
		out.writeUTF(getString(entry.secret));
		out.writeUTF(getString(entry.title));
		
		out.writeUTF(getString(entry.isfamily));
	}
	
}
