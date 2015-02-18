package org.javenstudio.falcon.setting.cluster;

import java.io.IOException;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.INameData;
import org.javenstudio.falcon.user.IUserData;
import org.javenstudio.falcon.user.IUserName;
import org.javenstudio.falcon.user.User;
import org.javenstudio.falcon.user.UserHelper;
import org.javenstudio.falcon.user.UserManager;

public class HostClusterSelf extends HostCluster {
	private static final Logger LOG = Logger.getLogger(HostClusterSelf.class);

	private final HostSelf mHostSelf;
	
	public HostClusterSelf(HostManager manager, String clusterId, HostSelf hostself) {
		super(manager, clusterId);
		if (hostself == null) throw new NullPointerException();
		mHostSelf = hostself;
	}
	
	public HostSelf getHostSelf() { return mHostSelf; }

	@Override
	public IHostUserName getHostUserName(final IUserName uname) throws ErrorException {
		if (uname == null) throw new NullPointerException();
		if (LOG.isDebugEnabled())
			LOG.debug("getHostUserName: username=" + uname);
		
		String username = uname.getUserName();
		String hostkey = uname.getHostKey();
		
		final IHostUserData userdata;
		
		if (hostkey != null && hostkey.length() > 0) {
			if (hostkey.equals(getHostSelf().getHostKey())) {
				IUserData data = UserManager.getInstance().getService().searchUser(username);
				userdata = createUserData(data);
			} else {
				IHostNode host = getHostByKey(hostkey);
				userdata = requestGetUserData(host, username);
			}
			
			if (userdata == null) {
				throw new ErrorException(ErrorException.ErrorCode.NOT_FOUND, 
						"User: " + username + " not found");
			}
		} else {
			userdata = searchUser(username);
		}
		
		final IHostInfo host;
		
		if (userdata != null && userdata.getHostNode() != null) {
			host = userdata.getHostNode();
		} else {
			IHostNode node = null;
			IHostList hostlist = getHostListByName(username);
			if (hostlist != null) node = hostlist.currentHost();
			if (node == null) node = getHostSelf();
			host = node;
		}
		
		return new IHostUserName() {
				@Override
				public String getUserName() {
					return uname.getUserName();
				}
				@Override
				public String getName() {
					return uname.getName();
				}
				@Override
				public String getHostKey() {
					if (host != null) return host.getHostKey();
					return uname.getHostKey();
				}
				@Override
				public String getDomain() {
					return uname.getDomain();
				}
				@Override
				public IHostInfo getHostNode() {
					return host;
				}
				@Override
				public IHostUserData getHostUserData() {
					return userdata;
				}
				@Override
				public String toString() {
					return "HostUserName{username=" + getUserName() 
							+ ",name=" + getName() + ",domain=" + getDomain() 
							+ ",hostkey=" + getHostKey() 
							+ ",host=" + getHostNode() + "}";
				}
			};
	}
	
