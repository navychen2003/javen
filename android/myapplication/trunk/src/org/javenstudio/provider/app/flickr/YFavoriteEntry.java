package org.javenstudio.provider.app.flickr;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YFavoriteEntry extends BaseEntry {

	public String photoId;
	public String ownerId;
	
	public String serverId;
	public String farmId;
	public String secret;
	public String title;
	
	public String ispublic;
	public String isfriend;
	public String isfamily;
	
	public String datefavedStr;
	public long datefaved;
	
	static YFavoriteEntry parseEntry(Node node) { 
		YFavoriteEntry entry = new YFavoriteEntry();
		
		entry.photoId = node.getAttribute("id");
		entry.ownerId = node.getAttribute("owner");
		
		entry.secret = node.getAttribute("secret");
		entry.serverId = node.getAttribute("server");
		entry.farmId = node.getAttribute("farm");
		entry.title = node.getAttribute("title");
		
		entry.ispublic = node.getAttribute("ispublic");
		entry.isfriend = node.getAttribute("isfriend");
		entry.isfamily = node.getAttribute("isfamily");
		
		entry.datefavedStr = node.getAttribute("date_faved");
		entry.datefaved = parseLong(entry.datefavedStr) * 1000;
		
		return entry;
	}
	
}
