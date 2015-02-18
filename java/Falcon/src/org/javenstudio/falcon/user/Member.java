package org.javenstudio.falcon.user;

import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.user.device.DeviceManager;
import org.javenstudio.falcon.user.profile.ContactManager;
import org.javenstudio.falcon.user.profile.FriendManager;
import org.javenstudio.falcon.user.profile.GroupManager;
import org.javenstudio.falcon.user.profile.HistoryManager;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.util.ILockable;

public final class Member extends User implements IMember {
	private static final Logger LOG = Logger.getLogger(Member.class);

	private final Map<String,UserClient> mClients;
	
	private DeviceManager mDeviceRef = null;
	private ContactManager mContactRef = null;
	private FriendManager mFriendRef = null;
	private GroupManager mGroupRef = null;
	private HistoryManager mHistoryRef = null;
	
	private UserClient mClientLast = null;
	
	Member(UserManager manager, IUserData data) throws ErrorException { 
		super(manager, data);
		
		mClients = new HashMap<String,UserClient>();
		
		if (LOG.isDebugEnabled())
			LOG.debug("create: user=" + this);
	}
	
	@Override
	public boolean isManager() throws ErrorException { 
		return getUserManager().isManager(getUserName());
	}
	
	@Override
	public boolean isAdministrator() throws ErrorException {
		return getUserManager().isAdministrator(getUserName());
	}
	
	@Override
	public synchronized DeviceManager getDeviceManager() throws ErrorException { 
		DeviceManager manager = mDeviceRef;
		if (manager == null || manager.isClosed()) {
			manager = new DeviceManager(this, getUserManager().getStore()); 
			mDeviceRef = manager;
		}
		return manager;
	}
	
	public synchronized DeviceManager removeDeviceManager() {
		DeviceManager manager = mDeviceRef;
		mDeviceRef = null;
		return manager;
	}
	
	@Override
	public synchronized ContactManager getContactManager() throws ErrorException { 
		ContactManager manager = mContactRef;
		if (manager == null || manager.isClosed()) {
			manager = new ContactManager(this, getUserManager().getStore()); 
			mContactRef = manager;
		}
		return manager;
	}
	
	public synchronized ContactManager removeContactManager() { 
		ContactManager manager = mContactRef;
		mContactRef = null;
		return manager;
	}
	
	@Override
	public synchronized FriendManager getFriendManager() throws ErrorException { 
		FriendManager manager = mFriendRef;
		if (manager == null || manager.isClosed()) {
			manager = new FriendManager(this, getUserManager().getStore()); 
			mFriendRef = manager;
		}
		return manager;
	}
	
	public synchronized FriendManager removeFriendManager() { 
		FriendManager manager = mFriendRef;
		mFriendRef = null;
		return manager;
	}
	
	@Override
	public synchronized GroupManager getGroupManager() throws ErrorException {
		GroupManager manager = mGroupRef;
		if (manager == null || manager.isClosed()) {
			manager = new GroupManager(this, getUserManager().getStore()); 
			mGroupRef = manager;
		}
		return manager;
	}
	
	public synchronized GroupManager removeGroupManager() { 
		GroupManager manager = mGroupRef;
		mGroupRef = null;
		return manager;
	}
	
	@Override
	public synchronized HistoryManager getHistoryManager() throws ErrorException {
		HistoryManager manager = mHistoryRef;
		if (manager == null || manager.isClosed()) {
			manager = new HistoryManager(this, getUserManager().getStore()); 
			mHistoryRef = manager;
		}
		return manager;
	}
	
	public synchronized HistoryManager removeHistoryManager() { 
		HistoryManager manager = mHistoryRef;
		mHistoryRef = null;
		return manager;
	}
	
	@Override
	public synchronized UserClient getClient(String token, 
			IUserClient.Factory factory) throws ErrorException {
		if (token == null || token.length() == 0) return null;
		
		synchronized (mClients) {
			UserClient client = mClients.get(token);
			if (client != null) { 
				client.setUpdateTime(System.currentTimeMillis());
				
			} else if (client == null && factory != null) { 
				IUserClient newclient = factory.create(this, token);
				
				if (LOG.isDebugEnabled()) { 
					LOG.debug("getClient: create client: " + newclient 
							+ ", token=" + token);
				}
				
				if (newclient == null) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Cannot create user client for user: " + getUserName());
				}
				if (!(newclient instanceof UserClient)) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Wrong user client class: " + newclient.getClass().getName());
				}
				
				client = (UserClient)newclient;
				if (client.getUser() != this) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Wrong user client for user: " + getUserName());
				}
				if (!token.equals(client.getToken())) { 
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Wrong user client for token: " + token);
				}
				
