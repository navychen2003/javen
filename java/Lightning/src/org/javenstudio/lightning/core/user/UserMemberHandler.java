package org.javenstudio.lightning.core.user;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.GroupManager;
import org.javenstudio.falcon.user.profile.Invite;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserMemberHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserMemberHandler(core);
	}
	
	public UserMemberHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "list";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("join")) { 
			handleJoin(req, rsp, user);
		} else if (action.equalsIgnoreCase("invite")) { 
			handleInvite(req, rsp, user);
		} else if (action.equalsIgnoreCase("leave")) { 
			handleLeave(req, rsp, user);
		} else if (action.equalsIgnoreCase("accept")) { 
			handleAccept(req, rsp, user);
		} else if (action.equalsIgnoreCase("reject")) { 
			handleReject(req, rsp, user);
		} else if (action.equalsIgnoreCase("cancel")) { 
			handleCancel(req, rsp, user);
		} else if (action.equalsIgnoreCase("delete")) { 
			handleDelete(req, rsp, user);
		} else if (action.equalsIgnoreCase("list")) { 
			handleList(req, rsp, user);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleList(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		
		NamedList<Object> inviteList = null;
		NamedList<Object> memberList = null;
		
		NamedList<Object> groupInfo = null;
		NamedList<Object> userInfo = null;
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		if (grp != null && grp instanceof IGroup) {
			IGroup group = (IGroup)grp;
			MemberManager manager = group.getMemberManager();
			if (manager != null) { 
				manager.loadMembers(false);
				
				MemberManager.GroupMember gm = manager.getMember(user.getUserKey());
				boolean isManager = false;
				if (gm != null) { 
					if (MemberManager.ROLE_OWNER.equals(gm.getRole()) || 
						MemberManager.ROLE_MANAGER.equals(gm.getRole()))
						isManager = true;
				}
				
				inviteList = isManager ? getInviteList(manager) : null;
				memberList = getMemberList(manager);
				
				userInfo = UserInfoHandler.getUserInfo(gm, user);
			}
			
			groupInfo = UserInfoHandler.getGroupInfo(group);
		}
		
		if (inviteList == null) inviteList = new NamedMap<Object>();
		if (memberList == null) memberList = new NamedMap<Object>();
		
		if (groupInfo == null) groupInfo = new NamedMap<Object>();
		if (userInfo == null) userInfo = new NamedMap<Object>();
		
		rsp.add("user", userInfo);
		rsp.add("group", groupInfo);
		
		rsp.add("invites", inviteList);
		rsp.add("members", memberList);
	}
	
	private void handleJoin(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		String username = trim(req.getParam("username"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		if (!username.equals(user.getUserName())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username must be yourself");
		}
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		if (grp != null && grp instanceof IGroup) {
			IGroup group = (IGroup)grp;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				if (mm.getMember(user.getUserKey()) != null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"You already joined group: " + groupname);
				}
				
				GroupManager gm = user.getGroupManager();
				if (gm != null) { 
					gm.loadGroups(false);
					
					Invite in = gm.getInvite(group.getUserKey());
					if (in != null && Invite.TYPE_IN.equals(in.getType())) { 
						mm.removeInvite(user.getUserKey());
						MemberManager.GroupMember usergm = mm.addMember(user.getUserKey(), user.getUserName(), MemberManager.ROLE_MEMBER);
						mm.saveMembers();
						
						gm.removeInvite(group.getUserKey());
						gm.addGroup(group.getUserKey(), group.getUserName(), MemberManager.ROLE_MEMBER);
						gm.saveGroups();
						
						String text = Strings.get(user.getPreference().getLanguage(), "Accepted to join \"%1$s\"");
						MessageHelper.notifySys(user.getUserKey(), String.format(text, group.getUserName()));
						
						rsp.add("member", UserInfoHandler.getUserInfo(usergm, user));
						
					} else {
						Invite invite = new Invite(user.getUserKey(), user.getUserName(), Invite.TYPE_IN);
						Invite request = new Invite(group.getUserKey(), group.getUserName(), Invite.TYPE_OUT);
						
						gm.addInvite(request);
						gm.saveGroups();
						
						mm.addInvite(invite);
						mm.saveMembers();
						
						rsp.add("group", getInviteInfo(request));
					}
				}
			}
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Group: " + groupname + " not found");
		}
	}
	
	private void handleInvite(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		String username = trim(req.getParam("username"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		if (username.equals(me.getUserName())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be yourself");
		}
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		IUser usr = UserHelper.getLocalUserByName(username);
		
		if (grp != null && grp instanceof IGroup && usr != null && usr instanceof IMember) {
			IGroup group = (IGroup)grp;
			IMember user = (IMember)usr;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				MemberManager.GroupMember megm = mm.getMember(me.getUserKey());
				if (megm == null || (!MemberManager.ROLE_OWNER.equals(megm.getRole()) && 
					!MemberManager.ROLE_MANAGER.equals(megm.getRole()))) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"You are not manager of group: " + groupname);
				}
				
				if (mm.getMember(user.getUserKey()) != null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"User: " + username + " already joined group: " + groupname);
				}
				
				Invite invite = new Invite(user.getUserKey(), user.getUserName(), Invite.TYPE_OUT);
				Invite request = new Invite(group.getUserKey(), group.getUserName(), Invite.TYPE_IN);
				
				GroupManager gm = user.getGroupManager();
				if (gm != null) { 
					gm.loadGroups(false);
					
					gm.addInvite(request);
					gm.saveGroups();
					
					mm.addInvite(invite);
					mm.saveMembers();
					
					rsp.add("group", getInviteInfo(request));
				}
			}
		} else { 
			if (grp == null || !(grp instanceof IGroup)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Group: " + groupname + " not found");
			} else if (usr == null || !(usr instanceof IMember)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"User: " + username + " not found");
			}
		}
	}
	
	private void handleCancel(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		String username = trim(req.getParam("username"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		IUser usr = UserHelper.getLocalUserByName(username);
		
		if (grp != null && grp instanceof IGroup && usr != null && usr instanceof IMember) {
			IGroup group = (IGroup)grp;
			IMember user = (IMember)usr;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				if (!username.equals(me.getUserName())) { 
					MemberManager.GroupMember megm = mm.getMember(me.getUserKey());
					if (megm == null || (!MemberManager.ROLE_OWNER.equals(megm.getRole()) && 
						!MemberManager.ROLE_MANAGER.equals(megm.getRole()))) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"You are not manager of group: " + groupname);
					}
				}
				
				if (mm.getMember(user.getUserKey()) != null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"User: " + username + " already joined group: " + groupname);
				}
				
				GroupManager gm = user.getGroupManager();
				if (gm != null) { 
					gm.loadGroups(false);
					
					Invite invite = gm.removeInvite(group.getUserKey());
					gm.saveGroups();
					
					mm.removeInvite(user.getUserKey());
					mm.saveMembers();
					
					rsp.add("group", getInviteInfo(invite));
				}
			}
		} else { 
			if (grp == null || !(grp instanceof IGroup)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Group: " + groupname + " not found");
			} else if (usr == null || !(usr instanceof IMember)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"User: " + username + " not found");
			}
		}
	}
	
	private void handleLeave(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (req == null || rsp == null || user == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		String username = trim(req.getParam("username"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		if (!username.equals(user.getUserName())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username must be yourself");
		}
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		if (grp != null && grp instanceof IGroup) {
			IGroup group = (IGroup)grp;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				MemberManager.GroupMember member = mm.getMember(user.getUserKey());
				if (member == null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"You aren't a member of group: " + groupname);
				}
				
				if (MemberManager.ROLE_OWNER.equals(member.getRole())) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"You are the owner of group: " + groupname + ", cannot leave.");
				}
				
				GroupManager gm = user.getGroupManager();
				if (gm != null) { 
					gm.loadGroups(false);
					
					gm.removeGroup(group.getUserKey());
					gm.saveGroups();
					
					member = mm.removeMember(user.getUserKey());
					mm.saveMembers();
					
					String text = Strings.get(user.getPreference().getLanguage(), "Leave group \"%1$s\"");
					MessageHelper.notifySys(user.getUserKey(), String.format(text, group.getUserName()));
					
					rsp.add("user", getMemberInfo(member));
				}
			}
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Group: " + groupname + " not found");
		}
	}
	
	private void handleAccept(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		String username = trim(req.getParam("username"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		IUser usr = UserHelper.getLocalUserByName(username);
		
		if (grp != null && grp instanceof IGroup && usr != null && usr instanceof IMember) {
			IGroup group = (IGroup)grp;
			IMember user = (IMember)usr;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				if (!username.equals(me.getUserName())) { 
					MemberManager.GroupMember megm = mm.getMember(me.getUserKey());
					if (megm == null || (!MemberManager.ROLE_OWNER.equals(megm.getRole()) && 
						!MemberManager.ROLE_MANAGER.equals(megm.getRole()))) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"You are not manager of group: " + groupname);
					}
				}
				
				GroupManager gm = user.getGroupManager();
				if (gm != null) { 
					gm.loadGroups(false);
					
					mm.removeInvite(user.getUserKey());
					MemberManager.GroupMember usergm = mm.addMember(user.getUserKey(), user.getUserName(), MemberManager.ROLE_MEMBER);
					mm.saveMembers();
					
					gm.removeInvite(group.getUserKey());
					gm.addGroup(group.getUserKey(), group.getUserName(), MemberManager.ROLE_MEMBER);
					gm.saveGroups();
					
					if (!username.equals(me.getUserName())) { 
						String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" accepted you to join \"%2$s\"");
						MessageHelper.notifySys(user.getUserKey(), String.format(text, me.getUserName(), group.getUserName()));
					} else { 
						String text = Strings.get(user.getPreference().getLanguage(), "Accepted to join \"%1$s\"");
						MessageHelper.notifySys(user.getUserKey(), String.format(text, group.getUserName()));
					}
					
					rsp.add("member", UserInfoHandler.getUserInfo(usergm, user));
				}
			}
		} else { 
			if (grp == null || !(grp instanceof IGroup)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Group: " + groupname + " not found");
			} else if (usr == null || !(usr instanceof IMember)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"User: " + username + " not found");
			}
		}
	}
	
	private void handleDelete(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		String username = trim(req.getParam("username"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		if (username.equals(me.getUserName())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be yourself");
		}
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		IUser usr = UserHelper.getLocalUserByName(username);
		
		if (grp != null && grp instanceof IGroup && usr != null && usr instanceof IMember) {
			IGroup group = (IGroup)grp;
			IMember user = (IMember)usr;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				MemberManager.GroupMember megm = mm.getMember(me.getUserKey());
				if (megm == null || (!MemberManager.ROLE_OWNER.equals(megm.getRole()) && 
					!MemberManager.ROLE_MANAGER.equals(megm.getRole()))) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"You are not manager of group: " + groupname);
				}
				
				GroupManager gm = user.getGroupManager();
				if (gm != null) { 
					gm.loadGroups(false);
					
					MemberManager.GroupMember usergm = mm.removeMember(user.getUserKey());
					mm.saveMembers();
					
					gm.removeGroup(group.getUserKey());
					gm.saveGroups();
					
					String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" removed you from \"%2$s\"");
					MessageHelper.notifySys(user.getUserKey(), String.format(text, me.getUserName(), group.getUserName()));
					
					rsp.add("member", UserInfoHandler.getUserInfo(usergm, user));
				}
			}
		} else { 
			if (grp == null || !(grp instanceof IGroup)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Group: " + groupname + " not found");
			} else if (usr == null || !(usr instanceof IMember)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"User: " + username + " not found");
			}
		}
	}
	
	private void handleReject(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String groupname = trim(req.getParam("groupname"));
		String username = trim(req.getParam("username"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		IUser grp = UserHelper.getLocalUserByName(groupname);
		IUser usr = UserHelper.getLocalUserByName(username);
		
		if (grp != null && grp instanceof IGroup && usr != null && usr instanceof IMember) {
			IGroup group = (IGroup)grp;
			IMember user = (IMember)usr;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				if (!username.equals(me.getUserName())) { 
					MemberManager.GroupMember megm = mm.getMember(me.getUserKey());
					if (megm == null || (!MemberManager.ROLE_OWNER.equals(megm.getRole()) && 
						!MemberManager.ROLE_MANAGER.equals(megm.getRole()))) { 
						throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
								"You are not manager of group: " + groupname);
					}
				}
				
				GroupManager gm = user.getGroupManager();
				if (gm != null) { 
					gm.loadGroups(false);
					
					if (!username.equals(me.getUserName())) { 
						Invite invite = new Invite(group.getUserKey(), group.getUserName(), Invite.TYPE_OUT);
						invite.setMessage("Rejected");
						
						mm.removeInvite(user.getUserKey());
						mm.saveMembers();
						
						gm.addInvite(invite);
						gm.saveGroups();
						
						String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" rejected you to join \"%2$s\"");
						MessageHelper.notifySys(user.getUserKey(), String.format(text, me.getUserName(), group.getUserName()));
						
					} else { 
						Invite invite = new Invite(user.getUserKey(), user.getUserName(), Invite.TYPE_OUT);
						invite.setMessage("Rejected");
						
						mm.addInvite(invite);
						mm.saveMembers();
						
						gm.removeInvite(group.getUserKey());
						gm.saveGroups();
						
						String text = Strings.get(user.getPreference().getLanguage(), "Rejected to join \"%1$s\"");
						MessageHelper.notifySys(user.getUserKey(), String.format(text, group.getUserName()));
					}
					
					rsp.add("user", UserInfoHandler.getUserInfo(user));
				}
			}
		} else { 
			if (grp == null || !(grp instanceof IGroup)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Group: " + groupname + " not found");
			} else if (usr == null || !(usr instanceof IMember)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"User: " + username + " not found");
			}
		}
	}
	
	static NamedList<Object> getInviteList(MemberManager manager) 
			throws ErrorException { 
		Invite[] invites = manager.getInvites();
		return UserFriendHandler.getInviteList(invites);
	}
	
	static NamedList<Object> getMemberList(MemberManager manager) 
			throws ErrorException { 
		NamedList<Object> list = new NamedMap<Object>();
		
		MemberManager.GroupMember[] members = manager.getMembers();
		for (int i=0; members != null && i < members.length; i++) { 
			MemberManager.GroupMember member = members[i];
			if (member == null) continue;
			
			NamedList<Object> info = getMemberInfo(member);
			if (info != null) 
				list.add(member.getKey(), info);
		}
		
		return list;
	}
	
	static NamedList<Object> getInviteInfo(Invite item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			String avatar = "";
			String title = "";
			
			IUser user = UserHelper.getLocalUserByName(item.getName());
			if (user != null) { 
				avatar = user.getPreference().getAvatar();
				title = user.getPreference().getNickName();
			}
			
			info.add("key", toString(item.getKey()));
			info.add("name", toString(item.getName()));
			info.add("type", toString(item.getType()));
			info.add("avatar", toString(avatar));
			info.add("title", toString(title));
			info.add("message", toString(item.getMessage()));
			info.add("time", item.getTime());
		}
		
		return info;
	}
	
	static NamedList<Object> getMemberInfo(MemberManager.GroupMember item) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			String category = "";
			String avatar = "";
			String status = "";
			String title = "";
			
			IUser user = UserHelper.getLocalUserByName(item.getName());
			if (user != null) { 
				avatar = user.getPreference().getAvatar();
				status = user.getPreference().getStatus();
				category = user.getPreference().getCategory();
				
				if (title == null || title.length() == 0)
					title = user.getPreference().getNickName();
			}
			
			info.add("key", toString(item.getKey()));
			info.add("name", toString(item.getName()));
			info.add("role", toString(item.getRole()));
			info.add("title", toString(title));
			info.add("avatar", toString(avatar));
			info.add("status", toString(status));
			info.add("category", toString(category));
		}
		
		return info;
	}
	
}
