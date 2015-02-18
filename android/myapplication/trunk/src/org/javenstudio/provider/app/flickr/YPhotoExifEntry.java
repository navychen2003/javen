package org.javenstudio.provider.app.flickr;

import java.util.ArrayList;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YPhotoExifEntry extends BaseEntry {

	public static interface FetchListener { 
		public void onPhotoExifFetching(String source);
		public void onPhotoExifFetched(YPhotoExifEntry entry);
	}
	
	public static class ExifItem { 
		public String tagspace;
		public String tagspaceid;
		public String tag;
		public String label;
		public String raw;
		public String clean;
	}
	
	public String photoId;
	public String farmId;
	public String serverId;
	public String secret;
	public String camera;
	
	public ExifItem[] exifs;
	
	static YPhotoExifEntry parseEntry(Node node) { 
		YPhotoExifEntry entry = new YPhotoExifEntry();
		
		entry.photoId = node.getAttribute("id");
		entry.secret = node.getAttribute("secret");
		entry.serverId = node.getAttribute("server");
		entry.farmId = node.getAttribute("farm");
		entry.camera = node.getAttribute("camera");
		
		ArrayList<ExifItem> items = new ArrayList<ExifItem>();
		
		for (int i=0; i < node.getChildCount(); i++) { 
			Node child = node.getChildAt(i);
			if (child == null) continue;
			
			if ("exif".equalsIgnoreCase(child.getName())) { 
				ExifItem item = parseItem(child);
				items.add(item);
			}
		}
		
		entry.exifs = items.toArray(new ExifItem[items.size()]);
		
		return entry;
	}
	
	static ExifItem parseItem(Node node) { 
		ExifItem item = new ExifItem();
		
		item.tagspace = node.getAttribute("tagspace");
		item.tagspaceid = node.getAttribute("tagspaceid");
		item.tag = node.getAttribute("tag");
		item.label = node.getAttribute("label");
		item.tagspace = node.getAttribute("tagspace");
		
		item.raw = node.getFirstChildValue("raw");
		item.clean = node.getFirstChildValue("clean");
		
		return item;
	}
	
}
