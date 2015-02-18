package org.javenstudio.lightning.core.user;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.setting.cluster.AttachHostUser;
import org.javenstudio.falcon.setting.cluster.AttachUser;
import org.javenstudio.falcon.setting.cluster.HostMode;
import org.javenstudio.falcon.setting.cluster.IAttachUser;
import org.javenstudio.falcon.setting.cluster.IHostCluster;
import org.javenstudio.falcon.setting.cluster.IHostInfo;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IMember;
import org.javenstudio.falcon.user.INameData;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserClient;
import org.javenstudio.falcon.user.IUserData;
import org.javenstudio.falcon.user.NamedHelper;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.device.DeviceManager;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.lightning.core.CoreCluster;
import org.javenstudio.lightning.core.datum.DatumDashboardHandler;
import org.javenstudio.lightning.handler.RequestHandler;
import org.javenstudio.lightning.request.Request;
import org.javenstudio.lightning.response.Response;
import org.javenstudio.util.StringUtils;

public class UserClusterHandler extends UserHandlerBase {
	private static final Logger LOG = Logger.getLogger(UserClusterHandler.class);

	public static RequestHandler createHandler(UserCore core) { 
		return new UserClusterHandler(core);
	}
	
	public UserClusterHandler(UserCore core) { 
		super(core);
	}
	
	@Override
	public void handleRequestBody(Request req, Response rsp)
			throws ErrorException {
		String action = trim(req.getParam("action"));
		
		if (action == null || action.length() == 0)
			action = "list";
		
		if (action.equalsIgnoreCase("list")) { 
			rsp.add("action", action);
			rsp.add("clusterid", getCore().getCluster().getHostSelf().getClusterId());
			rsp.add("scheme", req.getRequestInput().getScheme());
			
			IMember user = UserHelper.checkAdmin(req, IUserClient.Op.ACCESS);
			handleList(req, rsp, user);
			
		} else if (action.equalsIgnoreCase("cluster")) { 
			String clusterId = req.getParam("clusterid");
			
			rsp.add("action", action);
			rsp.add("clusterid", toString(clusterId));
			rsp.add("scheme", req.getRequestInput().getScheme());
			
			IMember user = UserHelper.checkAdmin(req, IUserClient.Op.ACCESS);
			handleCluster(req, rsp, user, clusterId);
			
		} else if (action.equalsIgnoreCase("invite")) { 
			rsp.add("action", action);
			rsp.add("clusterid", getCore().getCluster().getHostSelf().getClusterId());
			rsp.add("scheme", req.getRequestInput().getScheme());
			
			IMember user = UserHelper.checkAdmin(req, IUserClient.Op.ACCESS);
			handleInvite(req, rsp, user);
			
		} else if (action.equalsIgnoreCase("join")) { 
			rsp.add("action", action);
			handleJoinAttach(req, rsp, false);
			
		} else if (action.equalsIgnoreCase("attach")) { 
			rsp.add("action", action);
			handleJoinAttach(req, rsp, true);
			
		} else if (action.equalsIgnoreCase("get")) { 
			rsp.add("action", action);
			handleHostGet(req, rsp);
			
		} else if (action.equalsIgnoreCase("joinget")) { 
			rsp.add("action", action);
			handleJoinGet(req, rsp);
			
		} else if (action.equalsIgnoreCase("attachget")) { 
			rsp.add("action", action);
			handleAttachGet(req, rsp);
			
		} else if (action.equalsIgnoreCase("getuser")) { 
			rsp.add("action", action);
			handleGetUser(req, rsp);
			
		} else if (action.equalsIgnoreCase("putuser")) { 
			rsp.add("action", action);
			handlePutUser(req, rsp);
			
		} else if (action.equalsIgnoreCase("rmuser")) { 
			rsp.add("action", action);
			handleRemoveUser(req, rsp);
			
		} else if (action.equalsIgnoreCase("getname")) { 
			rsp.add("action", action);
			handleGetName(req, rsp);
			
		} else if (action.equalsIgnoreCase("putname")) { 
			rsp.add("action", action);
			handlePutName(req, rsp);
			
		} else if (action.equalsIgnoreCase("rmname")) { 
			rsp.add("action", action);
			handleRemoveName(req, rsp);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Unsupported Action: " + action);
		}
	}
	
	private void handleRemoveUser(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		HostMode selfMode = getCore().getHostSelf().getHostMode();
		if (selfMode != HostMode.HOST && selfMode != HostMode.NAMED) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Remove operation not supported for host mode: " + selfMode); 
		}
		
		String username = trim(req.getParam("username"));
		String reqfrom = trim(req.getParam("requestfrom"));
		String reqip = req.getRemoteAddr();
		
