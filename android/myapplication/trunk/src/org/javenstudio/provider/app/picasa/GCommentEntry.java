package org.javenstudio.provider.app.picasa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final public class GCommentEntry extends BaseEntry {
	//private static final Logger LOG = Logger.getLogger(GCommentEntry.class);

	public static interface FetchListener { 
		public void onCommentFetching(String source);
		public void onCommentFetched(GCommentEntry entry);
		public GCommentItem createCommentItem();
	}
	
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
	
	public GCommentItem[] comments;
	
    public static GCommentEntry parseEntry(Node node, FetchListener listener) { 
    	GCommentEntry entry = new GCommentEntry();
    	
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
    	
    	Node author = node.getFirstChild("author");
    	if (author != null) { 
    		entry.authorName = author.getFirstChildValue("name");
    	}
    	
    	entry.authorNickName = node.getFirstChildValue("gphoto:nickname");
    	entry.authorThumbnail = node.getFirstChildValue("gphoto:thumbnail");
    	
    	ArrayList<GCommentItem> items = new ArrayList<GCommentItem>();
    	for (int i=0; i < node.getChildCount(); i++) { 
    		Node child = node.getChildAt(i);
    		if (child != null && "entry".equalsIgnoreCase(child.getName())) { 
    			GCommentItem item = parseItem(child, listener);
    			items.add(item);
    		}
    	}
    	
    	GCommentItem[] comments = items.toArray(new GCommentItem[items.size()]);
    	if (comments != null) { 
    		Arrays.sort(comments, new Comparator<GCommentItem>() {
				@Override
				public int compare(GCommentItem lhs, GCommentItem rhs) {
					if (lhs != null || rhs != null) { 
						if (lhs == null) return -1;
						if (rhs == null) return 1;
						return lhs.updated > rhs.updated ? -1 : 
							(lhs.updated < rhs.updated ? 1 : 0);
					}
					return 0;
				}
    		});
    	}
    	entry.comments = comments;
    	
    	return entry;
    }
	
    static GCommentItem parseItem(Node node, FetchListener listener) { 
    	GCommentItem item = listener.createCommentItem();
    	
    	item.id = node.getFirstChildValue("id");
    	
    	item.updatedStr = node.getFirstChildValue("updated");
    	item.updated = Utilities.parseTime(item.updatedStr);
    	
    	item.title = node.getFirstChildValue("title");
    	item.content = node.getFirstChildValue("content");
    	
    	Node author = node.getFirstChild("author");
    	if (author != null) { 
    		item.authorName = author.getFirstChildValue("name");
    		item.authorNickName = author.getFirstChildValue("gphoto:nickname");
    		item.authorThumbnail = author.getFirstChildValue("gphoto:thumbnail");
    		item.authorUser = author.getFirstChildValue("gphoto:user");
    		
    		item.authorThumbnail = GPhotoHelper.normalizeAvatarLocation(item.authorThumbnail);
    	}
    	
    	item.photoId = node.getFirstChildValue("gphoto:photoid");
    	
    	return item;
    }
    
}
