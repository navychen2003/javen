package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.profile.FriendManager;
import org.javenstudio.falcon.user.profile.Invite;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.datum.DatumHandlerBase;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserInfoHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserInfoHandler(core);
	}
	
	public UserInfoHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		IMember user = UserHelper.checkUser(req, IUserClient.Op.ACCESS);
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "info";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("info")) { 
			handleInfo(req, rsp, user);
		} else if (action.equalsIgnoreCase("update")) { 
			handleUpdate(req, rsp, user);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleUpdate(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String username = trim(req.getParam("username"));
		rsp.add("username", toString(username));
		
		IUser user = UserHelper.getLocalUserByName(username);
		if (user == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + username + " not found");
		}
		
		if (user instanceof IGroup) { 
			IGroup group = (IGroup)user;
			
			MemberManager mm = group.getMemberManager();
			if (mm != null) { 
				mm.loadMembers(false);
				
				MemberManager.GroupMember megm = mm.getMember(me.getUserKey());
				if (megm == null || (!MemberManager.ROLE_OWNER.equals(megm.getRole()) && !MemberManager.ROLE_MANAGER.equals(megm.getRole()))) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"You are not manager of group: " + username);
				}
				
				UserProfileHandler.handleUpdate(req, rsp, user, me, getCore().getHostSelf());
			}
		}
	}
	
	private void handleInfo(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String accesskey = trim(req.getParam("accesskey"));
		String username = trim(req.getParam("username"));
		
		if (username == null || username.length() == 0)
			username = me.getUserName();
		
		rsp.add("username", toString(username));
		
		IUser user = UserHelper.getLocalUserByName(username);
		if (user == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + username + " not found");
		}
		
		Profile profile = user.getProfile();
		if (profile != null) { 
			profile.loadProfile(false);
			
			if (user instanceof IGroup)
				rsp.add("group", getGroupInfo(me, (IGroup)user));
			else
				rsp.add("user", getUserInfo(me, user));
			
			rsp.add("profile", getProfileInfo(profile));
			
			String avatarSrc = profile.getAvatar();
			String backgroundSrc = profile.getBackground();
			
			IData avatarData = SectionHelper.getData(me, avatarSrc, IData.Access.THUMB, accesskey);
			IData backgroundData = SectionHelper.getData(me, backgroundSrc, IData.Access.THUMB, accesskey);
			
			if (avatarData != null && avatarData instanceof ISection) {
				NamedList<Object> dataInfo = DatumHandlerBase.getSectionInfo((ISection)avatarData);
				if (dataInfo != null)
					rsp.add("avatar", dataInfo);
			}
			
			if (backgroundData != null && backgroundData instanceof ISection) {
				NamedList<Object> dataInfo = DatumHandlerBase.getSectionInfo((ISection)backgroundData);
				if (dataInfo != null)
					rsp.add("background", dataInfo);
			}
		}
	}
	
	static NamedList<Object> getGroupInfo(IMember me, 
			IGroup grp) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		if (me == null || grp == null) 
			return info;
		
		String type = IUser.Util.typeOfUser(grp);
		String flag = IUser.Util.stringOfFlag(grp.getUserFlag());
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - grp.getAccessTime()) / 1000;
		
		String avatar = grp.getPreference().getAvatar();
		String status = grp.getPreference().getStatus();
		String category = grp.getPreference().getCategory();
		String title = grp.getPreference().getNickName();
		
		MemberManager mm = grp.getMemberManager();
		String role = null;
		int memberCount = 0;
		
		if (mm != null) { 
			mm.loadMembers(false);
			memberCount = mm.getMemberCount();
			
			MemberManager.GroupMember m = mm.getMember(me.getUserKey());
			if (m != null) 
				role = m.getRole();
		}
		
		info.add("key", toString(grp.getUserKey()));
		info.add("name", toString(grp.getUserName()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("idle", toString(Integer.toString(idleSecs) + 's'));
		info.add("title", toString(title));
		info.add("avatar", toString(avatar));
		info.add("status", toString(status));
		info.add("category", toString(category));
		info.add("mcount", memberCount);
		info.add("role", toString(role));
		
		return info;
	}
	
	static NamedList<Object> getGroupInfo(IGroup grp) 
			throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		if (grp == null) return info;
		
		String type = IUser.Util.typeOfUser(grp);
		String flag = IUser.Util.stringOfFlag(grp.getUserFlag());
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - grp.getAccessTime()) / 1000;
		
		String avatar = grp.getPreference().getAvatar();
		String status = grp.getPreference().getStatus();
		String category = grp.getPreference().getCategory();
		String title = grp.getPreference().getNickName();
		
		MemberManager mm = grp.getMemberManager();
		int memberCount = 0;
		
		if (mm != null) { 
			mm.loadMembers(false);
			memberCount = mm.getMemberCount();
		}
		
		info.add("key", toString(grp.getUserKey()));
		info.add("name", toString(grp.getUserName()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("idle", toString(Integer.toString(idleSecs) + 's'));
		info.add("title", toString(title));
		info.add("avatar", toString(avatar));
		info.add("status", toString(status));
		info.add("category", toString(category));
		info.add("mcount", memberCount);
		
		return info;
	}
	
	static NamedList<Object> getUserInfo(IMember me, 
			IUser usr) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		if (me == null || usr == null) 
			return info;
		
		String type = IUser.Util.typeOfUser(usr);
		String flag = IUser.Util.stringOfFlag(usr.getUserFlag());
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - usr.getAccessTime()) / 1000;
		
		FriendManager fm = me.getFriendManager();
		if (fm != null) fm.loadFriends(false);
		
		String avatar = usr.getPreference().getAvatar();
		String status = usr.getPreference().getStatus();
		String category = usr.getPreference().getCategory();
		String title = usr.getPreference().getNickName();
		
		String invite = null;
		String message = null;
		
		if (fm != null) { 
			if (fm.getFriend(usr.getUserKey()) != null) {
				invite = "friend";
			} else { 
				Invite iv = fm.getInvite(usr.getUserKey());
				if (iv != null) { 
					invite = iv.getType();
					message = iv.getMessage();
				}
			}
		}
		
		info.add("key", toString(usr.getUserKey()));
		info.add("name", toString(usr.getUserName()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("idle", toString(Integer.toString(idleSecs) + 's'));
		info.add("title", toString(title));
		info.add("avatar", toString(avatar));
		info.add("status", toString(status));
		info.add("category", toString(category));
		info.add("invite", toString(invite));
		info.add("message", toString(message));
		
		return info;
	}
	
	static NamedList<Object> getUserInfo(MemberManager.GroupMember gm, 
			IUser usr) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		if (usr == null) return info;
		
		String type = IUser.Util.typeOfUser(usr);
		String flag = IUser.Util.stringOfFlag(usr.getUserFlag());
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - usr.getAccessTime()) / 1000;
		
		String role = "";
		if (gm != null) role = gm.getRole();
		
		String avatar = usr.getPreference().getAvatar();
		String status = usr.getPreference().getStatus();
		String category = usr.getPreference().getCategory();
		String title = usr.getPreference().getNickName();
		
		info.add("key", toString(usr.getUserKey()));
		info.add("name", toString(usr.getUserName()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("idle", toString(Integer.toString(idleSecs) + 's'));
		info.add("title", toString(title));
		info.add("avatar", toString(avatar));
		info.add("status", toString(status));
		info.add("category", toString(category));
		info.add("role", toString(role));
		
		return info;
	}
	
	static NamedList<Object> getUserInfo(IUser usr) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		if (usr == null) return info;
		
		String type = IUser.Util.typeOfUser(usr);
		String flag = IUser.Util.stringOfFlag(usr.getUserFlag());
		
		long now = System.currentTimeMillis();
		int idleSecs = (int)(now - usr.getAccessTime()) / 1000;
		
		String avatar = usr.getPreference().getAvatar();
		String status = usr.getPreference().getStatus();
		String category = usr.getPreference().getCategory();
		String title = usr.getPreference().getNickName();
		
		info.add("key", toString(usr.getUserKey()));
		info.add("name", toString(usr.getUserName()));
		info.add("type", toString(type));
		info.add("flag", toString(flag));
		info.add("idle", toString(Integer.toString(idleSecs) + 's'));
		info.add("title", toString(title));
		info.add("avatar", toString(avatar));
		info.add("status", toString(status));
		info.add("category", toString(category));
		
		return info;
	}
	
	static NamedList<Object> getProfileInfo(Profile item) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("nickname", toString(item.getNickName()));
			info.add("firstname", toString(item.getFirstName()));
			info.add("lastname", toString(item.getLastName()));
			info.add("sex", toString(item.getSex()));
			//info.add("birthday", toString(item.getBirthday()));
			//info.add("timezone", toString(item.getTimezone()));
			info.add("region", toString(item.getRegion()));
			info.add("tags", toString(item.getTags()));
			info.add("brief", toString(item.getBrief()));
			info.add("intro", toString(item.getIntroduction()));
			info.add("avatar", toString(item.getAvatar()));
			info.add("background", toString(item.getBackground()));
		}
		
		return info;
	}
	
}