		IHostNode host = null;
		if (reqfrom != null) {
			IHostNode node = getCore().getClusterSelf().getHostByKey(reqfrom);
			if (node != null && node.getHostAddress().equals(reqip)) {
				if (node.getHostMode() != HostMode.ATTACH)
					host = node;
			}
		}
		if (host == null) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Request denied from wrong ip: " + reqip);
		}
		
		IUserData userdata = getManager().getService().searchUser(username);
		if (userdata != null) {
			if (userdata.getUserType() != IUser.TYPE_NAMED_USER && 
				userdata.getUserType() != IUser.TYPE_NAMED_GROUP) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"User: " + username + " data already existed");
			}
			
			userdata = getManager().getService().removeUser(username);
			
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + username + " data not found");
		}
		
		if (userdata != null) {
			NamedList<Object> info = getUserInfo(userdata);
			rsp.add("userdata", info);
		}
		
		NamedList<Object> hostSelf = getHostInfo(getCore().getHostSelf(), null);
		rsp.add("hostself", hostSelf);
		
		rsp.add("username", username);
		rsp.add("result", (userdata != null));
	}
	
	private void handlePutUser(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		HostMode selfMode = getCore().getHostSelf().getHostMode();
		if (selfMode != HostMode.HOST && selfMode != HostMode.NAMED) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Put operation not supported for host mode: " + selfMode); 
		}
		
		String username = trim(req.getParam("username"));
		String reqfrom = trim(req.getParam("requestfrom"));
		String reqip = req.getRemoteAddr();
		
		IHostNode host = null;
		if (reqfrom != null) {
			IHostNode node = getCore().getClusterSelf().getHostByKey(reqfrom);
			if (node != null && node.getHostAddress().equals(reqip)) {
				if (node.getHostMode() != HostMode.ATTACH)
					host = node;
			}
		}
		if (host == null) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Request denied from wrong ip: " + reqip);
		}
		
		String userkey = trim(req.getParam("userkey"));
		String hostkey = trim(req.getParam("hostkey"));
		int userflag = parseInt(req.getParam("flag"));
		int usertype = parseInt(req.getParam("type"));
		String[] attrlist = req.getParams("attr");
		
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User name is empty");
		}
		if (userkey == null || userkey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User key is empty");
		}
		if (hostkey == null || hostkey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host key is empty");
		}
		if (usertype != IUser.TYPE_NAMED_USER && usertype != IUser.TYPE_NAMED_GROUP) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User type: " + usertype + " is wrong");
		}
		
		Map<String,String> attrs = null;
		if (attrlist != null && attrlist.length > 0) {
			attrs = new HashMap<String,String>();
			
			for (String attrstr : attrlist) {
				if (attrstr == null || attrstr.length() == 0)
					continue;
				
				int pos = attrstr.indexOf('=');
				if (pos > 0) {
					String name = attrstr.substring(0, pos);
					String value = attrstr.substring(pos+1);
					
					if (name != null && name.length() > 0 && 
						value != null && value.length() > 0) {
						attrs.put(name, value);
					}
				}
			}
		}
		
		IUserData userdata = getManager().getService().searchUser(username);
		if (userdata != null) {
			if (userdata.getUserType() != IUser.TYPE_NAMED_USER && 
				userdata.getUserType() != IUser.TYPE_NAMED_GROUP) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"User: " + username + " data already existed");
			}
			
			userdata = getManager().getService().updateUser(username, 
					hostkey, null, userflag, attrs);
			
		} else {
			userdata = getManager().getService().addUser(username, 
					userkey, hostkey, "000000", userflag, usertype, attrs);
		}
		
		if (userdata != null) {
			NamedList<Object> info = getUserInfo(userdata);
			rsp.add("userdata", info);
		}
		
		NamedList<Object> hostSelf = getHostInfo(getCore().getHostSelf(), null);
		rsp.add("hostself", hostSelf);
		
		rsp.add("username", username);
		rsp.add("result", (userdata != null));
	}
	
	private void handleGetUser(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		String username = trim(req.getParam("username"));
		String reqfrom = trim(req.getParam("requestfrom"));
		String reqip = req.getRemoteAddr();
		
		IHostNode host = null;
		if (reqfrom != null) {
			IHostNode node = getCore().getClusterSelf().getHostByKey(reqfrom);
			if (node != null && node.getHostAddress().equals(reqip)) {
				if (node.getHostMode() != HostMode.ATTACH)
					host = node;
			}
		}
		if (host == null) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Request denied from wrong ip: " + reqip);
		}
		
		IUserData data = getManager().getService().searchUser(username);
		boolean result = false;
		
		if (data != null) {
			NamedList<Object> info = getUserInfo(data);
			
			rsp.add("userdata", info);
			username = data.getUserName();
			result = true;
		}
		
		NamedList<Object> hostSelf = getHostInfo(getCore().getHostSelf(), null);
		rsp.add("hostself", hostSelf);
		
		rsp.add("username", username);
		rsp.add("result", result);
	}
	
	static NamedList<Object> getUserInfo(IUserData data) {
		NamedList<Object> info = new NamedMap<Object>();
		
		if (data != null) {
			info.add("key", toString(data.getUserKey()));
			info.add("name", toString(data.getUserName()));
			info.add("hostkey", toString(data.getHostKey()));
			info.add("flag", data.getUserFlag());
			info.add("type", data.getUserType());
			
			String[] attrNames = data.getAttrNames();
			if (attrNames != null && attrNames.length > 0) {
				NamedList<Object> attrs = new NamedMap<Object>();
				for (String attrName : attrNames) {
					String value = data.getAttr(attrName);
					if (value != null && value.length() > 0)
						attrs.add(attrName, value);
				}
				info.add("attrs", attrs);
			}
		}
		
		return info;
	}
	
	private void handlePutName(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		HostMode selfMode = getCore().getHostSelf().getHostMode();
		if (selfMode != HostMode.HOST && selfMode != HostMode.NAMED) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Put operation not supported for host mode: " + selfMode); 
		}
		
		String namekey = trim(req.getParam("name"));
		String reqfrom = trim(req.getParam("requestfrom"));
		String reqip = req.getRemoteAddr();
		
		IHostNode host = null;
		if (reqfrom != null) {
			IHostNode node = getCore().getClusterSelf().getHostByKey(reqfrom);
			if (node != null && node.getHostAddress().equals(reqip)) {
				if (node.getHostMode() != HostMode.ATTACH)
					host = node;
			}
		}
		if (host == null) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Request denied from wrong ip: " + reqip);
		}
		
		String namevalue = trim(req.getParam("value"));
		String hostkey = trim(req.getParam("hostkey"));
		int nameflag = parseInt(req.getParam("flag"));
		String[] attrlist = req.getParams("attr");
		
		if (namekey == null || namekey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Name key is empty");
		}
		if (namevalue == null || namevalue.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Name value is empty");
		}
		if (hostkey == null || hostkey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host key is empty");
		}
		
		Map<String,String> attrs = null;
		if (attrlist != null && attrlist.length > 0) {
			attrs = new HashMap<String,String>();
			
			for (String attrstr : attrlist) {
				if (attrstr == null || attrstr.length() == 0)
					continue;
				
				int pos = attrstr.indexOf('=');
				if (pos > 0) {
					String name = attrstr.substring(0, pos);
					String value = attrstr.substring(pos+1);
					
					if (name != null && name.length() > 0 && 
						value != null && value.length() > 0) {
						attrs.put(name, value);
					}
				}
			}
		}
		
		INameData namedata = getManager().getService().updateName(namekey, namevalue, 
				hostkey, nameflag, attrs);
		
		NamedList<Object> hostSelf = getHostInfo(getCore().getHostSelf(), null);
		rsp.add("hostself", hostSelf);
		
		rsp.add("name", namekey);
		rsp.add("result", (namedata != null));
	}
	
	private void handleRemoveName(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		HostMode selfMode = getCore().getHostSelf().getHostMode();
		if (selfMode != HostMode.HOST && selfMode != HostMode.NAMED) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Remove operation not supported for host mode: " + selfMode); 
		}
		
		String namekey = trim(req.getParam("name"));
		String reqfrom = trim(req.getParam("requestfrom"));
		String reqip = req.getRemoteAddr();
		
		IHostNode host = null;
		if (reqfrom != null) {
			IHostNode node = getCore().getClusterSelf().getHostByKey(reqfrom);
			if (node != null && node.getHostAddress().equals(reqip)) {
				if (node.getHostMode() != HostMode.ATTACH)
					host = node;
			}
		}
		if (host == null) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Request denied from wrong ip: " + reqip);
		}
		
		if (namekey == null || namekey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Name key is empty");
		}
		
		INameData namedata = getManager().getService().removeName(namekey);
		if (namedata != null) {
			NamedList<Object> info = getNameInfo(namedata);
			rsp.add("namedata", info);
		}
		
		NamedList<Object> hostSelf = getHostInfo(getCore().getHostSelf(), null);
		rsp.add("hostself", hostSelf);
		
		rsp.add("name", namekey);
		rsp.add("result", (namedata != null));
	}
	
	private void handleGetName(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		String name = trim(req.getParam("name"));
		String reqfrom = trim(req.getParam("requestfrom"));
		String reqip = req.getRemoteAddr();
		
		IHostNode host = null;
		if (reqfrom != null) {
			IHostNode node = getCore().getClusterSelf().getHostByKey(reqfrom);
			if (node != null && node.getHostAddress().equals(reqip)) {
				if (node.getHostMode() != HostMode.ATTACH)
					host = node;
			}
		}
		if (host == null) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Request denied from wrong ip: " + reqip);
		}
		
		INameData data = getManager().getService().searchName(name);
		boolean result = false;
		
		if (data != null) {
			NamedList<Object> info = getNameInfo(data);
			
			rsp.add("namedata", info);
			result = true;
		}
		
		NamedList<Object> hostSelf = getHostInfo(getCore().getHostSelf(), null);
		rsp.add("hostself", hostSelf);
		
		rsp.add("name", name);
		rsp.add("result", result);
	}
	
	static NamedList<Object> getNameInfo(INameData data) {
		NamedList<Object> info = new NamedMap<Object>();
		
		if (data != null) {
			info.add("key", toString(data.getNameKey()));
			info.add("value", toString(data.getNameValue()));
			info.add("hostkey", toString(data.getHostKey()));
			info.add("flag", data.getNameFlag());
			
			String[] attrNames = data.getAttrNames();
			if (attrNames != null && attrNames.length > 0) {
				NamedList<Object> attrs = new NamedMap<Object>();
				for (String attrName : attrNames) {
					String value = data.getAttr(attrName);
					if (value != null && value.length() > 0)
						attrs.add(attrName, value);
				}
				info.add("attrs", attrs);
			}
		}
		
		return info;
	}
	
	private void checkClient(Request req) throws ErrorException {
		if (req == null) return;
		
		String apptype = trim(req.getParam("apptype"));
		String appkey = trim(req.getParam("appkey"));
		
		if (apptype == null || apptype.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"App type cannot be empty");
		}
		
		if (appkey == null || appkey.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"App key cannot be empty");
		}
		
		if (!DeviceManager.hasDeviceType(apptype)) {
			throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
					"Device type: " + apptype + " not supported");
		}
		
		if (LOG.isInfoEnabled()) {
			LOG.info("checkClient: apptype=" + apptype + " appkey=" + appkey 
					+ " remoteaddr=" + req.getRemoteAddr());
		}
	}
	
	private void handleJoinAttach(Request req, Response rsp, boolean attach) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		CoreCluster cc = getCore().getCluster();
		HostMode hostMode = cc.getHostSelf().getHostMode();
		
		if (attach) {
			if (hostMode != HostMode.HOST && hostMode != HostMode.NAMED && hostMode != HostMode.JOIN) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Can not attach host in " + hostMode + " mode");
			}
		} else {
			if (hostMode != HostMode.HOST && hostMode != HostMode.NAMED) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Can not join host in " + hostMode + " mode");
			}
		}
		
		checkClient(req);
		
		IHostNode host = cc.parseHost(req, attach ? HostMode.ATTACH : null, 
				req.getRemoteAddr());
		
		boolean result = false;
		
		if (host != null) {
			NamedList<Object> hostSelf = getHostInfo(cc.getHostSelf(), IUser.ShowType.DEFAULT);
			
			IHostCluster cluster = cc.getCluster(host.getClusterId());
			long heartbeat = 0;
			
			if (cluster != null) {
				String clusterSecret = cluster.getSecret();
				if (clusterSecret != null && clusterSecret.length() > 0) {
					if (!attach && !clusterSecret.equals(host.getClusterSecret())) {
						throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
								"Wrong secret for cluster: " + cluster.getClusterId());
					}
				}
				
				IHostNode old = cluster.getHostByKey(host.getHostKey());
				if (old != null) {
					String hostAddress = old.getHostAddress();
					heartbeat = old.getHeartbeatTime();
					
					if (hostAddress != null && hostAddress.equals(host.getHostAddress())) {
						//host.setHeartbeatTime(old.getHeartbeatTime());
						host.setStatusCode(old.getStatusCode());
					}
				}
			} else if (cluster == null) {
				if (host.getHostMode() != HostMode.HOST && host.getHostMode() != HostMode.NAMED) {
					throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
							"Cluster: " + host.getClusterId() + " not found");
				}
			}
			
			IAttachUser[] selfAttachUsers = host.getAttachUsers(cc.getHostSelf().getHostKey());
			if (!attach || (selfAttachUsers != null && selfAttachUsers.length > 0))
				cc.addHost(host);
			
			rsp.add("hostself", hostSelf);
			rsp.add("host", getHostInfo(host, IUser.ShowType.DEFAULT));
			rsp.add("cluster", getClusterInfo(cluster, heartbeat, 0, false, IUser.ShowType.ALL));
			
			if (attach) {
				IHostInfo[] hosts = host.getAttachHosts();
				rsp.add("attachhosts", getAttachHostInfos(host, hosts, IUser.ShowType.ALL));
			}
			
			result = true;
		}
		
		rsp.add("now", System.currentTimeMillis());
		rsp.add("tz", TimeZone.getDefault().getRawOffset());
		rsp.add("result", result);
	}
	
	private void handleHostGet(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		checkClient(req);
		handleGet0(req, rsp);
		
		rsp.add("now", System.currentTimeMillis());
		rsp.add("tz", TimeZone.getDefault().getRawOffset());
		rsp.add("result", true);
	}
	
	private void handleJoinGet(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null) return;
		
		checkClient(req);
		handleGet0(req, rsp);
		
		rsp.add("now", System.currentTimeMillis());
		rsp.add("tz", TimeZone.getDefault().getRawOffset());
		rsp.add("result", true);
	}
	
	private void handleAttachGet(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null) return;
		
		checkClient(req);
		
		String appkey = trim(req.getParam("appkey"));
		String[] attachUserNames = req.getParams("attachuser");
		
		if (attachUserNames != null && attachUserNames.length > 0) {
			if (getCore().getCluster().getHostSelf().getHostMode() != HostMode.ATTACH) {
				throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
						"Host not in " + HostMode.ATTACH + " mode");
			}
			
			Set<AttachUser> attachUsers = new HashSet<AttachUser>();
			
			for (String attachUserName : attachUserNames) {
				if (attachUserName == null || attachUserName.length() == 0) 
					continue;
				
				String[] tokens = StringUtils.split(attachUserName, '/');
				if (tokens == null || tokens.length < 3) continue;
				
				AttachUser attachUser = new AttachUser(tokens[0],tokens[1],tokens[2]);
				if (LOG.isDebugEnabled()) 
					LOG.debug("handleAttachGet: attachUser=" + attachUser);
				
				if (!getCore().getAdminConfig().hasAttachUser(attachUser.getUserName())) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Attach user: " + attachUser.getUserName() + " not found");
				}
				
				if (!getCore().getAdminConfig().hasAttachUser(attachUser.getUserEmail())) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Attach user: " + attachUser.getUserEmail() + " not found");
				}
				
				attachUsers.add(attachUser);
			}
			
			if (appkey == null || appkey.length() == 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Attach host key is empty");
			}
			
			IHostInfo attachHost = getCore().getCluster().getAttachHost(appkey);
			ArrayList<IUser> users = new ArrayList<IUser>();
			
			for (AttachUser attachUser : attachUsers) {
				if (attachUser == null) continue;
				String hostkey = attachHost.getHostKey();
				
				IUser user = UserHelper.getLocalUserByName(attachUser.getUserEmail());
				if (user == null) {
					user = UserHelper.registerLocalUser(attachUser.getUserEmail(), 
							"000000", IUser.ATTACHUSER, hostkey);
				}
				if (user != null) {
					String hostname = attachHost.getClusterDomain();
					if (hostname == null || hostname.length() == 0) 
						hostname = attachHost.getHostName() + "/" + attachHost.getLanAddress();
					
					UserLoginHandler.initProfile(getCore().getClusterSelf(), user, 
							hostkey, (String)null, hostkey, hostname, attachUser.getUserKey(), 
							attachUser.getUserName(), attachUser.getUserEmail());
				}
				
				if (user != null) users.add(user);
			}
			
			NamedList<Object> userList = new NamedMap<Object>();
			for (IUser user : users) {
				if (user == null) continue;
				
				if (user instanceof IMember) {
					NamedList<Object> userInfo = NamedHelper.toNamedMap((IMember)user, 0, 0, null);
					if (userInfo != null) {
						userInfo.add("libraries", DatumDashboardHandler.getLibraries(user.getDataManager(), 0));
						userList.add(user.getUserKey(), userInfo);
					}
				} else if (user instanceof IGroup) {
					NamedList<Object> userInfo = NamedHelper.toNamedMap((IGroup)user, 0, 0, null);
					if (userInfo != null) {
						userInfo.add("libraries", DatumDashboardHandler.getLibraries(user.getDataManager(), 0));
						userList.add(user.getUserKey(), userInfo);
					}
				}
			}
			
			rsp.add("users", userList);
		} else {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Attach users is empty");
		}
		
		handleGet0(req, rsp);
		
		rsp.add("now", System.currentTimeMillis());
		rsp.add("tz", TimeZone.getDefault().getRawOffset());
		rsp.add("result", true);
	}
	
	private void handleGet0(Request req, Response rsp) 
			throws ErrorException {
		if (req == null || rsp == null)
			return;
		
		String[] idkeys = req.getParams("idkey");
		int hostcount = parseInt(req.getParam("hostcount"));
		if (hostcount <= 0) hostcount = 1;
		
		CoreCluster cc = getCore().getCluster();
		cc.scanClusters(null);
		//boolean result = false;
		
		NamedList<Object> hostSelf = getHostInfo(cc.getHostSelf(), IUser.ShowType.DEFAULT);
		NamedList<Object> clusterList = new NamedMap<Object>();
		
		for (int i=0; idkeys != null && i < idkeys.length; i++) {
			String idkey = idkeys[i];
			if (idkey == null || idkey.length() == 0) continue;
			
			String clusterId = null;
			ArrayList<String> keys = new ArrayList<String>();
			
			StringTokenizer st = new StringTokenizer(idkey, " \t\r\n,;/");
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (clusterId == null) clusterId = token;
				else keys.add(token);
			}
			
			IHostCluster cluster = cc.getCluster(clusterId);
			if (cluster == null) continue;
			
			NamedList<Object> clusterInfo = new NamedMap<Object>();
			clusterInfo.add("clusterid", toString(cluster.getClusterId()));
			clusterInfo.add("clusterdomain", toString(cluster.getDomain()));
			clusterInfo.add("maildomain", toString(cluster.getMailDomain()));
			clusterInfo.add("hostcount", cluster.getHostCount());
			
			if (keys != null && keys.size() > 0) {
				NamedList<Object> hostList = new NamedMap<Object>();
				for (String key : keys) {
					IHostNode host = cluster.getHostByKey(key);
					if (host != null) {
						NamedList<Object> hostInfo = getHostInfo(host, IUser.ShowType.DEFAULT);
						if (hostInfo != null) 
							hostList.add(host.getHostKey(), hostInfo);
					}
				}
				
				clusterInfo.add("hosts", hostList);
			} else {
				NamedList<Object> hostList = new NamedMap<Object>();
				IHostNode[] nodes = cluster.getHosts();
				
				for (int k=0; nodes != null && k < nodes.length; k++) { 
					IHostNode node = nodes[k];
					if (node == null) continue;
				
					NamedList<Object> hostInfo = getHostInfo(node, IUser.ShowType.DEFAULT);
					if (hostInfo != null) { 
						hostList.add(node.getHostKey(), hostInfo);
						if (hostList.size() >= hostcount)
							break;
					}
				}
				
				clusterInfo.add("hosts", hostList);
			}
			
			clusterList.add(cluster.getClusterId(), clusterInfo);
			
			//result = true;
		}
		
		rsp.add("hostself", hostSelf);
		rsp.add("clusters", clusterList);
		
		//rsp.add("now", System.currentTimeMillis());
		//rsp.add("tz", TimeZone.getDefault().getRawOffset());
		//rsp.add("result", result);
	}
	
	private void handleInvite(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		String address = trim(req.getParam("address"));
		URI uri = parseAddressURI(address, req.getRequestInput().getScheme());
		
		if (LOG.isDebugEnabled())
			LOG.debug("handleInvite: addressUri=" + uri);
		
		String scheme = uri.getScheme();
		String host = uri.getHost();
		int port = uri.getPort();
		
		if (scheme == null || scheme.length() == 0)
			scheme = "http";
		if (port <= 0) port = 80;
		
		if (host == null || host.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host(" + uri + ") is illegal");
		}
		
		if (host.equals(getCore().getHostAddress()) && 
		   (port == getCore().getHttpPort() || port == getCore().getHttpsPort())) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host(" + host + ":" + port + ") cannot be current site");
		}
	}
	
	private void handleList(Request req, Response rsp, 
			IMember me) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		CoreCluster cc = getCore().getCluster();
		cc.scanClusters(null);
		
		IHostCluster[] clusters = cc.getClusters();
		NamedList<Object> clusterList = new NamedMap<Object>();
		
		for (int i=0; clusters != null && i < clusters.length; i++) { 
			IHostCluster node = clusters[i];
			if (node == null) continue;
			
			NamedList<Object> info = getClusterInfo(node, 0, 1, true, IUser.ShowType.ALL);
			if (info != null) clusterList.add(node.getClusterId(), info);
		}
		
		rsp.add("clusters", clusterList);
		
		rsp.add("now", System.currentTimeMillis());
		rsp.add("tz", TimeZone.getDefault().getRawOffset());
		rsp.add("result", true);
	}
	
	private void handleCluster(Request req, Response rsp, 
			IMember me, String clusterId) throws ErrorException { 
		if (req == null || rsp == null || me == null)
			return;
		
		CoreCluster cc = getCore().getCluster();
		cc.scanClusters(null);
		
		NamedList<Object> clusterInfo = new NamedMap<Object>();
		boolean result = false;
		
		IHostCluster cluster = cc.getCluster(clusterId);
		if (cluster != null) {
			clusterInfo = getClusterInfo(cluster, 0, 0, true, IUser.ShowType.ALL);
			result = true;
		}
		
		rsp.add("cluster", clusterInfo);
		
		rsp.add("now", System.currentTimeMillis());
		rsp.add("tz", TimeZone.getDefault().getRawOffset());
		rsp.add("result", result);
	}
	
	static NamedList<Object> getClusterInfo(IHostCluster item, 
			long heartbeat, int hostSize, boolean includeAttach, 
			IUser.ShowType showType) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("clusterid", toString(item.getClusterId()));
			info.add("clusterdomain", toString(item.getDomain()));
			info.add("maildomain", toString(item.getMailDomain()));
			info.add("hostcount", item.getHostCount());
			
			if (hostSize >= 0) { 
				NamedList<Object> hostList = new NamedMap<Object>();
				IHostNode[] nodes = item.getHosts();
				
				for (int i=0; nodes != null && i < nodes.length; i++) { 
					IHostNode node = nodes[i];
					if (node == null || node.getHeartbeatTime() < heartbeat) 
						continue;
					
					if (!includeAttach && node.getHostMode() == HostMode.ATTACH)
						continue;
					
					NamedList<Object> hostInfo = getHostInfo(node, showType);
					if (hostInfo != null) { 
						hostList.add(node.getHostKey(), hostInfo);
						if (hostSize > 0 && hostList.size() >= hostSize)
							break;
					}
				}
				
				info.add("hosts", hostList);
			}
		}
		
		return info;
	}
	
	public static NamedList<Object> getHostInfo(IHostNode item, 
			IUser.ShowType showType) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("mode", toString(item.getHostMode()));
			info.add("key", toString(item.getHostKey()));
			info.add("clusterid", toString(item.getClusterId()));
			
			if (showType != null) {
				info.add("clusterdomain", toString(item.getClusterDomain()));
				info.add("maildomain", toString(item.getMailDomain()));
				info.add("admin", toString(item.getAdminUser()));
				info.add("domain", toString(item.getHostDomain()));
				info.add("hostaddr", toString(item.getHostAddress()));
				info.add("hostname", toString(item.getHostName()));
				info.add("lanaddr", toString(item.getLanAddress()));
				info.add("httpport", item.getHttpPort());
				info.add("httpsport", item.getHttpsPort());
				info.add("heartbeat", item.getHeartbeatTime());
				info.add("status", item.getStatusCode());
				info.add("hashcode", item.getHostHash());
				info.add("hostcount", item.getHostCount());
			}
			
			if (item.isSelf()) info.add("self", item.isSelf());
			
			if (showType == IUser.ShowType.ALL) {
				info.add("attachusers", toString(item.getAttachUserNames(null, 2)));
			}
		}
		
		return info;
	}
	
	public static NamedList<Object> getHostInfos(IHostInfo[] items, 
			IUser.ShowType showType) throws ErrorException { 
		NamedList<Object> hostList = new NamedMap<Object>();
		
		for (int i=0; items != null && i < items.length; i++) { 
			IHostInfo host = items[i];
			if (host == null) continue;
		
			NamedList<Object> hostInfo = getHostInfo(host, showType);
			if (hostInfo != null) { 
				hostList.add(host.getHostKey(), hostInfo);
			}
		}
		
		return hostList;
	}
	
	public static NamedList<Object> getHostInfo(IHostInfo item, 
			IUser.ShowType showType) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("mode", toString(item.getHostMode()));
			info.add("key", toString(item.getHostKey()));
			info.add("clusterid", toString(item.getClusterId()));
			
			if (showType != null) {
				info.add("clusterdomain", toString(item.getClusterDomain()));
				info.add("maildomain", toString(item.getMailDomain()));
				info.add("admin", toString(item.getAdminUser()));
				info.add("domain", toString(item.getHostDomain()));
				info.add("hostaddr", toString(item.getHostAddress()));
				info.add("hostname", toString(item.getHostName()));
				info.add("lanaddr", toString(item.getLanAddress()));
				info.add("httpport", item.getHttpPort());
				info.add("httpsport", item.getHttpsPort());
				info.add("heartbeat", item.getHeartbeatTime());
				info.add("status", item.getStatusCode());
				info.add("hashcode", item.getHostHash());
				info.add("hostcount", item.getHostCount());
			}
			
			if (item.isSelf()) info.add("self", item.isSelf());
			
			if (showType == IUser.ShowType.ALL) {
				info.add("attachusers", toString(item.getAttachUserNames(null, 2)));
			}
		}
		
		return info;
	}
	
	public static NamedList<Object> getAttachHostInfos(IHostNode node, 
			IHostInfo[] hosts, IUser.ShowType showType) throws ErrorException { 
		NamedList<Object> hostList = new NamedMap<Object>();
		
		for (int i=0; hosts != null && i < hosts.length; i++) { 
			IHostInfo host = hosts[i];
			if (host == null) continue;
		
			NamedList<Object> hostInfo = getAttachHostInfo(node, host, showType);
			if (hostInfo != null) { 
				hostList.add(host.getHostKey(), hostInfo);
			}
		}
		
		return hostList;
	}
	
	public static NamedList<Object> getAttachHostInfo(IHostNode node, 
			IHostInfo item, IUser.ShowType showType) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (node != null && item != null) { 
			info.add("mode", toString(item.getHostMode()));
			info.add("key", toString(item.getHostKey()));
			info.add("clusterid", toString(item.getClusterId()));
			
			if (showType != null) {
				info.add("clusterdomain", toString(item.getClusterDomain()));
				info.add("maildomain", toString(item.getMailDomain()));
				info.add("admin", toString(item.getAdminUser()));
				info.add("domain", toString(item.getHostDomain()));
				info.add("hostaddr", toString(item.getHostAddress()));
				info.add("hostname", toString(item.getHostName()));
				info.add("lanaddr", toString(item.getLanAddress()));
				info.add("httpport", item.getHttpPort());
				info.add("httpsport", item.getHttpsPort());
				info.add("heartbeat", item.getHeartbeatTime());
				info.add("status", item.getStatusCode());
				info.add("hashcode", item.getHostHash());
				info.add("hostcount", item.getHostCount());
			}
			
			if (item.isSelf()) info.add("self", item.isSelf());
			
			if (showType == IUser.ShowType.ALL) {
				info.add("attachusers", toString(node.getAttachUserNames(item.getHostKey(), 0)));
			}
		}
		
		return info;
	}
	
	public static NamedList<Object> getAttachUserInfos(IAttachUser[] items, 
			IUser.ShowType showType, boolean showHost) throws ErrorException { 
		NamedList<Object> userList = new NamedMap<Object>();
		
		for (int i=0; items != null && i < items.length; i++) { 
			IAttachUser user = items[i];
			if (user == null) continue;
		
			NamedList<Object> userInfo = getAttachUserInfo(user, showType, true);
			if (userInfo != null) { 
				userList.add(user.getUserKey(), userInfo);
			}
		}
		
		return userList;
	}
	
	public static NamedList<Object> getAttachUserInfo(IAttachUser item, 
			IUser.ShowType showType, boolean showHost) throws ErrorException { 
		NamedList<Object> info = new NamedMap<Object>();
		
		if (item != null) { 
			info.add("key", toString(item.getUserKey()));
			info.add("name", toString(item.getUserName()));
			info.add("mailaddr", toString(item.getUserEmail()));
			
			if (item instanceof AttachHostUser) {
				AttachHostUser hostUser = (AttachHostUser)item;
				IHostInfo host = hostUser.getHostNode();
				info.add("host", getHostInfo(host, showType));
			}
		}
		
		return info;
	}
	
	static URI parseAddressURI(String addr, String scheme) 
			throws ErrorException { 
		if (addr == null || addr.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host address is empty");
		}
		
		int pos1 = addr.indexOf('(');
		if (pos1 >= 0) {
			int pos2 = addr.indexOf(')', ++pos1);
			if (pos2 <= pos1) pos2 = addr.length();
			
			String val = addr.substring(pos1, pos2);
			if (val != null && val.length() > 0)
				addr = val;
		}
		
		try {
			if (addr.indexOf("://") < 0)
				addr = scheme + "://" + addr;
			
			if (LOG.isDebugEnabled())
				LOG.debug("parseAddressURI: address=" + addr);
			
			return new URI(addr);
		} catch (Throwable e) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host address:port is illegal", e);
		}
	}
	
}
