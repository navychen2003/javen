package org.javenstudio.provider.app.picasa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.cocoka.util.Utilities;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class GPhotoCommentEntry extends BaseEntry {
	//private static final Logger LOG = Logger.getLogger(GPhotoCommentEntry.class);

	public static interface FetchListener { 
		public void onPhotoCommentFetching(String source);
		public void onPhotoCommentFetched(GPhotoCommentEntry entry);
		public GCommentItem createCommentItem();
	}
	
	public String totalResultsStr;
	public int totalResults;
	
	public String startIndexStr;
	public int startIndex;
	
	public String itemsPerPageStr;
	public int itemsPerPage;
	
	public String id;
	public String photoId;
	public String albumId;
	
	public String updatedStr;
	public long updated;
	
	public String title;
	public String subtitle;
	
	public String version;
	
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
	
	public String mediaType;
	public String mediaUrl;
	
	public String mediaWidthStr;
	public String mediaHeightStr;
	
	public int mediaWidth;
	public int mediaHeight;
	
	public String mediaDescription;
	public String mediaTitle;
	
	public GCommentItem[] comments;
	
    public static GPhotoCommentEntry parseEntry(Node node, FetchListener listener) { 
    	GPhotoCommentEntry entry = new GPhotoCommentEntry();
    	
    	entry.totalResultsStr = node.getFirstChildValue("openSearch:totalResults");
    	entry.totalResults = parseInt(entry.totalResultsStr);
    	
    	entry.startIndexStr = node.getFirstChildValue("openSearch:startIndex");
    	entry.startIndex = parseInt(entry.startIndexStr);
    	
    	entry.itemsPerPageStr = node.getFirstChildValue("openSearch:itemsPerPage");
    	entry.itemsPerPage = parseInt(entry.itemsPerPageStr);
    	
    	entry.id = node.getFirstChildValue("id");
    	entry.photoId = node.getFirstChildValue("gphoto:id");
    	entry.albumId = node.getFirstChildValue("gphoto:albumid");
    	
    	entry.updatedStr = node.getFirstChildValue("updated");
    	entry.updated = Utilities.parseTime(entry.updatedStr);
    	
    	entry.title = node.getFirstChildValue("title");
    	entry.subtitle = node.getFirstChildValue("subtitle");
    	
    	entry.version = node.getFirstChildValue("gphoto:version");
    	
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
    	}
    	
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
