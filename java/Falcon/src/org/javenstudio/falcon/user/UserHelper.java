package org.javenstudio.falcon.user;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.setting.user.UserCategory;
import org.javenstudio.falcon.user.device.DeviceManager;
import org.javenstudio.falcon.user.global.GroupUnit;
import org.javenstudio.falcon.user.global.MemberUnit;
import org.javenstudio.falcon.user.global.UnitManager;
import org.javenstudio.falcon.user.profile.HistoryItem;
import org.javenstudio.falcon.user.profile.HistoryManager;
import org.javenstudio.falcon.user.profile.Preference;
import org.javenstudio.falcon.util.IParams;
import org.javenstudio.falcon.util.IdentityUtils;
import org.javenstudio.util.StringUtils;

public class UserHelper {
	private static final Logger LOG = Logger.getLogger(UserHelper.class);

	public static int createHostHash(String key) {
		if (key == null || key.length() == 0)
			key = String.valueOf(System.currentTimeMillis());
		int hashCode = createHashCode(key);
		//if (hashCode < 0) hashCode = 0;
		//if (hashCode > Integer.MAX_VALUE) hashCode = Integer.MAX_VALUE;
		return hashCode;
	}
	
	private static final int HASH_MAX_VALUE = 65535;
	
	public static int createHashCode(String value) {
        if (value != null && value.length() > 0) {
            int hash = HASH_MAX_VALUE;

            for (int i = 0; i < value.length(); i++) {
                hash = 31 * hash + value.charAt(i);
            }
            
            if (hash < 0) hash *= (-1);
            
            return hash % HASH_MAX_VALUE;
        }
        return 0;
    }
	
	public static boolean checkUserKey(String key) { 
		if (key == null || key.length() != 8) return false;
		return true;
	}
	
	static String newUserKey(String username) throws ErrorException { 
		return IdentityUtils.newKey(username, 7) + '0';
	}
	
	static String newGroupKey(String groupname) throws ErrorException {
		return IdentityUtils.newKey(groupname, 7) + '1';
	}
	
