package org.javenstudio.provider.app.flickr;

import org.javenstudio.common.parser.util.Node;
import org.javenstudio.provider.app.BaseEntry;

final class YGroupInfoEntry extends BaseEntry {

	public static interface FetchListener { 
		public void onGroupInfoFetching(String source);
		public void onGroupInfoFetched(YGroupInfoEntry entry);
	}
	
	public String groupId;
	public String name;
	
	public String iconserver;
	public String iconfarm;
	public String lang;
	public String ispoolmoderated;
	
	public String description;
	public String rules;
	
	public String membersStr;
	public int members;
	
	public String poolcountStr;
	public int poolcount;
	
	public String topiccountStr;
	public int topiccount;
	
	public String privacy;
	public String roles_member;
	public String roles_moderator;
	public String roles_admin;
	
	public String date_blast_addedStr;
	public long date_blast_added;
	
	public String blast_user_id;
	public String blast;
	
	public String throttle_count;
	public String throttle_mode;
	
	public String photos_ok;
	public String videos_ok;
	public String images_ok;
	public String screens_ok;
	public String art_ok;
	public String safe_ok;
	public String moderate_ok;
	public String restricted_ok;
	public String has_geo;
	
	static YGroupInfoEntry parseEntry(Node node) { 
		YGroupInfoEntry entry = new YGroupInfoEntry();
		
		entry.groupId = node.getAttribute("id");
		entry.iconserver = node.getAttribute("iconserver");
		entry.iconfarm = node.getAttribute("iconfarm");
		entry.lang = node.getAttribute("lang");
		entry.ispoolmoderated = node.getAttribute("ispoolmoderated");
		
		entry.name = node.getFirstChildValue("name");
		entry.description = node.getFirstChildValue("description");
		entry.rules = node.getFirstChildValue("rules");
		
		entry.membersStr = node.getFirstChildValue("members");
		entry.members = parseInt(entry.membersStr);
		
		entry.poolcountStr = node.getFirstChildValue("pool_count");
		entry.poolcount = parseInt(entry.poolcountStr);
		
		entry.topiccountStr = node.getFirstChildValue("topic_count");
		entry.topiccount = parseInt(entry.topiccountStr);
		
		entry.privacy = node.getFirstChildValue("privacy");
		
		Node roles = node.getFirstChild("roles");
		if (roles != null) { 
			entry.roles_member = roles.getAttribute("member");
			entry.roles_moderator = roles.getAttribute("moderator");
			entry.roles_admin = roles.getAttribute("admin");
		}
		
		Node blast = node.getFirstChild("blast");
		if (blast != null) { 
			entry.date_blast_addedStr = blast.getAttribute("date_blast_added");
			entry.blast_user_id = blast.getAttribute("user_id");
			entry.blast = blast.getValue();
			
			entry.date_blast_added = parseLong(entry.date_blast_addedStr) * 1000;
		}
		
		Node throttle = node.getFirstChild("throttle");
		if (throttle != null) { 
			entry.throttle_count = throttle.getAttribute("count");
			entry.throttle_mode = throttle.getAttribute("mode");
		}
		
		Node restrictions = node.getFirstChild("restrictions");
		if (restrictions != null) { 
			entry.photos_ok = restrictions.getAttribute("photos_ok");
			entry.videos_ok = restrictions.getAttribute("videos_ok");
			entry.images_ok = restrictions.getAttribute("images_ok");
			entry.screens_ok = restrictions.getAttribute("screens_ok");
			entry.art_ok = restrictions.getAttribute("art_ok");
			entry.safe_ok = restrictions.getAttribute("safe_ok");
			entry.moderate_ok = restrictions.getAttribute("moderate_ok");
			entry.restricted_ok = restrictions.getAttribute("restricted_ok");
			entry.has_geo = restrictions.getAttribute("has_geo");
		}
		
		return entry;
	}
	
}
