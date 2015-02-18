package org.javenstudio.lightning.core.user;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.global.UnitManager;
import org.javenstudio.falcon.user.profile.GroupManager;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserFindHandler extends UserHandlerBase {

	public static RequestHandler createHandler(UserCore core) { 
		return new UserFindHandler(core);
	}
	
	public UserFindHandler(UserCore core) { 
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
		
		if (action.equalsIgnoreCase("listall")) { 
			handleList(req, rsp, user);
		} else if (action.equalsIgnoreCase("search")) { 
			handleSearch(req, rsp, user);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleSearch(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (user == null) return;
		
		boolean isManager = user.isManager();
		boolean searchUser = true;
		boolean searchGroup = true;
		boolean searchAll = false;
		
		String groupCategory = IUser.PUBLIC;
		String userCategory = IUser.NORMAL;
		
		String name = trim(req.getParam("name"));
		if (name == null) name = "";
		name = name.toLowerCase();
		
		if (name.equals("*")) {
			name = "";
			if (isManager) {
				groupCategory = null;
				userCategory = null;
				searchAll = true;
			}
		} else if (name.equalsIgnoreCase("user:*")) {
			name = "";
			searchGroup = false;
			if (isManager) {
				userCategory = null;
				searchAll = true;
			}
		} else if (name.equalsIgnoreCase("group:*")) {
			name = "";
			searchUser = false;
			if (isManager) {
				groupCategory = null;
				searchAll = true;
			}
		}
		
		final String searchname = name;
		
		UserGroupHandler.UserFilter userFilter = new UserGroupHandler.UserFilter() {
				@Override
				public boolean accept(IMember user) {
					if (user != null) { 
						if (searchname == null || searchname.length() == 0)
							return true;
						
						String gname = user.getUserName();
						if (gname != null && gname.length() > 0) { 
							gname = gname.toLowerCase();
							if (gname.indexOf(searchname) >= 0) 
								return true;
						}
						
						String gnick = null; 
						try { gnick = user.getPreference().getNickName(); }
						catch (Throwable e) { gnick = null; }
						
						if (gnick != null && gnick.length() > 0) { 
							gnick = gnick.toLowerCase();
							if (gnick.indexOf(searchname) >= 0) 
								return true;
						}
					}
					return false;
				}
			};
		
		UserGroupHandler.GroupFilter groupFilter = new UserGroupHandler.GroupFilter() {
				@Override
				public boolean accept(GroupManager.GroupUser group) {
					if (group != null) return true;
					return false;
				}
				@Override
				public boolean accept(IGroup group) {
					if (group != null) { 
						if (searchname == null || searchname.length() == 0)
							return true;
						
						String gname = group.getUserName();
						if (gname != null && gname.length() > 0) { 
							gname = gname.toLowerCase();
							if (gname.indexOf(searchname) >= 0) 
								return true;
						}
						
						String gnick = null; 
						try { gnick = group.getPreference().getNickName(); }
						catch (Throwable e) { gnick = null; }
						
						if (gnick != null && gnick.length() > 0) { 
							gnick = gnick.toLowerCase();
							if (gnick.indexOf(searchname) >= 0) 
								return true;
						}
					}
					return false;
				}
			};
		
		UnitManager manager = getManager().getUnitManager();
		NamedList<Object> usersInfo = null;
		NamedList<Object> groupsInfo = null;
		
		if (searchUser) {
			usersInfo = isManager && searchAll ? 
				UserGroupHandler.getUserList(user, manager, userFilter, userCategory) :
				getUserList(user, name);
		}
		if (searchGroup) {
			groupsInfo = UserGroupHandler.getGroupList(
				user, manager, groupFilter, groupCategory);
		}
		
		if (groupsInfo == null)
			groupsInfo = new NamedMap<Object>();
		
		if (usersInfo == null)
			usersInfo = new NamedMap<Object>();
		
		rsp.add("groups", groupsInfo);
		rsp.add("users", usersInfo);
	}
	
	private void handleList(Request req, Response rsp, 
			IMember user) throws ErrorException { 
		if (user == null) return;
		
		UserGroupHandler.GroupFilter groupFilter = new UserGroupHandler.GroupFilter() {
				@Override
				public boolean accept(GroupManager.GroupUser group) {
					if (group != null) return true;
					return false;
				}
				@Override
				public boolean accept(IGroup group) {
					if (group != null) { 
						String category = null; 
						try { category = group.getPreference().getCategory(); }
						catch (Throwable e) { category = null; }
						
						if (category != null && category.length() > 0) { 
							if (category.equals(IUser.PUBLIC))
								return true;
						}
					}
					return false;
				}
			};
		
		NamedList<Object> usersInfo = getUserList(user, null);
		NamedList<Object> groupsInfo = UserGroupHandler.getGroupList(user, 
				getManager().getUnitManager(), groupFilter, IUser.PUBLIC);
		
		if (groupsInfo == null)
			groupsInfo = new NamedMap<Object>();
		
		if (usersInfo == null)
			usersInfo = new NamedMap<Object>();
		
		rsp.add("groups", groupsInfo);
		rsp.add("users", usersInfo);
	}
	
	private NamedList<Object> getUserList(IMember user, 
			String name) throws ErrorException { 
		NamedList<Object> list = new NamedMap<Object>();
		
		IUser[] users = UserHelper.searchLocalUser(name);
		for (int i=0; users != null && i < users.length; i++) { 
			IUser usr = users[i];
			if (usr == null || usr instanceof IGroup) continue;
			
			list.add(usr.getUserKey(), UserInfoHandler.getUserInfo(user, usr));
		}
		
		return list;
	}
	
}
