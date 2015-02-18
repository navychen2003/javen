package org.javenstudio.provider.app.flickr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YPhotoCommentEntry extends BaseEntry {
	//private static final Logger LOG = Logger.getLogger(YPhotoCommentEntry.class);

	public static interface FetchListener { 
		public void onPhotoCommentFetching(String source);
		public void onPhotoCommentFetched(YPhotoCommentEntry entry);
		public YCommentItem createCommentItem();
	}
	
	public String photoId;
	
	public YCommentItem[] comments;
	
	static YPhotoCommentEntry parseEntry(Node node, FetchListener listener) { 
		YPhotoCommentEntry entry = new YPhotoCommentEntry();
		
		entry.photoId = node.getAttribute("photo_id");
		
		ArrayList<YCommentItem> items = new ArrayList<YCommentItem>();
		
		for (int i=0; i < node.getChildCount(); i++) { 
			Node child = node.getChildAt(i);
			if (child == null) continue;
			
			if ("comment".equalsIgnoreCase(child.getName())) { 
				YCommentItem item = parseItem(child, listener);
				items.add(item);
			}
		}
		
		YCommentItem[] comments = items.toArray(new YCommentItem[items.size()]);
    	if (comments != null) { 
    		Arrays.sort(comments, new Comparator<YCommentItem>() {
				@Override
				public int compare(YCommentItem lhs, YCommentItem rhs) {
					if (lhs != null || rhs != null) { 
						if (lhs == null) return -1;
						if (rhs == null) return 1;
						return lhs.datecreate > rhs.datecreate ? -1 : 
							(lhs.datecreate < rhs.datecreate ? 1 : 0);
					}
					return 0;
				}
    		});
    	}
		entry.comments = comments;
		
		return entry;
	}
	
	static YCommentItem parseItem(Node node, FetchListener listener) { 
		YCommentItem item = listener.createCommentItem();
		
		item.commentId = node.getAttribute("id");
		item.author = node.getAttribute("author");
		item.authorName = node.getAttribute("authorname");
		item.iconserver = node.getAttribute("iconserver");
		item.iconfarm = node.getAttribute("iconfarm");
		
		item.datecreateStr = node.getAttribute("datecreate");
		item.datecreate = parseLong(item.datecreateStr) * 1000;
		
		item.content = node.getValue();
		
		return item;
	}
	
}