	public static String newConversationKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 7) + '2';
	}
	
	public static String newContactKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newAnnouncementKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newHostKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newManagerKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newDeviceKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newClientKey(String name) throws ErrorException { 
		return IdentityUtils.newKey(name, 8);
	}
	
	public static String newClientToken(String username) throws ErrorException { 
		return IdentityUtils.newKey(username+"@"+System.currentTimeMillis(), 16);
	}
	
	public static String newAuthToken(String name) throws ErrorException {
		return IdentityUtils.toSHA256(name+"@"+System.currentTimeMillis());
	}
	
	public static IGroup registerLocalGroup(String groupname, 
			String category, String hostkey, IMember owner) throws ErrorException { 
		if (groupname == null || owner == null || hostkey == null) 
			throw new NullPointerException();
		if (category == null) category = IUser.PRIVATE;
		category = category.toLowerCase();
		
		UserManager manager = UserManager.getInstance();
		if (manager != null) { 
			UserCategory uc = manager.getCategoryManager().getCategory(category);
			if (uc == null || !uc.getType().equals(IUser.GROUP)) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Wrong category: " + category + " for group");
			}
			
			String userkey = newGroupKey(groupname);
			
			final IUserData data = manager.getService().addUser(groupname, 
					userkey, hostkey, "000000", IUser.FLAG_ENABLED, IUser.TYPE_GROUP, null);
			
			if (data != null) { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("registerLocalGroup: groupname=" + groupname + " data=" + data 
							+ " category=" + category);
				}
				
				groupname = data.getUserName();
				
				UnitManager um = manager.getUnitManager();
				if (um != null) { 
					GroupUnit unit = new GroupUnit(userkey, groupname);
					unit.setCategory(category);
					unit.setOwner(owner.getUserName());
					
					um.loadUnits(false);
					um.addUnit(unit);
					um.saveUnits();
				}
				
				IGroup group = (IGroup)manager.getOrCreate(groupname, 
					new IUserData.Factory() {
						@Override
						public IUserData create(String username) throws ErrorException {
							return data;
						}
					});
				
				Preference preference = group.getPreference();
				preference.setUserKey(userkey);
				preference.setUserName(groupname);
				preference.setUserType(IUser.GROUP);
				preference.setCategory(category);
				preference.savePreference();
				
				return group;
			}
		}
		
		return null;
	}
	
	public static IMember authLocalUser(String username, String password) 
			throws ErrorException { 
		if (username == null || password == null) return null;
		
		UserManager manager = UserManager.getInstance();
		if (manager != null) { 
			final IUserData data = manager.getService().authUser(username, password);
			
			if (data != null) { 
				if (LOG.isDebugEnabled())
					LOG.debug("authLocalUser: username=" + username + " data=" + data);
				
				username = data.getUserName();
				
				if (data.getUserType() == IUser.TYPE_GROUP) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Group: " + username + " cannot login");
				}
				
				return (IMember)manager.getOrCreate(username, 
					new IUserData.Factory() {
						@Override
						public IUserData create(String username) throws ErrorException {
							return data;
						}
					});
			}
		}
		
		return null;
	}
	
	public static IMember registerLocalUser(String username, String password, 
			String category, String hostkey) throws ErrorException { 
		if (username == null || password == null || hostkey == null) 
			throw new NullPointerException();
		if (category == null) category = IUser.NORMAL;
		category = category.toLowerCase();
		
		UserManager manager = UserManager.getInstance();
		if (manager != null) { 
			UserCategory uc = manager.getCategoryManager().getCategory(category);
			if (uc == null || !uc.getType().equals(IUser.USER)) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Wrong category: " + category + " for user");
			}
			
			String userkey = newUserKey(username);
			
			final IUserData data = manager.getService().addUser(username, 
					userkey, hostkey, password, IUser.FLAG_ENABLED, IUser.TYPE_USER, null);
			
			if (data != null) { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("registerLocalUser: username=" + username + " data=" + data 
							+ " category=" + category);
				}
				
				username = data.getUserName();
				
				UnitManager um = manager.getUnitManager();
				if (um != null) { 
					MemberUnit unit = new MemberUnit(userkey, username);
					unit.setCategory(category);
					
					um.loadUnits(false);
					um.addUnit(unit);
					um.saveUnits();
				}
				
				IMember user = (IMember)manager.getOrCreate(username, 
					new IUserData.Factory() {
						@Override
						public IUserData create(String username) throws ErrorException {
							return data;
						}
					});
				
				Preference preference = user.getPreference();
				preference.setUserKey(userkey);
				preference.setUserName(username);
				preference.setUserType(IUser.USER);
				preference.setCategory(category);
				preference.savePreference();
				
				return user;
			}
		}
		
		return null;
	}
	
	public static IUser updateLocalUser(String username, String hostkey, 
			String password, int flag) throws ErrorException { 
		return updateLocalUser(username, hostkey, password, flag, null);
	}
	
	public static IUser updateLocalUser(String username, String hostkey, String password, 
			int flag, Map<String,String> attrs) throws ErrorException { 
		if (username == null) throw new NullPointerException();
		
		UserManager manager = UserManager.getInstance();
		if (manager != null) { 
			final IUserData data = manager.getService().updateUser(username, 
					hostkey, password, flag, attrs);
			
			if (data != null) { 
				if (LOG.isDebugEnabled())
					LOG.debug("updateLocalUser: username=" + username + " data=" + data);
				
				username = data.getUserName();
				
				User user = (User)manager.getOrCreate(username, 
					new IUserData.Factory() {
						@Override
						public IUserData create(String username) throws ErrorException {
							return data;
						}
					});
				
				if (user.getUserFlag() != data.getUserFlag())
					user.setUserFlag(data.getUserFlag());
				
				return user;
			}
		}
		
		return null;
	}
	
	public static IUser getLocalUserByKey(String userkey) throws ErrorException {
		if (userkey == null || userkey.length() == 0) return null;
		
		UserManager manager = UserManager.getInstance();
		if (manager != null) { 
			IUser user = manager.getUserByKey(userkey);
			if (user == null) { 
				INameData data = manager.getService().searchName(userkey);
				String username = data != null ? data.getNameValue() : null;
				if (username != null && username.length() > 0)
					user = getLocalUserByName(username);
			}
			return user;
		}
		
		return null;
	}
	
	public static IUser getLocalUserByName(String username) throws ErrorException { 
		if (username == null || username.length() == 0) return null;
		
		if (username.endsWith("@"))
			username = username.substring(0, username.length()-1);
		
		UserManager manager = UserManager.getInstance();
		if (manager != null) 
			return manager.getOrCreate(username);
		
		return null;
	}
	
	public static IUser[] searchLocalUser(String name) throws ErrorException { 
		if (name == null || name.length() == 0) return null;
		
		ArrayList<IUser> list = new ArrayList<IUser>();
		
		IUser user1 = getLocalUserByName(name);
		IUser user2 = getLocalUserByKey(name);
		
		if (user1 != null) list.add(user1);
		if (user2 != null) list.add(user2);
		
		return list.toArray(new IUser[list.size()]);
	}
	
	private static String searchLocalNameValue(UserManager manager, String name) 
			throws ErrorException {
		if (manager == null || name == null || name.length() == 0) 
			return null;
		
		INameData data = manager.getService().searchName(name);
		String value = data != null ? data.getNameValue() : null;
		
		return value;
	}
	
	public static void updateLocalName(UserManager manager, String name, 
			String value, String hostkey, int flag, Map<String,String> attrs, 
			String oldname) throws ErrorException {
		if (manager == null || name == null || name.length() == 0)
			return;
		
		String curvalue = searchLocalNameValue(manager, name);
		if (curvalue != null && curvalue.equals(value))
			return;
		
		if (oldname != null && oldname.length() > 0) {
			String oldvalue = searchLocalNameValue(manager, oldname);
			if (oldvalue != null && oldvalue.equals(value)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateLocalName: remove old name: " + oldname 
							+ " value: " + oldvalue);
				}
				
				manager.getService().removeName(oldname);
			}
		}
		
		manager.getService().updateName(name, value, hostkey, flag, attrs);
	}
	
	public static IUserClient getClientByToken(String token) { 
		return UserManager.getInstance().getClient(token);
	}
	
	public static IMember getUserByToken(String token) { 
		IUserClient client = getClientByToken(token);
		if (client != null) 
			return client.getUser();
		
		return null;
	}
	
	
	public static String getParamToken(IParams req) 
			throws ErrorException {
		return StringUtils.trim(req.getParam(IParams.TOKEN));
	}
	
	public static String[] getUserKeyTokens(IParams req) 
			throws ErrorException { 
		return getUserKeyTokens(getParamToken(req));
	}
	
	public static String[] getUserKeyTokens(String token) 
			throws ErrorException { 
		if (token != null && token.length() > 0) { 
			if (token.length() == 32) {
				String[] res = new String[3];
				res[0] = token.substring(0, 8);
				res[1] = token.substring(8, 16);
				res[2] = token.substring(16, 32);
				return res;
			} else
				return new String[0];
		}
		//throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
		//		"Unauthorized Access");
		return null;
	}
	
	public static String checkUserKey(IParams req, 
			IUserClient.Op op) throws ErrorException { 
		return checkUserKey(getUserKeyTokens(req), op);
	}
	
	public static String checkUserKey(String[] keyToken, 
			IUserClient.Op op) throws ErrorException { 
		if (keyToken != null && keyToken.length == 3)
			return checkUserKey(keyToken[0], keyToken[1], keyToken[2], op);
		throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
				"Unauthorized Access");
	}
	
	public static String checkUserKey(String hostKey, 
			String userKey, String token, IUserClient.Op op) 
			throws ErrorException { 
		IUser user = checkUser(hostKey, userKey, token, op);
		if (user != null) return user.getUserKey();
		throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
				"Unauthorized Access");
	}
	
	public static IMember checkUser(IParams req, 
			IUserClient.Op op) throws ErrorException { 
		return checkUser(getUserKeyTokens(req), op);
	}
	
	public static IMember checkAdmin(IParams req, 
			IUserClient.Op op) throws ErrorException { 
		IMember user = checkUser(getUserKeyTokens(req), op);
		if (user == null) { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Unauthorized Access");
		}
		if (!user.isManager()) { 
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"No permission: not administrator");
		}
		return user;
	}
	
	public static IMember checkUser(String[] keyToken, 
			IUserClient.Op op) throws ErrorException { 
		if (keyToken != null && keyToken.length == 3)
			return checkUser(keyToken[0], keyToken[1], keyToken[2], op);
		throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
				"Unauthorized Access");
	}
	
	public static IMember checkUser(String hostKey, 
			String userKey, String token, IUserClient.Op op) 
			throws ErrorException { 
		IUserClient client = checkUserClient(hostKey, userKey, token, op);
		IMember user = client != null ? client.getUser() : null;
		if (user != null) return user;
		throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
				"Unauthorized Access");
	}
	
	public static IUserClient checkUserClient(String token, 
			IUserClient.Op op) throws ErrorException { 
		String[] keyToken = getUserKeyTokens(token);
		if (keyToken != null) return checkUserClient(keyToken, op);
		return null;
	}
	
	public static IUserClient checkUserClient(IParams req, 
			IUserClient.Op op) throws ErrorException { 
		String[] keyToken = getUserKeyTokens(req);
		if (keyToken != null) return checkUserClient(keyToken, op);
		return null;
	}
	
	public static IUserClient checkUserClient(String[] keyToken, 
			IUserClient.Op op) throws ErrorException { 
		if (keyToken != null && keyToken.length == 3)
			return checkUserClient(keyToken[0], keyToken[1], keyToken[2], op);
		throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
				"Unauthorized Access");
	}
	
	public static IUserClient checkUserClient(String hostKey, 
			String userKey, String token, IUserClient.Op op) throws ErrorException { 
		String reason = "";
		if (hostKey != null && userKey != null && token != null) {
			if (hostKey.equals(UserManager.getInstance().getStore().getHostNode().getHostKey())) { 
				IUserClient client = UserManager.checkClientTimeout(getClientByToken(token));
				
				if (client != null) { 
					if (userKey.equals(client.getUser().getUserKey())) {
						if (op == IUserClient.Op.ACCESS) {
							long now = System.currentTimeMillis();
							((UserClient)client).setAccessTime(now);
							((User)client.getUser()).setAccessTime(now);
							
						} else if (op == IUserClient.Op.REFRESH) {
							long now = System.currentTimeMillis();
							((UserClient)client).setAccessTime(now);
							((User)client.getUser()).setAccessTime(now);
							client.refreshToken();
						}
						
						return client;
					} else
						reason = ": wrong user";
				} else {
					reason = ": timeout";
				}
			} else
				reason = ": wrong host";
		}
		if (op == null) return null;
		throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
				"Unauthorized Access" + reason);
	}
	
	public static DeviceManager checkUserDevice(String[] keyToken, 
			IUserClient.Op op) throws ErrorException { 
		if (keyToken != null && keyToken.length == 3)
			return checkUserDevice(keyToken[0], keyToken[1], keyToken[2], op);
		throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
				"Unauthorized Access");
	}
	
	public static DeviceManager checkUserDevice(String hostKey, 
			String userKey, String token, IUserClient.Op op) 
			throws ErrorException { 
		IMember user = checkUser(hostKey, userKey, token, op);
		if (user != null) { 
			DeviceManager manager = user.getDeviceManager();
			if (manager != null) manager.loadDevices(false);
			return manager;
		}
		return null;
	}
	
	public static HistoryManager addHistory(IMember user, 
			IData data, String op) {
		return addHistory(user, data, op, 0, true);
	}
	
	private static HistoryManager addHistory(IMember user, 
			IData data, String operation, long time, boolean save) {
		if (user == null || data == null) return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("addHistory: user=" + user + " data=" + data 
					+ " op=" + operation + " time=" + time + " save=" + save);
		}
		
		if (data instanceof ILibrary || data instanceof ISectionRoot)
			return null;
		
		if (data instanceof ISection) {
			ISection section = (ISection)data;
			if (section.isFolder()) return null;
		}
		
		try {
			HistoryManager hm = user.getHistoryManager();
			if (hm != null) {
				hm.loadHistoryItems(false);
				
				if (time <= 0) time = System.currentTimeMillis();
				
				HistoryItem item = new HistoryItem(
						data.getContentId(), data.getContentType());
				item.setTitle(data.getName());
				item.setOwner(data.getOwner());
				item.setOperation(operation);
				item.setTime(time);
				
				hm.addHistory(item);
				if (save) hm.saveHistoryItems();
				
				return hm;
			}
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("addHistory: error: " + e, e);
		}
		
		return null;
	}
	
	private static HistoryManager removeHistory(IMember user, 
			String contentId, boolean save) {
		if (user == null || contentId == null) return null;
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("removeHistory: user=" + user + " contentId=" + contentId);
		
		try {
			HistoryManager hm = user.getHistoryManager();
			if (hm != null) {
				hm.loadHistoryItems(false);
				
				HistoryItem item = hm.removeHistory(contentId);
				if (item != null && save) 
					hm.saveHistoryItems();
				
				return hm;
			}
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("removeHistory: error: " + e, e);
		}
		
		return null;
	}
	
	public static void addHistory(IMember user, 
			String[] contentIds, String[] removeIds, String op) {
		addHistory(user, contentIds, removeIds, op, 0);
	}
	
	private static void addHistory(IMember user, 
			String[] contentIds, String[] removeIds, String op, long time) {
		if (user == null || contentIds == null)
			return;
		
		HistoryManager historyManager = null;
		IMember member = user;
		
		if (removeIds != null) {
			for (String contentId : removeIds) {
				if (contentId == null || contentId.length() == 0)
					continue;
				
				if (LOG.isDebugEnabled())
					LOG.debug("addHistory: removeId: " + contentId);
				
				HistoryManager hm = removeHistory(user, contentId, false);
				if (hm != null)
					historyManager = hm;
			}
		}
		
		for (String contentId : contentIds) {
			if (contentId == null || contentId.length() == 0)
				continue;
			
			if (LOG.isDebugEnabled())
				LOG.debug("addHistory: contentId: " + contentId);
			
			try {
				IData data = SectionHelper.getData(member, contentId, 
						IData.Access.DETAILS, null);
				if (data != null) {
					long historyTime = time;
					if (historyTime <= 0) historyTime = data.getModifiedTime();
					
					HistoryManager hm = UserHelper.addHistory(member, 
							data, op, historyTime, false);
					if (hm != null)
						historyManager = hm;
				}
			} catch (Throwable e) {
				if (LOG.isWarnEnabled())
					LOG.warn("addHistory: add history error: " + e, e);
			}
		}
		
		try {
			if (historyManager != null)
				historyManager.saveHistoryItems();
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("addHistory: save history error: " + e, e);
		}
	}
	
	public static boolean checkEmailAddress(String name) {
		if (name == null || name.length() < 3)
			return false;
		
		if (name.indexOf('@') >= 0) {
			Matcher matcher = EMAIL_PATTERN.matcher(name);
			return matcher.matches();
		}
		
		return false;
	}
	
	public static boolean checkUserName(String name) {
		if (name == null || name.length() < 4 || name.length() > 38)
			return false;
		
		@SuppressWarnings("unused")
		int count1 = 0;
		@SuppressWarnings("unused")
		int count2 = 0;
		int count3 = 0;
		
		for (int i=0; i < name.length(); i++) { 
			char chr = name.charAt(i);
			
			if (chr >= 'a' && chr <= 'z') {
				count1 ++;
				continue;
			}
			
			if (chr >= '0' && chr <= '9') {
				if (i == 0) return false;
				count2 ++;
				continue;
			}
			
			if (chr == '-' || chr == '_' || chr == '.') {
				if (i == 0) return false;
				count3 ++;
				continue;
			}
			
			return false;
		}
		
		if (count3 > 1) return false;
		
		return true;
	}
	
	private static final Pattern EMAIL_PATTERN = 
			Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
	
}
