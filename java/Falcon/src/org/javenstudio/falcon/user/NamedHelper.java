package org.javenstudio.falcon.user;

import org.javenstudio.common.parser.util.ParseUtils;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.message.IMessage;
import org.javenstudio.falcon.message.IMessageSet;
import org.javenstudio.falcon.setting.cluster.IHostUser;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;

public class NamedHelper {

	public static NamedMap<Object> getClientNamedMapByToken(String token, 
			int maxInvites, int maxMessages, IUser.ShowType showType) 
			throws ErrorException { 
		return toNamedMap(UserHelper.getClientByToken(token), 
				maxInvites, maxMessages, showType);
	}
	
	public static String toString(Object o) { 
		if (o == null) return "";
		if (o instanceof String) return (String)o;
		if (o instanceof CharSequence) return ((CharSequence)o).toString();
		return o.toString();
	}
	
	static NamedList<Object> getInviteInfo(IInvite item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("key", toString(item.getKey()));
			info.add("name", toString(item.getName()));
			info.add("type", toString(item.getType()));
			info.add("message", toString(item.getMessage()));
			info.add("time", item.getTime());
			
			IUser user = UserHelper.getLocalUserByName(item.getName());
			if (user != null) {
				info.add("title", toString(user.getPreference().getNickName()));
				info.add("avatar", toString(user.getPreference().getAvatar()));
			}
		}
		
