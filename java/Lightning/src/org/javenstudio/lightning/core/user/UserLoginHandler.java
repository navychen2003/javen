package org.javenstudio.lightning.core.user;

import java.util.Iterator;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.util.TimeUtils;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.setting.cluster.ActionError;
import org.javenstudio.falcon.setting.cluster.AnyboxData;
import org.javenstudio.falcon.setting.cluster.AnyboxListener;
import org.javenstudio.falcon.setting.cluster.HostHelper;
import org.javenstudio.falcon.setting.cluster.HostMode;
import org.javenstudio.falcon.setting.cluster.IAttachUserInfo;
import org.javenstudio.falcon.setting.cluster.IAuthInfo;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.setting.cluster.IHostInfo;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.setting.cluster.IHostUserName;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.IUserName;
import org.javenstudio.falcon.user.Member;
import org.javenstudio.falcon.user.NamedHelper;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.auth.AuthHelper;
import org.javenstudio.falcon.user.device.AndroidDevice;
import org.javenstudio.falcon.user.device.AndroidDeviceType;
import org.javenstudio.falcon.user.device.AndroidUserClient;
import org.javenstudio.falcon.user.device.Device;
import org.javenstudio.falcon.user.device.DeviceGroup;
import org.javenstudio.falcon.user.device.DeviceManager;
import org.javenstudio.falcon.user.device.DeviceType;
import org.javenstudio.falcon.user.device.IOSDevice;
import org.javenstudio.falcon.user.device.IOSDeviceType;
import org.javenstudio.falcon.user.device.IOSUserClient;
import org.javenstudio.falcon.user.device.WebDevice;
import org.javenstudio.falcon.user.device.WebDeviceType;
import org.javenstudio.falcon.user.device.WebUserClient;
import org.javenstudio.falcon.user.device.WinDevice;
import org.javenstudio.falcon.user.device.WinDeviceType;
import org.javenstudio.falcon.user.device.WinUserClient;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.CoreCluster;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;

public class UserLoginHandler extends UserHandlerBase {
	private static final Logger LOG = Logger.getLogger(UserLoginHandler.class);

	public static RequestHandler createHandler(UserCore core) { 
		return new UserLoginHandler(core);
	}
	
