package org.javenstudio.falcon.user.auth;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.IUserStore;
import org.javenstudio.raptor.io.Text;

final class AuthStore {
	private static final Logger LOG = Logger.getLogger(AuthStore.class);

	private final AuthService mService;
	private final IUserStore mStore;
	
	private final Map<String,UserWriter> mUserWriters = 
			new HashMap<String,UserWriter>();
	
	private final Map<String,NameWriter> mNameWriters = 
			new HashMap<String,NameWriter>();
	
	public AuthStore(AuthService service, IUserStore store) { 
		if (service == null || store == null) throw new NullPointerException();
		mService = service;
		mStore = store;
	}
	
	public AuthService getService() { return mService; }
	public IUserStore getStore() { return mStore; }
	
	private synchronized UserWriter getUserWriter(String name) 
			throws ErrorException { 
		String dirname = AuthHelper.getStoreDir(name);
		synchronized (mUserWriters) {
			UserWriter writer = mUserWriters.get(dirname);
			if (writer == null) {
				writer = new UserWriter(this, dirname);
				mUserWriters.put(dirname, writer);
			}
			return writer;
		}
	}
	
	private synchronized void closeUserWriter(UserWriter writer) 
			throws ErrorException { 
		if (writer == null) return;
		synchronized (mUserWriters) {
			UserWriter w = mUserWriters.remove(writer.getDirName());
			if (w != null && w != writer) w.close();
			writer.close();
		}
	}
	
	private synchronized void closeUserWriters() throws ErrorException { 
		synchronized (mUserWriters) {
			for (UserWriter writer : mUserWriters.values()) {
				if (writer != null) writer.close();
			}
			mUserWriters.clear();
		}
	}
	
	private synchronized NameWriter getNameWriter(String name) 
			throws ErrorException { 
		String dirname = AuthHelper.getStoreDir(name);
		synchronized (mNameWriters) {
			NameWriter writer = mNameWriters.get(dirname);
			if (writer == null) {
				writer = new NameWriter(this, dirname);
				mNameWriters.put(dirname, writer);
			}
			return writer;
		}
	}
	
	private synchronized void closeNameWriter(NameWriter writer) 
			throws ErrorException { 
		if (writer == null) return;
		synchronized (mNameWriters) {
			NameWriter w = mNameWriters.remove(writer.getDirName());
			if (w != null && w != writer) w.close();
			writer.close();
		}
	}
	
	private synchronized void closeNameWriters() throws ErrorException { 
		synchronized (mNameWriters) {
			for (NameWriter writer : mNameWriters.values()) {
				if (writer != null) writer.close();
			}
			mNameWriters.clear();
		}
	}
	
	public synchronized UserBuffer loadUserData(String username) 
			throws ErrorException { 
		if (username == null) throw new NullPointerException();
		
		return getUserWriter(username).get(new Text(username));
	}
	
	public synchronized NameBuffer loadNameData(String name) 
			throws ErrorException { 
		if (name == null) throw new NullPointerException();
		
		return getNameWriter(name).get(new Text(name));
	}
	
	public synchronized void saveUserData(String username, 
			UserBuffer data) throws ErrorException { 
		if (username == null) throw new NullPointerException();
		
		if (data != null) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("saveNameData: username=" + username + " data=" + data);
			
			UserWriter writer = getUserWriter(username);
			writer.write(new Text(username), data);
			closeUserWriter(writer);
			
			byte[] buffer = Buffer.encode(data);
			getService().getCache().putUserData(username, buffer);
			
		} else { 
			if (LOG.isDebugEnabled()) 
				LOG.debug("saveNameData: remove username=" + username);
			
			UserWriter writer = getUserWriter(username);
			writer.remove(new Text(username));
			closeUserWriter(writer);
			
			getService().getCache().clearUserData(username);
		}
	}
	
	public synchronized void saveNameData(String name, 
			NameBuffer data) throws ErrorException { 
		if (name == null) throw new NullPointerException();
		
		if (data != null) {
			byte[] buffer = NameBuffer.encode(data);
			
			if (buffer != null && buffer.length > 0) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("saveNameData: name=" + name + " data=" + data
							+ " bufferSize=" + buffer.length);
				}
				
				NameWriter writer = getNameWriter(name);
				writer.write(new Text(name), data);
				closeNameWriter(writer);
				
				getService().getCache().putNameData(name, buffer);
				
				return;
			}
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("saveNameData: remove name=" + name);
		
		NameWriter writer = getNameWriter(name);
		writer.remove(new Text(name));
		closeNameWriter(writer);
		
		getService().getCache().clearNameData(name);
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		try {
			closeUserWriters();
			closeNameWriters();
		} catch (ErrorException e) { 
			if (LOG.isErrorEnabled())
				LOG.error("close: error: " + e, e);
			
			throw new RuntimeException(e);
		}
	}
	
	static class UserWriter extends BufferWriter<UserBuffer> { 
		public UserWriter(AuthStore store, String dirname) throws ErrorException { 
			super(store, UserBuffer.class, dirname, "user");
		}
		@Override
		public UserBuffer newBuffer() throws ErrorException { 
			return new UserBuffer();
		}
	}
	
	static class NameWriter extends BufferWriter<NameBuffer> { 
		public NameWriter(AuthStore store, String dirname) throws ErrorException { 
			super(store, NameBuffer.class, dirname, "name");
		}
		@Override
		public NameBuffer newBuffer() throws ErrorException { 
			return new NameBuffer();
		}
	}
	
}
