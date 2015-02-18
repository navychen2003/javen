package org.javenstudio.provider.app.flickr;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.javenstudio.cocoka.android.ResourceHelper;
import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;
import org.javenstudio.provider.publish.discuss.ITopicData;

final class YTopicEntry extends BaseEntry implements ITopicData {

	public static class TopicsInfo { 
		public String groupId;
		public String iconserver;
		public String iconfarm;
		public String name;
		public String members;
		public String privacy;
		public String lang;
		public String ispoolmoderated;
		public String total;
		public String page;
		public String per_page;
		public String pages;
	}
	
	private final FlickrUserClickListener mUserClickListener;
	private final int mIconRes;
	
	public String topicId;
	public String subject;
	public String message;
	
	public String author;
	public String authorname;
	public String iconserver;
	public String iconfarm;
	
	public String ispro;
	public String role;
	public String is_sticky;
	public String is_locked;
	public String last_reply;
	
	public String count_repliesStr;
	public int count_replies;
	
	public String can_edit;
	public String can_delete;
	public String can_reply;
	
	public String datecreateStr;
	public long datecreate;
	
	public String datelastpostStr;
	public long datelastpost;
	
	public YTopicEntry(FlickrUserClickListener listener, int iconRes) { 
		mUserClickListener = listener;
		mIconRes = iconRes;
	}
	
	public String getTopicId() { return topicId; }
	public String getTopicSubject() { return subject; }
	public String getMessage() { return message; }
	
	public String getUserId() { return author; }
	public String getUserName() { return authorname; }
	
	public int getReplyCount() { return count_replies; }
	
	public long getCreateDate() { return datecreate; }
	public long getUpdateDate() { return datelastpost; }
	
	@Override
	public String getAvatarLocation() { 
		return FlickrHelper.getIconURL(author, iconfarm, iconserver); 
	}
	
	@Override
	public Drawable getProviderIcon() { 
		if (mIconRes != 0) 
			return ResourceHelper.getResources().getDrawable(mIconRes);
		return null;
	}
	
	@Override
	public View.OnClickListener getUserClickListener() { 
		if (mUserClickListener == null)
			return null;
		
		return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mUserClickListener.onFlickrUserClick(author, authorname);
				}
			};
	}
	
	
	static TopicsInfo parseInfo(Node node) { 
		TopicsInfo entry = new TopicsInfo();
		
		entry.groupId = node.getAttribute("group_id");
		entry.iconserver = node.getAttribute("iconserver");
		entry.iconfarm = node.getAttribute("iconfarm");
		entry.name = node.getAttribute("name");
		entry.members = node.getAttribute("members");
		entry.privacy = node.getAttribute("privacy");
		entry.lang = node.getAttribute("lang");
		entry.ispoolmoderated = node.getAttribute("ispoolmoderated");
		entry.total = node.getAttribute("total");
		entry.page = node.getAttribute("page");
		entry.per_page = node.getAttribute("per_page");
		entry.pages = node.getAttribute("pages");
		
		return entry;
	}
	
	static YTopicEntry parseEntry(Node node, FlickrUserClickListener listener, int iconRes) { 
		YTopicEntry entry = new YTopicEntry(listener, iconRes);
		
		entry.topicId = node.getAttribute("id");
		entry.subject = node.getAttribute("subject");
		entry.message = node.getFirstChildValue("message");
		
		entry.author = node.getAttribute("author");
		entry.authorname = node.getAttribute("authorname");
		entry.iconserver = node.getAttribute("iconserver");
		entry.iconfarm = node.getAttribute("iconfarm");
		
		entry.ispro = node.getAttribute("ispro");
		entry.role = node.getAttribute("role");
		entry.is_sticky = node.getAttribute("is_sticky");
		entry.is_locked = node.getAttribute("is_locked");
		entry.last_reply = node.getAttribute("last_reply");
		
		entry.can_edit = node.getAttribute("can_edit");
		entry.can_delete = node.getAttribute("can_delete");
		entry.can_reply = node.getAttribute("can_reply");
		
		entry.count_repliesStr = node.getAttribute("count_replies");
		entry.count_replies = parseInt(entry.count_repliesStr);
		
		entry.datecreateStr = node.getAttribute("datecreate");
		entry.datecreate = parseLong(entry.datecreateStr) * 1000;
		
		entry.datelastpostStr = node.getAttribute("datelastpost");
		entry.datelastpost = parseLong(entry.datelastpostStr) * 1000;
		
		return entry;
	}
	
}