	@Override
	public IHostUserData searchUser(String username) throws ErrorException {
		if (username == null || username.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("searchUser: username=" + username);
		
		final IUserData data = UserManager.getInstance().getService().searchUser(username);
		if (data != null) {
			String hostkey = data.getHostKey();
			if (hostkey == null || hostkey.length() == 0 || 
				hostkey.equals(getHostSelf().getHostKey())) {
				return createUserData(data);
			}
			
			final IHostNode host = getHostByKey(hostkey);
			if (host != null) {
				IHostUserData userdata = requestGetUserData(host, username);
				if (userdata != null) return userdata;
			}
		}
		
		final IHostNode named = selectNamedHost(getHostSelf().getHostKey());
		if (named != null) {
			IHostUserData userdata = requestGetUserData(named, username);
			if (userdata != null) return userdata;
		}
		
		return null;
	}

	private IHostUserData createUserData(final IUserData data) {
		if (data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("createUserData: data=" + data);
		
		return new IHostUserData() {
				@Override
				public IHostInfo getHostNode() {
					return getHostSelf();
				}
				@Override
				public String getUserName() {
					return data.getUserName();
				}
				@Override
				public String getUserKey() {
					return data.getUserKey();
				}
				@Override
				public String getUserEmail() {
					return User.toUserEmail(getHostNode(), getUserName());
				}
				@Override
				public String toString() {
					return "HostUserData{username=" + getUserName() 
							+ ",userkey=" + getUserKey() + ",email=" + getUserEmail() 
							+ ",host=" + getHostNode() + "}";
				}
			};
	}
	
	private IHostUserData createUserData(final IHostNode host, 
			AnyboxData data) throws ErrorException {
		if (host == null || data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("createUserData: host=" + host + " data=" + data);
		
		try {
			final AnyboxUser.GetUserData userdata = AnyboxUser.loadGetUser(data);
			if (userdata != null) {
				return new IHostUserData() {
					@Override
					public IHostInfo getHostNode() {
						return host;
					}
					@Override
					public String getUserName() {
						return userdata.getUserName();
					}
					@Override
					public String getUserKey() {
						return userdata.getUserKey();
					}
					@Override
					public String getUserEmail() {
						return User.toUserEmail(getHostNode(), getUserName());
					}
					@Override
					public String toString() {
						return "HostUserData{username=" + getUserName() 
								+ ",userkey=" + getUserKey() + ",email=" + getUserEmail() 
								+ ",host=" + getHostNode() + "}";
					}
				};
			}
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					e.toString(), e);
		}
		
		return null;
	}
	
	private IHostUserData requestGetUserData(final IHostNode host, 
			String username) throws ErrorException {
		if (host == null || username == null || username.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("requestUserData: username=" + username + " host=" + host);
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String url = "http://" + hostAddress + "/lightning/user/cluster?action=getuser&wt=secretjson"
				+ "&secret.username=" + HostHelper.encodeSecret(username)
				+ "&secret.requestfrom=" + HostHelper.encodeSecret(getHostSelf().getHostKey());
		
		AnyboxListener.GetUserListener listener = new AnyboxListener.GetUserListener();
		getManager().fetchUri(url, listener);
		
		ActionError error = listener.mError;
		AnyboxData adata = listener.mData;
		if (error == null || error.getCode() == 0) {
			error = null;
			if (adata != null) {
				try {
					return createUserData(host, adata.get("userdata"));
				} catch (IOException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							e.toString(), e);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public IHostNameData searchName(String name) throws ErrorException {
		if (name == null || name.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("searchName: name=" + name);
		
		final INameData data = UserManager.getInstance().getService().searchName(name);
		if (data != null) {
			String hostkey = data.getHostKey();
			if (hostkey == null || hostkey.length() == 0 || 
				hostkey.equals(getHostSelf().getHostKey())) {
				if (LOG.isDebugEnabled())
					LOG.debug("searchName: create HostNameData: " + data);
				
				return new IHostNameData() {
						@Override
						public IHostInfo getHostNode() {
							return getHostSelf();
						}
						@Override
						public String getNameKey() {
							return data.getNameKey();
						}
						@Override
						public String getNameValue() {
							return data.getNameValue();
						}
						@Override
						public String toString() {
							return "HostNameData{key=" + getNameKey() 
									+ ",value=" + getNameValue() 
									+ ",host=" + getHostNode() + "}";
						}
					};
			}
			
			final IHostNode host = getHostByKey(hostkey);
			if (host != null) {
				IHostNameData namedata = requestGetNameData(host, name);
				if (namedata != null) return namedata;
			}
		}
		
		final IHostNode named = selectNamedHost(getHostSelf().getHostKey());
		if (named != null) {
			IHostNameData namedata = requestGetNameData(named, name);
			if (namedata != null) return namedata;
		}
		
		return null;
	}
	
	private IHostNameData createNameData(final IHostNode host, 
			AnyboxData data) throws ErrorException {
		if (host == null || data == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("createNameData: host=" + host + " data=" + data);
		
		try {
			final AnyboxUser.GetNameData namedata = AnyboxUser.loadGetName(data);
			if (namedata != null) {
				return new IHostNameData() {
					@Override
					public IHostInfo getHostNode() {
						return host;
					}
					@Override
					public String getNameKey() {
						return namedata.getNameKey();
					}
					@Override
					public String getNameValue() {
						return namedata.getNameValue();
					}
					@Override
					public String toString() {
						return "HostNameData{key=" + getNameKey() 
								+ ",value=" + getNameValue() 
								+ ",host=" + getHostNode() + "}";
					}
				};
			}
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					e.toString(), e);
		}
		
		return null;
	}
	
	private IHostNameData requestGetNameData(final IHostNode host, 
			String name) throws ErrorException {
		if (host == null || name == null || name.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("requestGetNameData: name=" + name + " host=" + host);
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String url = "http://" + hostAddress + "/lightning/user/cluster?action=getname&wt=secretjson"
				+ "&secret.name=" + HostHelper.encodeSecret(name)
				+ "&secret.requestfrom=" + HostHelper.encodeSecret(getHostSelf().getHostKey());
		
		AnyboxListener.GetNameListener listener = new AnyboxListener.GetNameListener();
		getManager().fetchUri(url, listener);
		
		ActionError error = listener.mError;
		AnyboxData adata = listener.mData;
		if (error == null || error.getCode() == 0) {
			error = null;
			if (adata != null) {
				try {
					return createNameData(host, adata.get("namedata"));
				} catch (IOException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							e.toString(), e);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public IHostUserData addUser(String username, String userkey, String hostkey, 
			String password, int flag, int type, Map<String,String> attrs) throws ErrorException {
		if (username == null || username.length() == 0)
			return null;
		
		IHostNode[] namedNodes = getNamedHosts(getHostSelf().getHostKey());
		IHostUserData userData = null;
		
		if (namedNodes != null) {
			for (IHostNode host : namedNodes) {
				IHostUserData data = requestPutUserData(host, username, 
						userkey, hostkey, password, flag, type, attrs);
				if (data != null)
					userData = data;
			}
		}
		
		return userData;
	}
	
	@Override
	public IHostUserData updateUser(String username, String hostkey, String password, 
			int flag, Map<String,String> attrs) throws ErrorException {
		if (username == null || username.length() == 0)
			return null;
		
		IHostNode[] namedNodes = getNamedHosts(getHostSelf().getHostKey());
		IHostUserData userData = null;
		
		if (namedNodes != null) {
			for (IHostNode host : namedNodes) {
				IHostUserData data = requestPutUserData(host, username, 
						null, hostkey, password, flag, -1, attrs);
				if (data != null)
					userData = data;
			}
		}
		
		return userData;
	}
	
	private IHostUserData requestPutUserData(final IHostNode host, 
			String username, String userkey, String hostkey, String password, 
			int flag, int type, Map<String,String> attrs) throws ErrorException {
		if (host == null || username == null || username.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("requestPutUserData: username=" + username + " hostkey=" + hostkey 
					+ " host=" + host);
		}
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String url = "http://" + hostAddress + "/lightning/user/cluster?action=putuser&wt=secretjson"
				+ "&secret.username=" + HostHelper.encodeSecret(username)
				+ "&secret.userkey=" + HostHelper.encodeSecret(userkey)
				+ "&secret.hostkey=" + HostHelper.encodeSecret(hostkey)
				+ "&secret.password=" + HostHelper.encodeSecret(password)
				+ "&secret.flag=" + HostHelper.encodeSecret(String.valueOf(flag)) 
				+ "&secret.type=" + HostHelper.encodeSecret(String.valueOf(type))
				+ "&secret.requestfrom=" + HostHelper.encodeSecret(getHostSelf().getHostKey());
		
		if (attrs != null && attrs.size() > 0) {
			for (Map.Entry<String, String> entry : attrs.entrySet()) {
				String attrkey = entry.getKey();
				String attrval = entry.getValue();
				if (attrkey != null && attrkey.length() > 0 && attrval != null && attrval.length() > 0) {
					url += "&secret.attr=" + HostHelper.encodeSecret(attrkey + "=" + attrval);
				}
			}
		}
		
		AnyboxListener.PutUserListener listener = new AnyboxListener.PutUserListener();
		getManager().fetchUri(url, listener);
		
		ActionError error = listener.mError;
		AnyboxData adata = listener.mData;
		if (error == null || error.getCode() == 0) {
			error = null;
			if (adata != null) {
				try {
					return createUserData(host, adata.get("userdata"));
				} catch (IOException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							e.toString(), e);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public IHostUserData removeUser(String username) throws ErrorException {
		if (username == null || username.length() == 0)
			return null;
		
		IHostNode[] namedNodes = getNamedHosts(getHostSelf().getHostKey());
		IHostUserData userData = null;
		
		if (namedNodes != null) {
			for (IHostNode host : namedNodes) {
				IHostUserData data = requestRemoveUserData(host, username);
				if (data != null)
					userData = data;
			}
		}
		
		return userData;
	}
	
	private IHostUserData requestRemoveUserData(final IHostNode host, 
			String username) throws ErrorException {
		if (host == null || username == null || username.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("requestRemoveUserData: username=" + username 
					+ " host=" + host);
		}
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String url = "http://" + hostAddress + "/lightning/user/cluster?action=rmuser&wt=secretjson"
				+ "&secret.username=" + HostHelper.encodeSecret(username)
				+ "&secret.requestfrom=" + HostHelper.encodeSecret(getHostSelf().getHostKey());
		
		AnyboxListener.RmUserListener listener = new AnyboxListener.RmUserListener();
		getManager().fetchUri(url, listener);
		
		ActionError error = listener.mError;
		AnyboxData adata = listener.mData;
		if (error == null || error.getCode() == 0) {
			error = null;
			if (adata != null) {
				try {
					return createUserData(host, adata.get("userdata"));
				} catch (IOException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							e.toString(), e);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public IHostNameData updateName(String name, String value, String hostkey, 
			int flag, Map<String,String> attrs, String oldname) throws ErrorException {
		if (name == null || name.length() == 0)
			return null;
		
		IHostNode[] namedNodes = getNamedHosts(getHostSelf().getHostKey());
		IHostNameData nameData = null;
		
		if (namedNodes != null) {
			for (IHostNode host : namedNodes) {
				IHostNameData data = requestPutNameData(host, name, 
						value, hostkey, flag, attrs, oldname);
				if (data != null)
					nameData = data;
			}
		}
		
		return nameData;
	}
	
	private IHostNameData requestPutNameData(final IHostNode host, 
			String name, String value, String hostkey, int flag, 
			Map<String,String> attrs, String oldname) throws ErrorException {
		if (host == null || name == null || name.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("requestPutNameData: name=" + name + " host=" + host);
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String url = "http://" + hostAddress + "/lightning/user/cluster?action=putname&wt=secretjson"
				+ "&secret.name=" + HostHelper.encodeSecret(name)
				+ "&secret.value=" + HostHelper.encodeSecret(value)
				+ "&secret.hostkey=" + HostHelper.encodeSecret(hostkey)
				+ "&secret.flag=" + HostHelper.encodeSecret(String.valueOf(flag)) 
				+ "&secret.requestfrom=" + HostHelper.encodeSecret(getHostSelf().getHostKey());
		
		if (attrs != null && attrs.size() > 0) {
			for (Map.Entry<String, String> entry : attrs.entrySet()) {
				String attrkey = entry.getKey();
				String attrval = entry.getValue();
				if (attrkey != null && attrkey.length() > 0 && attrval != null && attrval.length() > 0) {
					url += "&secret.attr=" + HostHelper.encodeSecret(attrkey + "=" + attrval);
				}
			}
		}
		
		AnyboxListener.PutNameListener listener = new AnyboxListener.PutNameListener();
		getManager().fetchUri(url, listener);
		
		ActionError error = listener.mError;
		AnyboxData adata = listener.mData;
		if (error == null || error.getCode() == 0) {
			error = null;
			if (adata != null) {
				try {
					return createNameData(host, adata.get("namedata"));
				} catch (IOException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							e.toString(), e);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public IHostNameData removeName(String name) throws ErrorException {
		if (name == null || name.length() == 0)
			return null;
		
		IHostNode[] namedNodes = getNamedHosts(getHostSelf().getHostKey());
		IHostNameData nameData = null;
		
		if (namedNodes != null) {
			for (IHostNode host : namedNodes) {
				IHostNameData data = requestRemoveNameData(host, name);
				if (data != null)
					nameData = data;
			}
		}
		
		return nameData;
	}
	
	private IHostNameData requestRemoveNameData(final IHostNode host, 
			String name) throws ErrorException {
		if (host == null || name == null || name.length() == 0)
			return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("requestRemoveNameData: name=" + name + " host=" + host);
		
		String hostAddress = host.getHostAddress() + ":" + host.getHttpPort();
		String url = "http://" + hostAddress + "/lightning/user/cluster?action=rmname&wt=secretjson"
				+ "&secret.name=" + HostHelper.encodeSecret(name)
				+ "&secret.requestfrom=" + HostHelper.encodeSecret(getHostSelf().getHostKey());
		
		AnyboxListener.RmNameListener listener = new AnyboxListener.RmNameListener();
		getManager().fetchUri(url, listener);
		
		ActionError error = listener.mError;
		AnyboxData adata = listener.mData;
		if (error == null || error.getCode() == 0) {
			error = null;
			if (adata != null) {
				try {
					return createNameData(host, adata.get("namedata"));
				} catch (IOException e) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							e.toString(), e);
				}
			}
		}
		
		return null;
	}
	
	@Override
	public IHostList getHostListByName(String name) throws ErrorException {
		if (name == null) throw new NullPointerException();
		
		int hash = UserHelper.createHashCode(name);
		IHostList hostlist = getHostListByHash(hash);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("getHostListByName: name=" + name + " hash=" + hash 
					+ " hostlist=" + hostlist);
		}
		
		return hostlist;
	}
	
}