		return info;
	}
	
	static NamedList<Object> getMessageInfo(IMessage item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			String subject = item.getSubject();
			if (subject == null || subject.length() == 0) {
				String body = item.getBody();
				subject = ParseUtils.extractContentFromHtml(body);
				if (subject != null && subject.length() > 100)
					subject = subject.substring(0, 100) + "...";
			}
			
			info.add("id", toString(item.getMessageId()));
			//info.add("streamid", toString(item.getStreamId()));
			//info.add("replyid", toString(item.getReplyId()));
			info.add("type", toString(item.getService().getType()));
			info.add("folder", toString(item.getFolder()));
			//info.add("folderfrom", toString(item.getFolderFrom()));
			
			info.add("from", toString(item.getFrom()));
			info.add("to", toString(item.getTo()));
			info.add("cc", toString(item.getCc()));
			//info.add("bcc", toString(item.getBcc()));
			//info.add("replyto", toString(item.getReplyTo()));
			info.add("subject", toString(subject));
			//info.add("body", toString(item.getBody()));
			info.add("ctype", toString(item.getContentType()));
			info.add("status", toString(item.getStatus()));
			info.add("flag", toString(item.getFlag()));
			
			info.add("ctime", item.getCreatedTime());
			info.add("utime", item.getUpdateTime());
			info.add("mtime", item.getMessageTime());
			
			//info.add("source", toString(item.getSourceFile()));
			
			String[] atts = item.getAttachmentFiles();
			info.add("attcount", atts != null ? atts.length : 0);
			//info.add("streamcount", stream != null ? stream.getTotalCount() : 0);
			
			//IUser user = item.getService().getManager().getUser();
			IUser userfrom = UserHelper.getLocalUserByName(item.getFrom());
			
			//info.add("user", UserInfoHandler.getUserInfo(user));
			//info.add("userfrom", UserInfoHandler.getUserInfo(userfrom));
			if (userfrom != null) {
				info.add("fromtitle", toString(userfrom.getPreference().getNickName()));
				info.add("fromavatar", toString(userfrom.getPreference().getAvatar()));
			}
		}
		
		return info;
	}
	
	static NamedMap<Object> getInvitesInfo(IInviteSet invites, int maxInvites) 
			throws ErrorException {
		NamedMap<Object> info = new NamedMap<Object>();
		
		if (invites != null) {
			info.add("count", invites.getTotalCount());
			info.add("utime", invites.getUpdateTime());
			
			IInvite[] ivts = invites.getInvites();
			if (ivts != null && maxInvites > 0) {
				NamedMap<Object> ivtsInfo = new NamedMap<Object>();
				
				for (IInvite invite : ivts) {
					if (invite != null) {
						NamedList<Object> inviteInfo = getInviteInfo(invite);
						if (inviteInfo != null) {
							ivtsInfo.add(invite.getKey(), inviteInfo);
							if (ivtsInfo.size() >= maxInvites)
								break;
						}
					}
				}
				
				info.add("invites", ivtsInfo);
			}
		} else {
			info.add("count", (long)0);
		}
		
		return info;
	}
	
	static NamedMap<Object> getMessagesInfo(IMessageSet messages, int maxMessages) 
			throws ErrorException {
		NamedMap<Object> info = new NamedMap<Object>();
		
		if (messages != null) {
			info.add("count", messages.getTotalCount());
			info.add("utime", messages.getUpdateTime());
			
			IMessage[] msgs = messages.getMessages();
			if (msgs != null && maxMessages > 0) { 
				NamedMap<Object> msgsInfo = new NamedMap<Object>();
				
				for (IMessage message : msgs) {
					if (message != null) {
						NamedList<Object> msgInfo = getMessageInfo(message);
						if (msgInfo != null) {
							msgsInfo.add(message.getMessageId(), msgInfo);
							if (msgsInfo.size() >= maxMessages)
								break;
						}
					}
				}
				
				info.add("messages", msgsInfo);
			}
		} else {
			info.add("count", (long)0);
		}
		
		return info;
	}
	
	public static NamedMap<Object> toNamedMap(IUserClient item, 
			int maxInvites, int maxMessages, IUser.ShowType showType) 
			throws ErrorException { 
		if (item == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - item.getAccessTime()) / 1000;
		
		IInviteSet invites = item.getUser().getInvites();
		IMessageSet messages = item.getUser().getMessages();
		
		if (showType == IUser.ShowType.ALL || showType == IUser.ShowType.INFO) {
			String type = IUser.Util.typeOfUser(item.getUser());
			String flag = IUser.Util.stringOfFlag(item.getUser().getUserFlag());
			
			info.add("key", toString(item.getUser().getUserKey()));
			info.add("name", toString(item.getUser().getUserName()));
			info.add("mailaddr", toString(item.getUser().getUserEmail()));
			info.add("nick", toString(item.getUser().getPreference().getNickName()));
			info.add("category", toString(item.getUser().getPreference().getCategory()));
			info.add("email", toString(item.getUser().getPreference().getEmail()));
			info.add("avatar", toString(item.getUser().getPreference().getAvatar()));
			info.add("background", toString(item.getUser().getPreference().getBackground()));
			
			if (showType == IUser.ShowType.ALL) {
				info.add("token", toString(item.getToken()));
				info.add("client", toString(item.getDeviceType().getName()));
				info.add("devicekey", toString(item.getDevice().getKey()));
				info.add("authkey", toString(item.getDevice().getAuthKey()));
			}
			
			info.add("attachhostkey", toString(item.getUser().getPreference().getAttachHostKey()));
			info.add("attachhostname", toString(item.getUser().getPreference().getAttachHostName()));
			info.add("attachuserkey", toString(item.getUser().getPreference().getAttachUserKey()));
			info.add("attachusername", toString(item.getUser().getPreference().getAttachUserName()));
			info.add("attachmailaddr", toString(item.getUser().getPreference().getAttachUserEmail()));
			
			info.add("type", toString(type));
			info.add("flag", toString(flag));
			info.add("idle", toString(Integer.toString(idleSecs) + 's'));
			info.add("rembme", item.isRememberMe());
			info.add("used", item.getUser().getUsedSpace());
			info.add("capacity", item.getUser().getCapacitySpace());
			info.add("usable", item.getUser().getUsableSpace());
			info.add("free", item.getUser().getFreeSpace());
			info.add("purchased", item.getUser().getPurchasedSpace());
			info.add("invites", getInvitesInfo(invites, maxInvites));
			info.add("messages", getMessagesInfo(messages, maxMessages));
			info.add("utime", item.getUser().getModifiedTime());
			
			//NamedMap<Object> infos = getMemberLibraryInfos(item.getUser());
			//if (infos != null)
			//	info.add("users", infos);
			
		} else {
			String flag = IUser.Util.stringOfFlag(item.getUser().getUserFlag());
			
			info.add("key", toString(item.getUser().getUserKey()));
			info.add("flag", toString(flag));
			info.add("idle", toString(Integer.toString(idleSecs) + 's'));
			info.add("usable", item.getUser().getUsableSpace());
			info.add("utime", item.getUser().getModifiedTime());
			
			if (invites != null && invites.getTotalCount() > 0)
				info.add("invites", getInvitesInfo(invites, maxInvites));
			if (messages != null && messages.getTotalCount() > 0)
				info.add("messages", getMessagesInfo(messages, maxMessages));
		}
		
		return info;
	}
	
	public static NamedMap<Object> toNamedMap(IHostUser item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		info.add("key", toString(item.getUserKey()));
		info.add("name", toString(item.getUserName()));
		info.add("mailaddr", toString(item.getUserEmail()));
		info.add("nick", toString(item.getNickName()));
		info.add("category", toString(item.getCategory()));
		//info.add("token", toString(item.getToken()));
		info.add("type", toString(item.getType()));
		info.add("flag", toString(item.getFlag()));
		info.add("idle", toString(item.getIdle()));
		info.add("used", item.getUsedSpace());
		info.add("capacity", item.getCapacitySpace());
		info.add("usable", item.getUsableSpace());
		info.add("free", item.getFreeSpace());
		info.add("purchased", item.getPurchasedSpace());
		info.add("utime", item.getModifiedTime());
		
		return info;
	}
	
	public static NamedMap<Object> toNamedMap(IMember item, 
			int maxInvites, int maxMessages, IUser.ShowType showType) 
			throws ErrorException { 
		if (item == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - item.getAccessTime()) / 1000;
		
		IInviteSet invites = item.getInvites();
		IMessageSet messages = item.getMessages();
		
		String type = IUser.Util.typeOfUser(item);
		String flag = IUser.Util.stringOfFlag(item.getUserFlag());
		
		info.add("key", toString(item.getUserKey()));
		info.add("name", toString(item.getUserName()));
		info.add("mailaddr", toString(item.getUserEmail()));
		info.add("nick", toString(item.getPreference().getNickName()));
		info.add("category", toString(item.getPreference().getCategory()));
		//info.add("token", toString(item.getToken()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("idle", toString(Integer.toString(idleSecs) + 's'));
		info.add("used", item.getUsedSpace());
		info.add("capacity", item.getCapacitySpace());
		info.add("usable", item.getUsableSpace());
		info.add("free", item.getFreeSpace());
		info.add("purchased", item.getPurchasedSpace());
		info.add("invites", getInvitesInfo(invites, maxInvites));
		info.add("messages", getMessagesInfo(messages, maxMessages));
		info.add("utime", item.getModifiedTime());
		
		if (showType == IUser.ShowType.ALL) {
			//NamedMap<Object> infos = getMemberLibraryInfos(item);
			//if (infos != null)
			//	info.add("users", infos);
		}
		
		return info;
	}
	
	public static NamedMap<Object> toNamedMap(IGroup item, 
			int maxInvites, int maxMessages, IUser.ShowType showType) 
			throws ErrorException { 
		if (item == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - item.getAccessTime()) / 1000;
		
		IInviteSet invites = item.getInvites();
		IMessageSet messages = item.getMessages();
		
		String type = IUser.Util.typeOfUser(item);
		String flag = IUser.Util.stringOfFlag(item.getUserFlag());
		
		info.add("key", toString(item.getUserKey()));
		info.add("name", toString(item.getUserName()));
		info.add("mailaddr", toString(item.getUserEmail()));
		info.add("nick", toString(item.getPreference().getNickName()));
		info.add("category", toString(item.getPreference().getCategory()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("idle", toString(Integer.toString(idleSecs) + 's'));
		info.add("used", item.getUsedSpace());
		info.add("capacity", item.getCapacitySpace());
		info.add("usable", item.getUsableSpace());
		info.add("free", item.getFreeSpace());
		info.add("purchased", item.getPurchasedSpace());
		info.add("invites", getInvitesInfo(invites, maxInvites));
		info.add("messages", getMessagesInfo(messages, maxMessages));
		info.add("utime", item.getModifiedTime());
		
		return info;
	}
	
	public static NamedMap<Object> getMemberSpaceInfos(IMember item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		if (item instanceof Member)
			((Member)item).getGroupUsedSpace(true);
		
		NamedMap<Object> selflibs = getUserSpaceInfos(item);
		if (selflibs != null) 
			info.add(item.getUserKey(), selflibs);
		
		IGroup[] groups = item.getGroups(MemberManager.ROLE_OWNER);
		if (groups != null) {
			for (IGroup group : groups) {
				NamedMap<Object> grouplibs = getUserSpaceInfos(group);
				if (group != null && grouplibs != null)
					info.add(group.getUserKey(), grouplibs);
			}
		}
		
		return info;
	}
	
	public static NamedMap<Object> getUserSpaceInfos(IUser item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		String type = IUser.Util.typeOfUser(item);
		String flag = IUser.Util.stringOfFlag(item.getUserFlag());
		
		info.add("key", toString(item.getUserKey()));
		info.add("name", toString(item.getUserName()));
		info.add("nick", toString(item.getPreference().getNickName()));
		info.add("mailaddr", toString(item.getUserEmail()));
		info.add("category", toString(item.getPreference().getCategory()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("used", item.getUsedSpace());
		info.add("capacity", item.getCapacitySpace());
		info.add("usable", item.getUsableSpace());
		info.add("free", item.getFreeSpace());
		info.add("purchased", item.getPurchasedSpace());
		
		NamedMap<Object> infos = new NamedMap<Object>();
		
		DataManager dm = item.getDataManager();
		if (dm != null) {
			ILibrary[] libraries = dm.getLibraries();
			if (libraries != null) {
				for (ILibrary library : libraries) {
					NamedMap<Object> libinfo = getLibraryInfo(library);
					if (libinfo != null && library != null)
						infos.add(library.getContentId(), libinfo);
				}
			}
		}
		
		info.add("libraries", infos);
		
		return info;
	}
	
	public static NamedMap<Object> getLibraryInfo(ILibrary item) 
			throws ErrorException { 
		if (item == null) return null;
		NamedMap<Object> info = new NamedMap<Object>();
		
		info.add("id", toString(item.getContentId()));
		info.add("name", toString(item.getName()));
		info.add("hostname", toString(item.getHostName()));
		info.add("type", toString(item.getContentType()));
		info.add("ctime", item.getCreatedTime());
		info.add("mtime", item.getModifiedTime());
		info.add("itime", item.getIndexedTime());
		info.add("totalfiles", item.getTotalFileCount());
		info.add("totalfolders", item.getTotalFolderCount());
		info.add("totallength", item.getTotalFileLength());
		
		return info;
	}
	
}