				synchronized (getUserManager().getClientLock()) {
					IUserClient[] ucs = mClients.values().toArray(new IUserClient[0]);
					if (ucs != null) {
						for (IUserClient uc : ucs) {
							if (uc != null && uc != client && 
								uc.getDevice().getKey().equals(client.getDevice().getKey())) {
								if (LOG.isDebugEnabled()) {
									LOG.debug("getClient: close same device: " + client.getDevice().getKey() 
											+ " client: " + uc);
								}
								uc.close();
							}
						}
					}
					
					mClients.put(token, client);
					mClientLast = client;
					
					getUserManager().addClient(client);
				}
			}
			
			return client;
		}
	}

	synchronized void refreshToken(UserClient client) 
			throws ErrorException { 
		if (client == null || client.getUser() != this)
			return;
		
		synchronized (mClients) {
			String oldtoken = client.getToken();
			String newtoken = client.changeToken();
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("refreshToken: client=" + client 
						+ " oldtoken=" + oldtoken + " newtoken=" + newtoken);
			}
			
			synchronized (getUserManager().getClientLock()) {
				UserClient client2 = mClients.remove(oldtoken);
				IUserClient client3 = getUserManager().removeClient(oldtoken);
				
				if (client != client2 || client != client3) { 
					if (LOG.isErrorEnabled()) {
						LOG.error("refreshToken: wrong client with token: " + oldtoken 
								+ ", client=" + client + " client2=" + client2 
								+ " client3=" + client3);
					}
					
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Wrong user client with token " + oldtoken);
				}
				
				mClients.put(newtoken, client);
				getUserManager().addClient(client);
			}
		}
	}
	
	synchronized boolean removeClient(UserClient client, 
			boolean closeIfEmpty) throws ErrorException { 
		if (client == null || client.getUser() != this)
			return false;
		
		synchronized (mClients) {
			String token = client.getToken();
			UserClient uc = mClients.get(token);
			if (uc == client) { 
				if (LOG.isDebugEnabled()) {
					LOG.debug("removeClient: client=" + client 
							+ " token=" + token);
				}
				
				synchronized (getUserManager().getClientLock()) {
					mClients.remove(token);
					IUserClient client2 = getUserManager().removeClient(token);
					
					if (client != client2) { 
						if (LOG.isErrorEnabled()) {
							LOG.error("removeClient: wrong client with token: " + token 
									+ ", client=" + client + " client2=" + client2);
						}
						
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"Wrong user client with token " + token);
					}
				}
				
				if (mClients.isEmpty() && closeIfEmpty) { 
					if (LOG.isDebugEnabled())
						LOG.debug("removeClient: close user with no clients");
					
					close();
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public synchronized UserClient lastClient() { 
		synchronized (mClients) {
			if (mClientLast == null) { 
				UserClient last = null;
				for (UserClient client : mClients.values()) { 
					if (client == null) continue;
					if (last == null || last.getLoginTime() < client.getLoginTime())
						last = client;
				}
				mClientLast = last;
			}
			return mClientLast;
		}
	}
	
	@Override
	public synchronized UserClient[] getClients() 
			throws ErrorException { 
		synchronized (mClients) {
			return mClients.values().toArray(
					new UserClient[mClients.size()]);
		}
	}
	
	@Override
	public long getUsedSpace() {
		long usedSpace = super.getUsedSpace();
		long groupusedSpace = getGroupUsedSpace(false);
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("getUsedSpace: user=" + getUserName() + " usedSpace=" + usedSpace 
					+ " groupusedSpace=" + groupusedSpace);
		}
		
		return usedSpace + groupusedSpace;
	}
	
	private long mGroupUsedSpace = 0;
	private long mGroupUsedCheckTime = 0;
	private final Object mLock = new Object();
	
	public long getGroupUsedSpace(boolean force) {
		synchronized (mLock) {
			long groupusedSpace = mGroupUsedSpace;
			long current = System.currentTimeMillis();
			
			if (force || current - mGroupUsedCheckTime > 60 * 1000) {
				groupusedSpace = 0;
				
				try {
					IGroup[] groups = getGroups(MemberManager.ROLE_OWNER);
					for (int i=0; groups != null && i < groups.length; i++) {
						IGroup group = groups[i];
						if (group != null) {
							long usedSpace = group.getUsedSpace();
							groupusedSpace += usedSpace;
							
							if (LOG.isDebugEnabled()) {
								LOG.debug("getGroupUsedSpace: user=" + getUserName() 
										+ " group=" + group.getUserName() 
										+ " groupused=" + usedSpace);
							}
						}
					}
				} catch (Throwable e) {
					if (LOG.isWarnEnabled())
						LOG.warn("getGroupUsedSpace: error: " + e, e);
				}
				
				if (LOG.isDebugEnabled()) {
					LOG.debug("getGroupUsedSpace: user=" + getUserName() 
							+ " groupusedSpace=" + groupusedSpace);
				}
				
				mGroupUsedCheckTime = current;
				mGroupUsedSpace = groupusedSpace;
			}
			
			return groupusedSpace;
		}
	}
	
	@Override
	public IGroup[] getGroups(String role) throws ErrorException {
		GroupManager gm = getGroupManager();
		if (gm != null) {
			IGroup[] groups = gm.getGroups(role);
			return groups;
		}
		return null;
	}
	
	@Override
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				DeviceManager dm = mDeviceRef;
				if (dm != null) dm.close();
				mDeviceRef = null;
				
				ContactManager cm = mContactRef;
				if (cm != null) cm.close();
				mContactRef = null;
				
				FriendManager fm = mFriendRef;
				if (fm != null) fm.close();
				mFriendRef = null;
				
				GroupManager gm = mGroupRef;
				if (gm != null) gm.close();
				mGroupRef = null;
				
				HistoryManager hm = mHistoryRef;
				if (hm != null) hm.close();
				mHistoryRef = null;
				
				synchronized (mClients) {
					for (UserClient client : mClients.values()) { 
						if (client != null) client.close();
					}
					mClients.clear();
				}
				
				super.close();
			} finally { 
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
}
