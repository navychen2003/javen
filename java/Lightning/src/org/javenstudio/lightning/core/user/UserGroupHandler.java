package org.javenstudio.lightning.core.user;

import java.util.Arrays;
import java.util.Comparator;

import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.NamedHelper;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.UserManager;
import org.javenstudio.falcon.user.global.GroupUnit;
import org.javenstudio.falcon.user.global.IUnit;
import org.javenstudio.falcon.user.global.MemberUnit;
import org.javenstudio.falcon.user.global.UnitManager;
import org.javenstudio.falcon.user.profile.GroupManager;
import org.javenstudio.falcon.user.profile.Invite;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserGroupHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserGroupHandler(core);
	}
	
	public UserGroupHandler(UserCore core) { 
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
		
		if (action.equalsIgnoreCase("register")) { 
			handleRegister(req, rsp, user);
		} else if (action.equalsIgnoreCase("list")) { 
			handleList(req, rsp, user);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleRegister(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		String groupname = trim(req.getParam("groupname"));
		String nickname = trim(req.getParam("nickname"));
		String category = trim(req.getParam("category"));
		
		if (groupname == null || groupname.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Groupname cannot be empty");
		}
		
		if (category == null || category.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Group category cannot be empty");
		}
		
		if (category.equalsIgnoreCase(IUser.PUBLIC) && !user.isManager()) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"You have no permission to create public group");
		}
		
		checkGroupname(groupname);
		nickname = UserProfileHandler.checkNickname(getCore().getClusterSelf(), groupname, nickname);
		
		IHostCluster cluster = getCore().getClusterSelf();
		String hostkey = getCore().getHostSelf().getHostKey();
		
		IGroup group = UserHelper.registerLocalGroup(groupname, category, hostkey, user);
		if (group != null) {
			initProfile(group, nickname, user);
			cluster.addUser(groupname, group.getUserKey(), hostkey, "000000", 
					group.getUserFlag(), group.getUserType(), null);
			
			String text = Strings.get(user.getPreference().getLanguage(), "Registered new group \"%1$s\"");
			MessageHelper.notifySys(user.getUserKey(), String.format(text, group.getUserName()));
			
			rsp.add("group", NamedHelper.toNamedMap(group, 0, 0, IUser.ShowType.ALL));
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Group: " + groupname + " register failed");
		}
	}
	
	private void initProfile(IGroup group, String nickname, 
			IMember user) throws ErrorException { 
		if (group == null) return;
		
		Profile profile = group.getProfile();
		if (profile != null) { 
			profile.loadProfile(false);
			profile.setUserName(group.getUserName(), false);
			profile.setNickName(nickname, false);
			profile.saveProfile();
		}
		
		if (user != null) { 
			MemberManager manager = group.getMemberManager();
			if (manager != null) { 
				manager.loadMembers(false);
				manager.addMember(user.getUserKey(), user.getUserName(), 
						MemberManager.ROLE_OWNER);
				manager.saveMembers();
			}
			
			GroupManager gm = user.getGroupManager();
			if (gm != null) { 
				gm.loadGroups(false);
				gm.addGroup(group.getUserKey(), group.getUserName(), 
						MemberManager.ROLE_OWNER);
				gm.saveGroups();
			}
		}
	}
	
	private void handleList(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (user == null) return;
		
		NamedList<Object> invitesInfo = getInviteList(user, 
				user.getGroupManager());
		NamedList<Object> groupsInfo = getGroupList(user, 
				user.getGroupManager(), null);
		
		if (invitesInfo == null)
			invitesInfo = new NamedMap<Object>();
		if (groupsInfo == null)
			groupsInfo = new NamedMap<Object>();
		
		rsp.add("invites", invitesInfo);
		rsp.add("groups", groupsInfo);
	}
	
	private NamedList<Object> getInviteList(IMember user, 
			GroupManager gm) throws ErrorException { 
		NamedList<Object> list = new NamedMap<Object>();
		if (gm == null) return list;
		
		gm.loadGroups(false);
		Invite[] invites = gm.getInvites();
		
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
			
			NamedList<Object> info = UserMemberHandler.getInviteInfo(invite);
			if (info != null) 
				list.add(invite.getKey(), info);
		}
		
		return list;
	}
	
	static interface GroupFilter { 
		public boolean accept(GroupManager.GroupUser group);
		public boolean accept(IGroup group);
	}
	
	static interface UserFilter { 
		public boolean accept(IMember user);
	}
	
	static NamedList<Object> getGroupList(IMember user, 
			GroupManager gm, GroupFilter filter) throws ErrorException { 
		NamedList<Object> list = new NamedMap<Object>();
		if (gm == null) return list;
		
		gm.loadGroups(false);
		GroupManager.GroupUser[] groups = gm.getGroups();
		
		for (int i=0; groups != null && i < groups.length; i++) { 
			GroupManager.GroupUser group = groups[i];
			if (group == null) continue;
			if (filter != null && filter.accept(group) == false)
				continue;
			
			IUser usr = UserManager.getInstance().getOrCreate(group.getName());
			if (usr != null && usr instanceof IGroup) { 
				IGroup grp = (IGroup)usr;
				if (filter != null && filter.accept(grp) == false)
					continue;
				
				list.add(grp.getUserKey(), UserInfoHandler.getGroupInfo(user, grp));
			}
		}
		
		return list;
	}
	
	static NamedList<Object> getGroupList(IMember me, 
			UnitManager um, GroupFilter filter, String category) 
			throws ErrorException { 
		NamedList<Object> list = new NamedMap<Object>();
		if (um == null) return list;
		
		um.loadUnits(false);
		IUnit[] groups = um.getUnits(IUnit.TYPE_GROUP, category);
		
		for (int i=0; groups != null && i < groups.length; i++) { 
			IUnit item = groups[i];
			if (item == null) continue;
			if (!(item instanceof GroupUnit)) continue;
			
			GroupUnit unit = (GroupUnit)item;
			GroupManager.GroupUser group = new GroupManager.GroupUser(
					unit.getKey(), unit.getName(), MemberManager.ROLE_MEMBER);
			
			if (filter != null && filter.accept(group) == false)
				continue;
			
			IUser usr = UserManager.getInstance().getOrCreate(group.getName());
			if (usr != null && usr instanceof IGroup) { 
				IGroup grp = (IGroup)usr;
				if (filter != null && filter.accept(grp) == false)
					continue;
				
				list.add(grp.getUserKey(), UserInfoHandler.getGroupInfo(me, grp));
			}
		}
		
		return list;
	}
	
	static NamedList<Object> getUserList(IMember me, 
			UnitManager um, UserFilter filter, String category) 
			throws ErrorException { 
		NamedList<Object> list = new NamedMap<Object>();
		if (um == null) return list;
		
		um.loadUnits(false);
		IUnit[] groups = um.getUnits(IUnit.TYPE_MEMBER, category);
		
		for (int i=0; groups != null && i < groups.length; i++) { 
			IUnit item = groups[i];
			if (item == null) continue;
			if (!(item instanceof MemberUnit)) continue;
			
			MemberUnit unit = (MemberUnit)item;
			
			IUser usr = UserManager.getInstance().getOrCreate(unit.getName());
			if (usr != null && usr instanceof IMember) { 
				IMember user = (IMember)usr;
				if (filter != null && filter.accept(user) == false)
					continue;
				
				list.add(user.getUserKey(), UserInfoHandler.getUserInfo(me, user));
			}
		}
		
		return list;
	}
	
}
