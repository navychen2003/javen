package org.javenstudio.provider.app.flickr;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YPhotoEntry extends BaseEntry {

	public static class ResultInfo { 
		public int page;
		public int pages;
		public int perpage;
		public int total;
	}
	
	public String photoId;
	public String ownerId;
	public String serverId;
	public String farmId;
	
	public String secret;
	public String title;
	
	public String isfamily;
	public String isfriend;
	public String ispublic;
	
	
	static ResultInfo parseInfo(Node node) { 
		ResultInfo entry = new ResultInfo();
		
		entry.page = parseInt(node.getAttribute("page"));
		entry.pages = parseInt(node.getAttribute("pages"));
		entry.perpage = parseInt(node.getAttribute("perpage"));
		entry.total = parseInt(node.getAttribute("total"));
		
		return entry;
	}
	
	static YPhotoEntry parseEntry(Node node) { 
		YPhotoEntry entry = new YPhotoEntry();
		
		entry.photoId = node.getAttribute("id");
		entry.ownerId = node.getAttribute("owner");
		entry.secret = node.getAttribute("secret");
		entry.serverId = node.getAttribute("server");
		entry.farmId = node.getAttribute("farm");
		entry.title = node.getAttribute("title");
		entry.isfamily = node.getAttribute("isfamily");
		entry.isfriend = node.getAttribute("isfriend");
		entry.ispublic = node.getAttribute("ispublic");
		
		return entry;
	}
	
	static YPhotoEntry readEntry(DataInput in) throws IOException { 
		if (in == null) return null;
		
		YPhotoEntry entry = new YPhotoEntry();
		
		entry.photoId = in.readUTF();
		entry.ownerId = in.readUTF();
		entry.serverId = in.readUTF();
		entry.farmId = in.readUTF();
		
		entry.secret = in.readUTF();
		entry.title = in.readUTF();
		
		entry.isfamily = in.readUTF();
		entry.isfriend = in.readUTF();
		entry.ispublic = in.readUTF();
		
		return entry;
	}
	
	static void writeEntry(DataOutput out, YPhotoEntry entry) throws IOException { 
		if (out == null || entry == null) return;
		
		out.writeUTF(getString(entry.photoId));
		out.writeUTF(getString(entry.ownerId));
		out.writeUTF(getString(entry.serverId));
		out.writeUTF(getString(entry.farmId));
		
		out.writeUTF(getString(entry.secret));
		out.writeUTF(getString(entry.title));
		
		out.writeUTF(getString(entry.isfamily));
		out.writeUTF(getString(entry.isfriend));
		out.writeUTF(getString(entry.ispublic));
	}
	
}
