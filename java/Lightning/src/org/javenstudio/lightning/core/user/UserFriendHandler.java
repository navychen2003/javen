package org.javenstudio.lightning.core.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.Friend;
import org.javenstudio.falcon.user.profile.FriendGroup;
import org.javenstudio.falcon.user.profile.FriendManager;
import org.javenstudio.falcon.user.profile.Invite;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserFriendHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserFriendHandler(core);
	}
	
	public UserFriendHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		String username = trim(req.getParam("username"));
		
		if (action == null || action.length() == 0)
			action = "list";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("invite")) { 
			Invite invite = inviteUser(user, username);
			rsp.add("user", getInviteInfo(invite));
			
		} else if (action.equalsIgnoreCase("accept")) {
			Friend friend = acceptUser(user, username);
			rsp.add("user", getFriendInfo(friend));
			
		} else if (action.equalsIgnoreCase("reject")) {
			Invite invite = rejectUser(user, username);
			rsp.add("user", getInviteInfo(invite));
			
		} else if (action.equalsIgnoreCase("cancel")) {
			Invite invite = cancelUser(user, username);
			rsp.add("user", getInviteInfo(invite));
			
		} else if (action.equalsIgnoreCase("delete")) {
			Friend friend = deleteUser(user, username);
			rsp.add("user", getFriendInfo(friend));
			
		} else if (action.equalsIgnoreCase("list")) { 
			handleList(req, rsp, user);
			
		} else if (action.equalsIgnoreCase("select")) { 
			handleSelect(req, rsp, user);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleList(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		NamedList<Object> inviteList = null;
		NamedList<Object> friendList = null;
		
		FriendManager manager = user.getFriendManager();
		if (manager != null) { 
			manager.loadFriends(false);
			
			inviteList = getInviteList(manager);
			friendList = getFriendList(manager);
		}
		
		if (inviteList == null) inviteList = new NamedMap<Object>();
		if (friendList == null) friendList = new NamedMap<Object>();
		
		rsp.add("invites", inviteList);
		rsp.add("friends", friendList);
	}
	
	private void handleSelect(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		String prefix = trim(req.getParam("prefix"));
		NamedList<Object> friendList = null;
		
		FriendManager manager = user.getFriendManager();
		if (manager != null) { 
			manager.loadFriends(false);
			
			friendList = selectFriends(manager, prefix);
		}
		
		if (friendList == null) friendList = new NamedMap<Object>();
		
		rsp.add("friends", friendList);
	}
	
	private NamedList<Object> selectFriends(FriendManager manager, 
			String prefix) throws ErrorException { 
		FriendGroup[] groups = manager.getFriendGroups();
		
		NamedList<Object> list = new NamedMap<Object>();
		ArrayList<Friend> others = new ArrayList<Friend>();
		
		for (int i=0; groups != null && i < groups.length; i++) { 
			FriendGroup group = groups[i];
			if (group == null) continue;
			
			Friend[] friends = group.getFriends();
			
			for (int j=0; friends != null && j < friends.length; j++) { 
				Friend friend = friends[j];
				if (friend == null) continue;
				
				if (prefix != null && prefix.length() > 0 && friend.getName().startsWith(prefix)) {
					NamedList<Object> info = getFriendInfo(friend);
					if (info != null) list.add(friend.getKey(), info);
				} else { 
					others.add(friend);
				}
			}
		}
		
		for (Friend friend : others) { 
			if (friend == null) continue;
			
			NamedList<Object> info = getFriendInfo(friend);
			if (info != null) list.add(friend.getKey(), info);
		}
		
		return list;
	}
	
	private NamedList<Object> getFriendList(FriendManager manager) 
			throws ErrorException { 
		FriendGroup[] groups = manager.getFriendGroups();
		NamedList<Object> list = new NamedMap<Object>();
		
		for (int i=0; groups != null && i < groups.length; i++) { 
			FriendGroup group = groups[i];
			if (group == null) continue;
			
			Friend[] friends = group.getFriends();
			ArrayList<NamedList<Object>> friendList = new ArrayList<NamedList<Object>>();
			
			for (int j=0; friends != null && j < friends.length; j++) { 
				Friend friend = friends[j];
				if (friend == null) continue;
				
				NamedList<Object> info = getFriendInfo(friend);
				if (info != null) friendList.add(info);
			}
			
			Object[] friendItems = friendList.toArray(new Object[friendList.size()]);
			list.add(group.getFriendType(), friendItems);
		}
		
		return list;
	}
	
	static NamedList<Object> getInviteList(FriendManager manager) 
			throws ErrorException { 
		Invite[] invites = manager.getInvites();
		return getInviteList(invites);
	}
	
	static NamedList<Object> getInviteList(Invite[] invites) 
			throws ErrorException { 
		NamedList<Object> list = new NamedMap<Object>();
		
		if (invites != null) { 
			Arrays.sort(invites, new Comparator<Invite>() {
					@Override
					public int compare(Invite o1, Invite o2) {
						long tm1 = o1.getTime();
						long tm2 = o2.getTime();
						return tm1 > tm2 ? -1 : (tm1 < tm2 ? 1 : 0);
					}
				});
		}
		
		for (int i=0; invites != null && i < invites.length; i++) { 
			Invite invite = invites[i];
			if (invite == null) continue;
			
			NamedList<Object> info = getInviteInfo(invite);
			if (info != null) 
				list.add(invite.getKey(), info);
		}
		
		return list;
	}
	
	private Friend acceptUser(IMember user, String username) 
			throws ErrorException { 
		if (username == null || username.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username or Email is empty");
		}
		
		FriendManager manager = user.getFriendManager();
		if (manager != null) { 
			manager.loadFriends(false);
			
			IMember friendUser = (IMember)getManager().getOrCreate(username);
			
			if (friendUser != null) {
				FriendManager fmanager = friendUser.getFriendManager();
				if (fmanager != null) {
					fmanager.loadFriends(false);
					
					Friend friend = new Friend(friendUser.getUserKey(), username);
					Friend ffriend = new Friend(user.getUserKey(), user.getUserName());
					
					fmanager.removeInvite(user.getUserKey());
					fmanager.addFriend(ffriend, Friend.TYPE_FRIEND);
					fmanager.saveFriends();
					
					manager.removeInvite(friendUser.getUserKey());
					manager.addFriend(friend, Friend.TYPE_FRIEND);
					manager.saveFriends();
					
					String text = Strings.get(user.getPreference().getLanguage(), "Accepted new friend \"%1$s\"");
					MessageHelper.notifySys(user.getUserKey(), String.format(text, username));
					
					text = Strings.get(friendUser.getPreference().getLanguage(), "\"%1$s\" accepted your invite");
					MessageHelper.notifySys(friendUser.getUserKey(), String.format(text, user.getUserName()));
					
					return friend;
				}
			}
		}
		
		return null;
	}
	
	private Invite rejectUser(IMember user, String username) 
			throws ErrorException { 
		if (username == null || username.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username or Email is empty");
		}
		
		FriendManager manager = user.getFriendManager();
		if (manager != null) { 
			manager.loadFriends(false);
			
			IMember friendUser = (IMember)getManager().getOrCreate(username);
			
			if (friendUser != null) {
				FriendManager fmanager = friendUser.getFriendManager();
				if (fmanager != null) {
					fmanager.loadFriends(false);
					
					Invite invite = new Invite(user.getUserKey(), user.getUserName(), Invite.TYPE_OUT);
					invite.setMessage("Rejected");
					
					fmanager.addInvite(invite);
					fmanager.saveFriends();
					
					manager.removeInvite(friendUser.getUserKey());
					manager.saveFriends();
					
					String text = Strings.get(user.getPreference().getLanguage(), "Rejected invite from \"%1$s\"");
					MessageHelper.notifySys(user.getUserKey(), String.format(text, username));
					
					text = Strings.get(friendUser.getPreference().getLanguage(), "\"%1$s\" rejected your invite");
					MessageHelper.notifySys(friendUser.getUserKey(), String.format(text, user.getUserName()));
					
					return invite;
				}
			}
		}
		
		return null;
	}
	
	private Invite cancelUser(IMember user, String username) 
			throws ErrorException { 
		if (username == null || username.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username or Email is empty");
		}
		
		FriendManager manager = user.getFriendManager();
		if (manager != null) { 
			manager.loadFriends(false);
			
			IMember friendUser = (IMember)getManager().getOrCreate(username);
			
			if (friendUser != null) {
				FriendManager fmanager = friendUser.getFriendManager();
				if (fmanager != null) {
					fmanager.loadFriends(false);
					
					Invite invite = new Invite(friendUser.getUserKey(), friendUser.getUserName(), Invite.TYPE_IN);
					invite.setMessage("Canceled");
					
					fmanager.removeInvite(user.getUserKey());
					fmanager.saveFriends();
					
					manager.removeInvite(friendUser.getUserKey());
					manager.saveFriends();
					
					return invite;
				}
			}
		}
		
		return null;
	}
	
	private Friend deleteUser(IMember user, String username) 
			throws ErrorException { 
		if (username == null || username.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username or Email is empty");
		}
		
		FriendManager manager = user.getFriendManager();
		if (manager != null) { 
			manager.loadFriends(false);
			
			IMember friendUser = (IMember)getManager().getOrCreate(username);
			
			if (friendUser != null) {
				FriendManager fmanager = friendUser.getFriendManager();
				if (fmanager != null) {
					fmanager.loadFriends(false);
					
					@SuppressWarnings("unused")
					Friend ffriend = fmanager.removeFriend(user.getUserKey());
					Friend friend = manager.removeFriend(friendUser.getUserKey());
					
					fmanager.saveFriends();
					manager.saveFriends();
					
					String text = Strings.get(user.getPreference().getLanguage(), "Removed friend \"%1$s\"");
					MessageHelper.notifySys(user.getUserKey(), String.format(text, username));
					
					text = Strings.get(friendUser.getPreference().getLanguage(), "\"%1$s\" removed you as friend");
					MessageHelper.notifySys(friendUser.getUserKey(), String.format(text, user.getUserName()));
					
					return friend;
				}
			}
		}
		
		return null;
	}
	
	private Invite inviteUser(IMember user, String username) 
			throws ErrorException { 
		if (username == null || username.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username or Email is empty");
		}
		
		if (username.equals(user.getUserName())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Cannot invite yourself");
		}
		
		if (getManager().getService().searchUser(username) == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + username + " not found");
		}
		
		FriendManager manager = user.getFriendManager();
		if (manager != null) { 
			manager.loadFriends(false);
			
			IMember friendUser = (IMember)getManager().getOrCreate(username);
			
			if (friendUser != null) {
				if (manager.getFriend(friendUser.getUserKey()) != null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"User: " + username + " already is your friend");
				}
				
				FriendManager fmanager = friendUser.getFriendManager();
				if (fmanager != null) {
					fmanager.loadFriends(false);
					
					Invite in = manager.getInvite(friendUser.getUserKey());
					if (in != null && Invite.TYPE_IN.equals(in.getType())) {
						Friend friend = new Friend(friendUser.getUserKey(), username);
						Friend ffriend = new Friend(user.getUserKey(), user.getUserName());
						
						fmanager.removeInvite(user.getUserKey());
						fmanager.addFriend(ffriend, Friend.TYPE_FRIEND);
						fmanager.saveFriends();
						
						manager.removeInvite(friendUser.getUserKey());
						manager.addFriend(friend, Friend.TYPE_FRIEND);
						manager.saveFriends();
						
						String text = Strings.get(user.getPreference().getLanguage(), "Accepted new friend \"%1$s\"");
						MessageHelper.notifySys(user.getUserKey(), String.format(text, username));
						
						text = Strings.get(friendUser.getPreference().getLanguage(), "\"%1$s\" accepted your invite");
						MessageHelper.notifySys(friendUser.getUserKey(), String.format(text, user.getUserName()));
						
						return in;
					} else {
						Invite invite = new Invite(user.getUserKey(), user.getUserName(), Invite.TYPE_IN);
						Invite request = new Invite(friendUser.getUserKey(), friendUser.getUserName(), Invite.TYPE_OUT);
						
						fmanager.addInvite(invite);
						fmanager.saveFriends();
						
						manager.addInvite(request);
						manager.saveFriends();
						
						return request;
					}
				}
			}
		}
		
		return null;
	}
	
	static NamedList<Object> getInviteInfo(Invite item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			String type = "";
			String flag = "";
			String avatar = "";
			String title = "";
			
			int idleSecs = 0;
			
			IUser user = UserHelper.getLocalUserByName(item.getName());
			if (user != null) { 
				avatar = user.getPreference().getAvatar();
				title = user.getPreference().getNickName();
				
				type = IUser.Util.typeOfUser(user);
				flag = IUser.Util.stringOfFlag(user.getUserFlag());
				
				long now = System.currentTimeMillis();
				idleSecs = (int)(now - user.getAccessTime()) / 1000;
			}
			
			info.add("key", toString(item.getKey()));
			info.add("name", toString(item.getName()));
			info.add("type", toString(item.getType()));
			info.add("utype", toString(type));
			info.add("flag", toString(flag));
			info.add("idle", toString(Integer.toString(idleSecs) + 's'));
			info.add("avatar", toString(avatar));
			info.add("title", toString(title));
			info.add("message", toString(item.getMessage()));
			info.add("time", item.getTime());
		}
		
		return info;
	}
	
	static NamedList<Object> getFriendInfo(Friend item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			String type = "";
			String flag = "";
			String avatar = "";
			String status = "";
			String category = "";
			String title = item.getTitle();
			
			int idleSecs = 0;
			
			IUser user = UserHelper.getLocalUserByName(item.getName());
			if (user != null) { 
				type = IUser.Util.typeOfUser(user);
				flag = IUser.Util.stringOfFlag(user.getUserFlag());
				
				long now = System.currentTimeMillis();
				idleSecs = (int)(now - user.getAccessTime()) / 1000;
				
				avatar = user.getPreference().getAvatar();
				status = user.getPreference().getStatus();
				category = user.getPreference().getCategory();
				
				if (title == null || title.length() == 0)
					title = user.getPreference().getNickName();
			}
			
			info.add("key", toString(item.getKey()));
			info.add("name", toString(item.getName()));
			info.add("type", toString(type));
			info.add("flag", toString(flag));
			info.add("idle", toString(Integer.toString(idleSecs) + 's'));
			info.add("title", toString(title));
			info.add("avatar", toString(avatar));
			info.add("status", toString(status));
			info.add("category", toString(category));
		}
		
		return info;
	}
	
}
