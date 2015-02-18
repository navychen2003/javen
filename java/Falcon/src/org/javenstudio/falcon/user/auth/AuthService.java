package org.javenstudio.falcon.user.auth;

import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.util.BytesBufferPool;
import org.javenstudio.falcon.user.IAuthService;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IUserStore;
import org.javenstudio.util.ArrayUtils;

public class AuthService implements IAuthService {
	private static final Logger LOG = Logger.getLogger(AuthService.class);
	
	private final AuthCache mCache;
	private final AuthStore mStore;
	
	public AuthService(IUserStore store, String cacheDir) {
		if (store == null || cacheDir == null) throw new NullPointerException();
		mCache = new AuthCache(cacheDir);
		mStore = new AuthStore(this, store);
	}
	
	public AuthStore getStore() { return mStore; }
	public AuthCache getCache() { return mCache; }
	
	@Override
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		mStore.close();
		mCache.close();
	}
	
	@Override
	public synchronized UserBuffer addUser(String username, String userkey, String hostkey, 
			String password, int flag, int type, Map<String,String> attrs) throws ErrorException {
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		if (userkey == null || userkey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User key cannot be empty");
		}
		if (hostkey == null || hostkey.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Host key cannot be empty");
		}
		if (password == null || password.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Password cannot be empty");
		}
		if (searchUser(username) != null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + username + " already existed");
		}
		if (searchName(userkey) != null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + username + " key: " + userkey + " already existed");
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("addUser: username=" + username + " key=" + userkey 
					+ " flag=" + flag + " type=" + type);
		}
		
		UserBuffer data = new UserBuffer(username, userkey, hostkey, password, flag, type);
		if (attrs != null && attrs.size() > 0) {
			for (Map.Entry<String, String> entry : attrs.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				
				data.setAttr(name, value);
			}
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("addUser: username=" + username + " data=" + data);
		
		getStore().saveUserData(username, data);
		updateName(userkey, username, hostkey, IUser.NAME_USERKEY, null);
		
		return data;
	}

	@Override
	public synchronized UserBuffer authUser(String username, 
			String password) throws ErrorException { 
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		if (password == null || password.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Password cannot be empty");
		}
		
		UserBuffer data = searchUser(username);
		if (data == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User: " + username + " not found");
		}
		
		byte[] pwd1 = AuthHelper.encodePwd(password);
		byte[] pwd2 = data.getPassword();
		
		if (pwd1 != null && pwd2 != null && pwd1.length == pwd2.length && pwd1.length > 0) { 
			if (ArrayUtils.equals(pwd1, pwd2, pwd1.length))
				return data;
		}
		
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Username or password is wrong");
	}
	
	@Override
	public synchronized UserBuffer searchUser(String username) 
			throws ErrorException {
		UserBuffer data = searchUser0(username);
		if (data == null && username.indexOf('@') >= 0) {
			NameBuffer namedata = searchName(username);
			if (namedata != null) {
				String namevalue = namedata.getValueAsString();
				if (namevalue != null && namevalue.length() > 0)
					data = searchUser0(namevalue);
			}
		}
		return data;
	}
	
	private UserBuffer searchUser0(String username) 
			throws ErrorException {
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		UserBuffer data = null;
		
		BytesBufferPool.BytesBuffer buffer = getCache().getUserData(username);
		if (buffer != null && buffer.offset >= 0 && buffer.length > 0) 
			data = UserBuffer.decode(buffer.data, buffer.offset, buffer.length);
		
		if (data == null) { 
			data = getStore().loadUserData(username);
			if (data != null) {
				byte[] buf = UserBuffer.encode(data);
				
				if (buf != null && buf.length > 0) 
					getCache().putUserData(username, buf);
			}
		}
		
		if (data != null) { 
			if (!username.equals(data.getNameAsString())) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Username: " + username + " is wrong in database");
			}
			return data;
		}
		
		return null;
	}

	@Override
	public synchronized UserBuffer updateUser(String username, 
			String hostkey, String password, int flag, Map<String,String> attrs) 
			throws ErrorException { 
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		switch (flag) {
		case IUser.FLAG_ENABLED:
		case IUser.FLAG_READONLY:
		case IUser.FLAG_DISABLED:
			break;
		default:
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User flag input wrong");
		}
		
		UserBuffer data = searchUser(username);
		if (data == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User:" + username + " not found");
		}
		
		boolean changed = false;
		
		if (hostkey != null) {
			data.setHostKey(hostkey);
			changed = true;
		}
		
		if (password != null) {
			data.setPassword(password);
			changed = true;
		}
		
		if (flag >= 0 && flag != data.getFlag()) { 
			data.setFlag(flag);
			changed = true;
		}
		
		if (attrs != null && attrs.size() > 0) {
			for (Map.Entry<String, String> entry : attrs.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				
				data.setAttr(name, value);
				changed = true;
			}
		}
		
		if (changed) {
			if (LOG.isDebugEnabled())
				LOG.debug("updateUser: username=" + username + " data=" + data);
			
			getStore().saveUserData(username, data);
		}
		
		return data;
	}
	
	@Override
	public synchronized UserBuffer removeUser(String username) 
			throws ErrorException {
		if (username == null || username.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username cannot be empty");
		}
		
		UserBuffer data = searchUser(username);
		if (data == null) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User:" + username + " not found");
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("removeUser: username=" + username + " data=" + data);
		
		getStore().saveUserData(username, null);
		getStore().saveNameData(data.getKeyAsString(), null);
		
		return data;
	}

	@Override
	public synchronized NameBuffer searchName(String name) throws ErrorException {
		if (name == null || name.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Name cannot be empty");
		}
		
		NameBuffer data = null;
		boolean cached = true;
		
		BytesBufferPool.BytesBuffer buffer = getCache().getNameData(name);
		if (buffer != null && buffer.offset >= 0 && buffer.length > 0) 
			data = NameBuffer.decode(buffer.data, buffer.offset, buffer.length);
		
		if (data == null) {
			cached = false;
			data = getStore().loadNameData(name);
			if (data != null) {
				byte[] buf = NameBuffer.encode(data);
				
				if (buf != null && buf.length > 0) 
					getCache().putNameData(name, buf);
			}
		}
		
		if (data != null) { 
			if (LOG.isDebugEnabled()) {
				LOG.debug("searchName: name=" + name + " data=" + data 
						+ " cached=" + cached);
			}
			
			return data;
		}
		
		return null;
	}

	@Override
	public synchronized NameBuffer updateName(String name, String value, 
			String hostkey, int flag, Map<String,String> attrs) throws ErrorException {
		if (name == null || name.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Name cannot be empty");
		}
		
		NameBuffer data = searchName(name);
		boolean changed = false;
		
		if (data == null) {
			if (hostkey != null && value != null && flag >= 0) {
				data = new NameBuffer(name, value, hostkey, flag);
				changed = true;
			} else
				return null;
		} else {
			if (value != null) {
				data.setValue(value);
				changed = true;
			}
			if (hostkey != null) {
				data.setHostKey(hostkey);
				changed = true;
			}
			if (flag >= 0) {
				data.setFlag(flag);
				changed = true;
			}
		}
		
		if (attrs != null && attrs.size() > 0) {
			for (Map.Entry<String, String> entry : attrs.entrySet()) {
				String key = entry.getKey();
				String val = entry.getValue();
				
				data.setAttr(key, val);
				changed = true;
			}
		}
		
		if (changed) {
			if (LOG.isDebugEnabled())
				LOG.debug("updateName: name=" + name + " data=" + data);
			
			getStore().saveNameData(name, data);
		}
		
		return data;
	}
	
	@Override
	public synchronized NameBuffer removeName(String name) throws ErrorException {
		if (name == null || name.length() == 0) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Name cannot be empty");
		}
		
		NameBuffer data = searchName(name);
		if (data != null) {
			if (LOG.isDebugEnabled())
				LOG.debug("removeName: name=" + name + " data=" + data);
			
			getStore().saveNameData(name, null);
		}
		
		return data;
	}
	
}