	public UserLoginHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0) 
			action = "login";
		
		rsp.add("action", action);
		
		if (action.equalsIgnoreCase("availability")) { 
			handleAvailability(req, rsp);
		} else if (action.equalsIgnoreCase("logout")) { 
			handleLogout(req, rsp);
		} else if (action.equalsIgnoreCase("check")) { 
			handleCheck(req, rsp);
		} else if (action.equalsIgnoreCase("updateflag")) { 
			handleUpdateFlag(req, rsp);
		} else if (action.equalsIgnoreCase("updatepwd")) { 
			handleUpdatePassword(req, rsp);
		} else { 
			handleAction(req, rsp, action);
		}
	}
	
	private void handleAvailability(Request req, Response rsp) 
			throws ErrorException { 
		if (req == null || rsp == null)
			return;
		
		String username = trim(req.getParam("username"));
		boolean availability = false;
		
		if (username != null && username.length() > 0) {
			IHostCluster cluster = getCore().getClusterSelf();
			if (cluster != null) {
				if (cluster.searchUser(username) == null && 
					cluster.searchName(username) == null) {
					availability = true;
				}
			}
		}
		
		rsp.add("result", availability);
	}
	
	private void handleLogout(Request req, Response rsp) 
			throws ErrorException { 
		if (req == null || rsp == null)
			return;
		
		NamedList<Object> info = null;
		
		IUserClient client = UserHelper.checkUserClient(req, IUserClient.Op.ACCESS);
		if (client != null) { 
			IMember user = client.getUser();
			String appaddr = req.getRemoteAddr();
			
			if (appaddr == null || appaddr.length() == 0)
				appaddr = "unknown";
			
			String text = Strings.get(user.getPreference().getLanguage(), "Logout from \"%1$s\" by \"%2$s\"");
			MessageHelper.notifyLog(user.getUserKey(), 
					String.format(text, appaddr, client.getDevice().toReadableTitle()), 
					client.getDevice().toReadableString());
			
			client.logout();
			
			info = NamedHelper.toNamedMap(user, 0, 0, null);
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"User did not login");
		}
		
		if (info == null)
			info = new NamedMap<Object>();
		
		rsp.add("user", info);
	}
	
	private void handleUpdatePassword(Request req, Response rsp) 
			throws ErrorException { 
		if (req == null || rsp == null)
			return;
		
		IUserClient client = UserHelper.checkUserClient(req, IUserClient.Op.ACCESS);
		if (client != null) { 
			IUserName uname = getCore().getClusterSelf().parseUserName(req.getParam("username"));
			String username = uname != null ? uname.getUserName() : null;
			String password = req.getParam("password");
			String hostkey = getCore().getHostSelf().getHostKey();
			
			if (username == null || username.length() == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Username cannot be empty");
			}
			
			if (password == null || password.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Password cannot be empty");
			}
			
			checkPassword(password);
			
			if (!client.getUser().isManager()) {
				if (!username.equals(client.getUser().getUserName())) {
					throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
							"You are not manager and cannot change other's password");
				}
			}
			
			IUser user = UserHelper.getLocalUserByName(username);
			if (user == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"User: " + username + " not found");
			}
			
			if (user instanceof IMember) {
				IMember member = (IMember)user;
				if (member.isManager() && !client.getUser().isAdministrator() && 
					!user.getUserName().equals(client.getUser().getUserName())) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"User: " + username + " is a manager and cannot changed by others");
				}
			}
			
			IUser user2 = UserHelper.updateLocalUser(username, hostkey, password, user.getUserFlag());
			if (user != user2) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"User: " + username + " update wrong");
			}
			
			String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" changed your password");
			MessageHelper.notifyLog(user.getUserKey(), String.format(text, client.getUser().getUserName()));
			
			NamedMap<Object> info = (user instanceof IGroup) ? 
					NamedHelper.toNamedMap((IGroup)user, 0, 0, null) : 
						NamedHelper.toNamedMap((IMember)user, 0, 0, null);
			
			if (info != null) rsp.add("user", info);
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"User did not login");
		}
	}
	
	private void handleUpdateFlag(Request req, Response rsp) 
			throws ErrorException { 
		if (req == null || rsp == null)
			return;
		
		@SuppressWarnings("unused")
		IMember adm = UserHelper.checkAdmin(req, IUserClient.Op.ACCESS);
		
		IUserName uname = getCore().getClusterSelf().parseUserName(req.getParam("username"));
		String username = uname != null ? uname.getUserName() : null;
		String userflag = trim(req.getParam("flag"));
		
		int newflag = IUser.Util.parseFlag(userflag);
		String hostkey = getCore().getHostSelf().getHostKey();
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		if (username.equals(getAdminConfig().getAdminUser())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Administrator's flag cannot be changed");
		}
		
		IUser user = UserHelper.updateLocalUser(username, hostkey, null, newflag);
		if (user != null) { 
			NamedMap<Object> info = (user instanceof IGroup) ? 
					NamedHelper.toNamedMap((IGroup)user, 0, 0, null) : 
						NamedHelper.toNamedMap((IMember)user, 0, 0, null);
			
			//String text = Strings.get(user.getPreference().getLanguage(), "\"%1$s\" update your flag to \"%2$s\"");
			//MessageHelper.notify(user.getUserKey(), String.format(text, adm.getUserName(), newflag));
				
			if (info != null) rsp.add("user", info);
			
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"User: " + username + " not found");
		}
	}
	
	private void handleCheck(Request req, Response rsp) 
			throws ErrorException { 
		if (req == null || rsp == null)
			return;
		
		IUserClient client = UserHelper.checkUserClient(req, null);
		HostMode hostMode = getCore().getCluster().getHostSelf().getHostMode();
		
		if (client == null && hostMode == HostMode.ATTACH) {
			IAuthInfo authInfo = getCore().getCluster().attachAuth(req);
			if (authInfo != null) {
				IMember user = authInfo.getUser();
				IAttachUserInfo attachUser = authInfo.getAttachUser();
				IHostInfo attachHost = authInfo.getAttachHost();
				
				String token = attachUser.getToken();
				String action = "attachauth";
				
				String lang = trim(req.getParam("lang"));
				String theme = trim(req.getParam("theme"));
				String clientkey = trim(req.getParam("clientkey"));
				String devicekey = trim(req.getParam("devicekey"));
				String authkey = trim(req.getParam("authkey"));
				String apptype = trim(req.getParam("apptype"));
				String appname = trim(req.getParam("appname"));
				String appversion = trim(req.getParam("appversion"));
				String applang = trim(req.getParam("applang"));
				String appaddr = trim(req.getParam("userip"));
				String appagent = trim(req.getParam("useragent"));
				String rembme = trim(req.getParam("rememberMe"));
				
				boolean rememberMe = false;
				if (rembme != null && (rembme.equalsIgnoreCase("true") || rembme.equals("1")))
					rememberMe = true;
				
				if (clientkey == null || clientkey.length() == 0)
					clientkey = attachHost.getHostKey();
				
				if (devicekey == null || devicekey.length() == 0)
					devicekey = attachUser.getDeviceKey();
				
				if (appaddr == null || appaddr.length() == 0)
					appaddr = trim(req.getRemoteAddr());
				
				if (appagent == null || appagent.length() == 0)
					appagent = trim(req.getUserAgent());
				
				if (apptype == null || apptype.length() == 0)
					apptype = attachUser.getClient();
				
				if (appname == null || appname.length() == 0)
					appname = "unknown";
				
				if (appversion == null || appversion.length() == 0)
					appversion = "unknown";
				
				if (applang == null || applang.length() == 0)
					applang = "";
				
				if (appaddr == null || appaddr.length() == 0)
					appaddr = "";
				
				if (appagent == null || appagent.length() == 0)
					appagent = "";
				
				if (lang == null || lang.length() == 0) 
					lang = applang;
				
				if (user.getUserFlag() == IUser.FLAG_DISABLED) {
					throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
							"User: " + user.getUserName() + " is disabled and cannot login");
				}
				
				client = createClient(user, lang, theme, 
						clientkey, devicekey, authkey, apptype, appname, appversion, applang, appaddr, appagent, 
						action, rememberMe, token);
				
				String text = Strings.get(user.getPreference().getLanguage(), "Authorized from \"%1$s\" by \"%2$s\"");
				MessageHelper.notifyLog(user.getUserKey(), 
						String.format(text, appaddr, client.getDevice().toReadableTitle()), 
						client.getDevice().toReadableString());
			}
		}
		
		if (client != null) { 
			UserHeartbeatHandler.handleAction(req, rsp, getCore(), client, 0, 0, 0, 
					IUser.ShowType.ALL);
			
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"User did not login");
		}
	}
	
	private void handleHttpAction(Request req, Response rsp, 
			String action, IHostInfo host) throws ErrorException { 
		if (req == null || rsp == null || action == null || host == null)
			return;
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String url = "http://" + hostAddress + "/lightning/user/login?wt=secretjson&action=" 
				+ HostHelper.encode(action);
		
		Iterator<String> it = req.getParams().getParameterNamesIterator();
		while (it.hasNext()) {
			String name = it.next();
			if (name == null || name.equalsIgnoreCase("wt") || 
				name.equalsIgnoreCase("action") || 
				name.equalsIgnoreCase("requestfrom")) {
				continue;
			}
			
			String[] values = req.getParams(name);
			if (values == null || values.length == 0)
				continue;
			
			for (String value : values) {
				url += "&" + name + "=" + HostHelper.encode(value);
			}
		}
		
		url += "&requestfrom=" + HostHelper.encode(getCore().getHostSelf().getHostKey());
		
		AnyboxListener.LoginListener listener = new AnyboxListener.LoginListener();
		CoreCluster.fetch(url, listener);
		
		AnyboxData data = listener.getData();
		if (data == null) {
			ActionError error = listener.getError();
			if (error != null) {
				throw new ErrorException(ErrorException.ErrorCode.getErrorCode(error.getCode()), 
						error.getMessage());
			}
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Host: " + host.getHostKey() + " response null");
		}
		
		data.copyTo(rsp);
	}
	
	private void handleAction(Request req, Response rsp, 
			String action) throws ErrorException { 
		if (req == null || rsp == null || action == null)
			return;
		
		IUserName uname = getCore().getClusterSelf().parseUserName(req.getParam("username"));
		String username = uname != null ? uname.getUserName() : null;
		boolean isAdminUser = username != null ? username.equals(getAdminConfig().getAdminUser()) : null;
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		String reqfrom = trim(req.getParam("requestfrom"));
		IHostCluster cluster = getCore().getClusterSelf(); 
		
		if (reqfrom == null || reqfrom.length() == 0) {
			if (uname != null && username != null && !isAdminUser) {
				IHostUserName userName = cluster.getHostUserName(uname);
				IHostInfo userHost = userName != null ? userName.getHostNode() : null;
				
				if (userName == null || userHost == null) {
					throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
							"Host of user: " + reqfrom + " not found");
				}
				
				if (!userHost.getHostKey().equals(getCore().getHostSelf().getHostKey())) {
					handleHttpAction(req, rsp, action, userHost);
					return;
				}
			}
		} else {
			IHostNode fromHost = cluster != null ? cluster.getHostByKey(reqfrom) : null;
			if (fromHost == null) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Request host: " + reqfrom + " not found");
			}
		}
		
		HostMode hostMode = getCore().getHostSelf().getHostMode();
		String hostkey = getCore().getHostSelf().getHostKey();
		
		if (hostMode == HostMode.ATTACH) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Can not login or register in " + hostMode + " mode");
		}
		
		String password = req.getParam("password");
		String secret = req.getParam("secret");
		String email = trim(req.getParam("email"));
		String lang = trim(req.getParam("lang"));
		String theme = trim(req.getParam("theme"));
		String clientkey = trim(req.getParam("clientkey"));
		String devicekey = trim(req.getParam("devicekey"));
		String authkey = trim(req.getParam("authkey"));
		String apptype = trim(req.getParam("apptype"));
		String appname = trim(req.getParam("appname"));
		String appversion = trim(req.getParam("appversion"));
		String applang = trim(req.getParam("applang"));
		String appaddr = trim(req.getParam("userip"));
		String appagent = trim(req.getParam("useragent"));
		String category = trim(req.getParam("category"));
		String rembme = trim(req.getParam("rememberMe"));
		
		boolean rememberMe = false;
		if (rembme != null && (rembme.equalsIgnoreCase("true") || rembme.equals("1")))
			rememberMe = true;
		
		if (appaddr == null || appaddr.length() == 0)
			appaddr = trim(req.getRemoteAddr());
		
		if (appagent == null || appagent.length() == 0)
			appagent = trim(req.getUserAgent());
		
		if (apptype == null || apptype.length() == 0)
			apptype = "";
		
		if (appname == null || appname.length() == 0)
			appname = "unknown";
		
		if (appversion == null || appversion.length() == 0)
			appversion = "unknown";
		
		if (applang == null || applang.length() == 0)
			applang = "";
		
		if (appaddr == null || appaddr.length() == 0)
			appaddr = "";
		
		if (appagent == null || appagent.length() == 0)
			appagent = "";
		
		if (lang == null || lang.length() == 0) 
			lang = applang;
		
		if (action.equalsIgnoreCase("login")) { 
			if (getAdminSetting().getGlobal().isLoginDisabled() && !isAdminUser) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Login is disabled now");
			}
			
			if (password == null || password.length() == 0) { 
				if (secret != null && secret.length() > 0) {
					password = AuthHelper.decodeSecret(secret);
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("handleAction: decodeSecret: secret=" + secret 
								+ " result=" + password);
					}
				}
			}
			
			if (password == null || password.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Password cannot be empty");
			}
			
			IMember user = UserHelper.authLocalUser(username, password);
			if (user != null) {
				username = user.getUserName();
				if (user.getUserFlag() == IUser.FLAG_DISABLED) {
					throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
							"User: " + username + " is disabled and cannot login");
				}
				
				if (IUser.ATTACHUSER.equals(user.getPreference().getCategory())) {
					throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
							"User: " + username + " is attachuser and cannot login");
				}
				
				IUserClient client = createClient(user, lang, theme, 
						clientkey, devicekey, authkey, apptype, appname, appversion, applang, appaddr, appagent, 
						action, rememberMe, null);
				
				String text = Strings.get(user.getPreference().getLanguage(), "Login from \"%1$s\" by \"%2$s\"");
				MessageHelper.notifyLog(user.getUserKey(), 
						String.format(text, appaddr, client.getDevice().toReadableTitle()), 
						client.getDevice().toReadableString());
				
				UserHeartbeatHandler.handleAction(req, rsp, getCore(), client, 0, 0, 0, IUser.ShowType.ALL);
			} else { 
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Username or password is wrong");
			}
			
		} else if (action.equalsIgnoreCase("register") || action.equalsIgnoreCase("registerlogin")) { 
			if (getAdminSetting().getGlobal().isRegisterDisabled()) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Register is disabled now");
			}
			
			if (password == null || password.length() == 0) { 
				if (secret != null && secret.length() > 0) {
					password = AuthHelper.decodeSecret(secret);
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("handleAction: decodeSecret: secret=" + secret 
								+ " result=" + password);
					}
				}
			}
			
			if (password == null || password.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Password cannot be empty");
			}
			
			checkUsername(username, appaddr);
			checkEmail(getCore().getClusterSelf(), username, email);
			checkPassword(password);
			
			IMember user = UserHelper.registerLocalUser(username, password, category, hostkey);
			if (user != null) {
				initProfile(getCore().getClusterSelf(), user, hostkey, email);
				if (!isAdminUser) {
					cluster.addUser(username, user.getUserKey(), hostkey, password, 
							user.getUserFlag(), user.getUserType(), null);
				}
				
				if (action.equalsIgnoreCase("registerlogin")) {
					IUserClient client = createClient(user, lang, theme, 
							clientkey, devicekey, authkey, apptype, appname, appversion, applang, appaddr, appagent, 
							action, rememberMe, null);
					
					String text = Strings.get(user.getPreference().getLanguage(), "Registered from \"%1$s\" by \"%2$s\"");
					MessageHelper.notifyLog(user.getUserKey(), 
							String.format(text, appaddr, client.getDevice().toReadableTitle()), 
							client.getDevice().toReadableString());
					
					UserHeartbeatHandler.handleAction(req, rsp, getCore(), client, 0, 0, 0, IUser.ShowType.ALL);
				} else { 
					NamedList<Object> info = NamedHelper.toNamedMap(user, 0, 0, null);
					NamedList<Object> setting = UserHeartbeatHandler.getSettingInfo(user, null);

					rsp.add("user", info);
					rsp.add("setting", setting);
				}
			} else { 
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"User register failed");
			}
		} else if (action.equalsIgnoreCase("authlogin")) {
			if (getAdminSetting().getGlobal().isLoginDisabled() && !isAdminUser) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Login is disabled now");
			}
			
			if (apptype == null || apptype.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Device type cannot be empty");
			}
			
			if (clientkey == null || clientkey.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Client key cannot be empty");
			}
			
			if (devicekey == null || devicekey.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Device key cannot be empty");
			}
			
			if (authkey == null || authkey.length() == 0) { 
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Auth key cannot be empty");
			}
			
			IUser usr = UserHelper.getLocalUserByName(username);
			if (usr == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"User: " + username + " not found");
			}
			
			username = usr.getUserName();
			if (usr.getUserFlag() == IUser.FLAG_DISABLED) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"User: " + username + " is disabled and cannot login");
			}
			if (!(usr instanceof IMember)) {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"Group: " + username + " cannot login");
			}
			
			if (IUser.ATTACHUSER.equals(usr.getPreference().getCategory())) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"User: " + username + " is attachuser and cannot login");
			}
			
			IMember user = (IMember)usr;
			DeviceManager manager = user.getDeviceManager();
			
			if (manager != null) {
				manager.loadDevices(false);
				
				final DeviceGroup group = manager.getDeviceGroup(apptype);
				if (group == null) { 
					throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
							"Device type: " + apptype + " not supported");
				}
				
				final Device dev = group.getDevice(devicekey);
				if (dev == null) {
					throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
							"Device: " + devicekey + " of type: " + apptype + " not found");
				}
				
				if (clientkey == null || !clientkey.equals(dev.getClientKey())) {
					throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
							"Wrong device client key for user: " + username);
				}
				
				if (authkey == null || !authkey.equals(dev.getAuthKey())) {
					throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
							"Wrong device auth key for user: " + username);
				}
				
				final long actionTime = dev.getActionTime();
				final long expireTime = System.currentTimeMillis() - actionTime;
				if (expireTime > Device.EXPIRED_MILLIS) {
					String fromstr = TimeUtils.formatDate(actionTime);
					throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
							"Device auth key expired from " + fromstr + 
							" (" + expireTime + " ms > " + Device.EXPIRED_MILLIS + 
							" ms) for user: " + username);
				}
				
				IUserClient client = createClient(user, lang, theme, 
						clientkey, devicekey, authkey, apptype, appname, appversion, applang, appaddr, appagent, 
						action, rememberMe, null);
				
				String text = Strings.get(user.getPreference().getLanguage(), "Login from \"%1$s\" by \"%2$s\"");
				MessageHelper.notifyLog(user.getUserKey(), 
						String.format(text, appaddr, client.getDevice().toReadableTitle()), 
						client.getDevice().toReadableString());
				
				UserHeartbeatHandler.handleAction(req, rsp, getCore(), client, 0, 0, 0, IUser.ShowType.ALL);
			} else {
				throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
						"User: " + username + " has no devices");
			}
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.UNAUTHORIZED, 
					"Unknown action: " + action);
		}
	}
	
	static void initProfile(IHostCluster cluster, IUser user, 
			String hostkey, String email) throws ErrorException {
		initProfile(cluster, user, hostkey, email, 
				null, null, null, null, null);
	}
	
	static void initProfile(IHostCluster cluster, IUser user, 
			String hostkey, String email, String attach_hostkey, 
			String attach_hostname, String attach_userkey, String attach_username, 
			String attach_mailaddr) throws ErrorException { 
		if (cluster == null || user == null) throw new NullPointerException();
		
		Profile profile = user.getProfile();
		if (profile != null) { 
			profile.loadProfile(false);
			
			profile.setUserName(user.getUserName(), false);
			profile.setEmail(email, false);
			profile.setAttachHostKey(attach_hostkey, false);
			profile.setAttachHostName(attach_hostname, false);
			profile.setAttachUserKey(attach_userkey, false);
			profile.setAttachUserName(attach_username, false);
			profile.setAttachUserEmail(attach_mailaddr, false);
			profile.saveProfile();
		}
		
		if (email != null && email.length() > 0) {
			cluster.updateName(email, user.getUserName(), hostkey, 
					IUser.NAME_EMAIL, null, null);
		}
	}
	
	private IUserClient createClient(IMember user, String lang, String theme, 
			String clientkey, String devicekey, String authkey, 
			String apptype, String appname, String appversion, 
			String applang, String appaddr, String appagent, String action, 
			final boolean rememberMe, String token) throws ErrorException { 
		if (user == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User is null");
		}
		
		if (token == null || token.length() == 0)
			token = UserHelper.newClientToken(user.getUserName());
		
		DeviceManager manager = user.getDeviceManager();
		IUserClient.Factory factory = null;
		
		if (manager != null) {
			manager.loadDevices(false);
			
			final DeviceType type = manager.getDeviceType(apptype);
			if (type == null) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Device type: " + apptype + " not supported");
			}
			
			if (type instanceof WebDeviceType) {
				final WebDeviceType webtype = (WebDeviceType)type;
				final WebDevice dev = new WebDevice(webtype, devicekey, appname, appversion);
				
				dev.setLanguage(applang);
				dev.setUserAgent(appagent);
				dev.setUserAddr(appaddr);
				dev.setAction(action);
				dev.setAuthKey(UserHelper.newAuthToken(appversion));
				dev.setActionTime(System.currentTimeMillis());
				
				if (clientkey != null && clientkey.length() > 0)
					dev.setClientKey(clientkey);
				if (lang != null && lang.length() > 0)
					webtype.setLanguage(lang);
				if (theme != null && theme.length() > 0)
					webtype.setTheme(theme);
				
				if (user.getUserFlag() == IUser.FLAG_ENABLED) {
					manager.addDevice(dev);
					manager.saveDevices();
					manager.close();
				} else { 
					if (LOG.isWarnEnabled()) {
						LOG.warn("createClient: user is locked as " 
								+ IUser.Util.stringOfFlag(user.getUserFlag()) 
								+ ", cannot save device: " + dev);
					}
				}
				
				factory = new IUserClient.Factory() {
						@Override
						public IUserClient create(IMember user, String token) 
								throws ErrorException {
							WebUserClient webclient = new WebUserClient((Member)user, dev, token);
							webclient.setRememberMe(rememberMe);
							return webclient;
						}
					};
			} else if (type instanceof AndroidDeviceType) {
				final AndroidDeviceType androidtype = (AndroidDeviceType)type;
				final AndroidDevice dev = new AndroidDevice(androidtype, devicekey, appname, appversion);
				
				dev.setLanguage(applang);
				dev.setUserAgent(appagent);
				dev.setUserAddr(appaddr);
				dev.setAction(action);
				dev.setAuthKey(UserHelper.newAuthToken(appversion));
				dev.setActionTime(System.currentTimeMillis());
				
				if (clientkey != null && clientkey.length() > 0)
					dev.setClientKey(clientkey);
				if (lang != null && lang.length() > 0)
					androidtype.setLanguage(lang);
				
				if (user.getUserFlag() == IUser.FLAG_ENABLED) {
					manager.addDevice(dev);
					manager.saveDevices();
					manager.close();
				} else { 
					if (LOG.isWarnEnabled()) {
						LOG.warn("createClient: user is locked as " 
								+ IUser.Util.stringOfFlag(user.getUserFlag()) 
								+ ", cannot save device: " + dev);
					}
				}
				
				factory = new IUserClient.Factory() {
						@Override
						public IUserClient create(IMember user, String token) 
								throws ErrorException {
							AndroidUserClient androidclient = new AndroidUserClient((Member)user, dev, token);
							//androidclient.setRememberMe(rememberMe);
							return androidclient;
						}
					};
			} else if (type instanceof IOSDeviceType) {
				final IOSDeviceType androidtype = (IOSDeviceType)type;
				final IOSDevice dev = new IOSDevice(androidtype, devicekey, appname, appversion);
				
				dev.setLanguage(applang);
				dev.setUserAgent(appagent);
				dev.setUserAddr(appaddr);
				dev.setAction(action);
				dev.setAuthKey(UserHelper.newAuthToken(appversion));
				dev.setActionTime(System.currentTimeMillis());
				
				if (clientkey != null && clientkey.length() > 0)
					dev.setClientKey(clientkey);
				if (lang != null && lang.length() > 0)
					androidtype.setLanguage(lang);
				
				if (user.getUserFlag() == IUser.FLAG_ENABLED) {
					manager.addDevice(dev);
					manager.saveDevices();
					manager.close();
				} else { 
					if (LOG.isWarnEnabled()) {
						LOG.warn("createClient: user is locked as " 
								+ IUser.Util.stringOfFlag(user.getUserFlag()) 
								+ ", cannot save device: " + dev);
					}
				}
				
				factory = new IUserClient.Factory() {
						@Override
						public IUserClient create(IMember user, String token) 
								throws ErrorException {
							IOSUserClient androidclient = new IOSUserClient((Member)user, dev, token);
							//androidclient.setRememberMe(rememberMe);
							return androidclient;
						}
					};
			} else if (type instanceof WinDeviceType) {
				final WinDeviceType androidtype = (WinDeviceType)type;
				final WinDevice dev = new WinDevice(androidtype, devicekey, appname, appversion);
				
				dev.setLanguage(applang);
				dev.setUserAgent(appagent);
				dev.setUserAddr(appaddr);
				dev.setAction(action);
				dev.setAuthKey(UserHelper.newAuthToken(appversion));
				dev.setActionTime(System.currentTimeMillis());
				
				if (clientkey != null && clientkey.length() > 0)
					dev.setClientKey(clientkey);
				if (lang != null && lang.length() > 0)
					androidtype.setLanguage(lang);
				
				if (user.getUserFlag() == IUser.FLAG_ENABLED) {
					manager.addDevice(dev);
					manager.saveDevices();
					manager.close();
				} else { 
					if (LOG.isWarnEnabled()) {
						LOG.warn("createClient: user is locked as " 
								+ IUser.Util.stringOfFlag(user.getUserFlag()) 
								+ ", cannot save device: " + dev);
					}
				}
				
				factory = new IUserClient.Factory() {
						@Override
						public IUserClient create(IMember user, String token) 
								throws ErrorException {
							WinUserClient androidclient = new WinUserClient((Member)user, dev, token);
							//androidclient.setRememberMe(rememberMe);
							return androidclient;
						}
					};
			} else { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Device type: " + apptype + " not supported");
			}
		}
		
		return user.getClient(token, factory);
	}
	
}
