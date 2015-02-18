package org.javenstudio.falcon.user;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.datum.IDatabaseStore;
import org.javenstudio.falcon.datum.table.TableManager;
import org.javenstudio.falcon.datum.util.TimeUtils;
import org.javenstudio.falcon.message.MessageHelper;
import org.javenstudio.falcon.publication.IPublicationStore;
import org.javenstudio.falcon.publication.PublicationManager;
import org.javenstudio.falcon.setting.cluster.StorageManager;
import org.javenstudio.falcon.setting.user.UserCategoryManager;
import org.javenstudio.falcon.user.global.AnnouncementManager;
import org.javenstudio.falcon.user.global.IUnitStore;
import org.javenstudio.falcon.user.global.UnitManager;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.job.JobContext;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.Path;

public final class UserManager implements IDatabase.Manager, 
		IUnitStore, IPublicationStore {
	private static final Logger LOG = Logger.getLogger(UserManager.class);
	
	private static UserManager sInstance = null;
	private static final Object sLock = new Object();
	
	public static UserManager getInstance() { 
		synchronized (sLock) { 
			if (sInstance == null) 
				throw new RuntimeException("UserManager instance not initialized");
			return sInstance;
		}
	}
	
	private final IUserStore mStore;
	private final UserJob mJob;
	private final Map<String, IUser> mUserByKeys;
	private final Map<String, IUser> mUserByNames;
	private final Map<String, StorageManager> mStorageManagers;
	private final String mStoreDir;
	private final String mKey;
	
	private IDatabase mDatabase = null;
	private UserCategoryManager mCategoryManager = null;
	private WeakReference<UnitManager> mUnitRef = null;
	private WeakReference<PublicationManager> mPublicationRef = null;
	private WeakReference<AnnouncementManager> mAnnouncementRef = null;
	private Set<String> mAdmins = null;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return null; // top
			}
			@Override
			public String getName() {
				return "UserManager";
			}
		};
	
	public UserManager(IUserStore store, String storeDir) 
			throws ErrorException { 
		if (store == null || storeDir == null) 
			throw new NullPointerException();
		
		mUserByKeys = new HashMap<String, IUser>();
		mUserByNames = new HashMap<String, IUser>();
		mStorageManagers = new HashMap<String, StorageManager>();
		
		mStore = store;
		mStoreDir = storeDir;
		mKey = UserHelper.newManagerKey(storeDir);
		mJob = new UserJob(this);
		
		synchronized (sLock) { 
			if (sInstance != null) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"UserManager instance already initialized");
			}
			sInstance = this;
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("created: storeDir=" + storeDir);
	}
	
	public IUserStore getStore() { return mStore; }
	public UserJob getJob() { return mJob; }
	
	public String getStoreDir() { return mStoreDir; }
	public String getKey() { return mKey; }
	
	@Override
	public String getUserKey() { 
		return IUser.SYSTEM; // for global IDatabase
	}
	
	@Override
	public String getUserName() {
		return IUser.SYSTEM; // for global IDatabase
	}
	
	@Override
	public ILockable.Lock getLock() { 
		return mLock;
	}
	
	public IAuthService getService() throws ErrorException { 
		return getStore().getService();
	}
	
	@Override
	public synchronized IDatabase getDatabase() 
			throws ErrorException { 
		if (mDatabase == null) 
			mDatabase = new TableManager(this);
		return mDatabase; 
	}
	
	public synchronized UserCategoryManager getCategoryManager() 
			throws ErrorException {
		if (mCategoryManager == null)
			mCategoryManager = new UserCategoryManager(this);
		return mCategoryManager;
	}
	
	public StorageManager getStorageManager(String userkey) {
		if (userkey == null) return null;
		synchronized (mStorageManagers) {
			return mStorageManagers.get(userkey);
		}
	}
	
	public StorageManager removeStorageManager(String userkey) {
		if (userkey == null) return null;
		synchronized (mStorageManagers) {
			return mStorageManagers.remove(userkey);
		}
	}
	
	public StorageManager putStorageManager(String userkey, StorageManager manager) {
		if (userkey == null) return null;
		synchronized (mStorageManagers) {
			if (manager == null) return mStorageManagers.remove(userkey);
			return mStorageManagers.put(userkey, manager);
		}
	}
	
	@Override
	public Configuration getConfiguration() { 
		return getStore().getConfiguration();
	}
	
	@Override
	public IDatabaseStore getDatabaseStore() throws ErrorException { 
		return getStore().getDatumCore().getDatabaseStore(); 
	}
	
	@Override
	public Path getDatabasePath() throws ErrorException { 
		return getDatabaseStore().getDatabasePath(this);
	}
	
	public synchronized void resetAdmin() { 
		mAdmins = null;
	}
	
	public boolean isAdministrator(String username) { 
		if (username == null || username.length() == 0)
			return false;
		
		String admin = getStore().getHostNode().getAdminUser();
		if (admin != null && admin.equals(username))
			return true;
		
		return false;
	}
	
	public synchronized boolean isManager(String username) 
			throws ErrorException { 
		if (username == null || username.length() == 0)
			return false;
		
		if (mAdmins == null) { 
			HashSet<String> set = new HashSet<String>();
			String[] admins = getStore().loadManagers();
			if (admins != null) { 
				for (String name : admins) { 
					if (name != null && name.length() > 0)
						set.add(name);
				}
			}
			mAdmins = set;
		}
		
		return mAdmins.contains(username);
	}
	
	public synchronized UnitManager getUnitManager() throws ErrorException { 
		WeakReference<UnitManager> ref = mUnitRef;
		UnitManager manager = ref != null ? ref.get() : null;
		if (manager == null || manager.isClosed()) {
			manager = new UnitManager(this); 
			mUnitRef = new WeakReference<UnitManager>(manager);
		}
		return manager;
	}
	
	public synchronized UnitManager removeUnitManager() { 
		WeakReference<UnitManager> ref = mUnitRef;
		UnitManager manager = ref != null ? ref.get() : null;
		mUnitRef = null;
		return manager;
	}
	
	public synchronized void closeUnitManager() { 
		UnitManager manager = removeUnitManager();
		if (manager != null) manager.close();
	}
	
	public synchronized PublicationManager getPublicationManager() 
			throws ErrorException { 
		WeakReference<PublicationManager> ref = mPublicationRef;
		PublicationManager manager = ref != null ? ref.get() : null;
		if (manager == null || manager.isClosed()) {
			manager = new PublicationManager(this); 
			manager.initSystemServices();
			mPublicationRef = new WeakReference<PublicationManager>(manager);
		}
		return manager;
	}
	
	public synchronized PublicationManager removePublicationManager() { 
		WeakReference<PublicationManager> ref = mPublicationRef;
		PublicationManager manager = ref != null ? ref.get() : null;
		mPublicationRef = null;
		return manager;
	}
	
	public synchronized void closePublicationManager() { 
		PublicationManager manager = removePublicationManager();
		if (manager != null) manager.close();
	}
	
	public synchronized AnnouncementManager getAnnouncementManager() 
			throws ErrorException { 
		WeakReference<AnnouncementManager> ref = mAnnouncementRef;
		AnnouncementManager manager = ref != null ? ref.get() : null;
		if (manager == null || manager.isClosed()) {
			manager = new AnnouncementManager(this, getStore()); 
			mAnnouncementRef = new WeakReference<AnnouncementManager>(manager);
		}
		return manager;
	}
	
	public synchronized AnnouncementManager removeAnnouncementManager() { 
		WeakReference<AnnouncementManager> ref = mAnnouncementRef;
		AnnouncementManager manager = ref != null ? ref.get() : null;
		mAnnouncementRef = null;
		return manager;
	}
	
	public synchronized void closeAnnouncementManager() { 
		AnnouncementManager manager = removeAnnouncementManager();
		if (manager != null) manager.close();
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				//synchronized (mUserByKeys) { 
					//synchronized (mUserByNames) {
						getJob().close();
						
						IUser[] users = mUserByKeys.values().toArray(
								new IUser[mUserByKeys.size()]);
						
						if (users != null) {
							for (IUser user : users) { 
								if (user != null) user.close();
							}
						}
						
						mUserByKeys.clear();
						mUserByNames.clear();
						
						synchronized (mStorageManagers) {
							mStorageManagers.clear();
						}
						
						closeUnitManager();
						closePublicationManager();
						closeAnnouncementManager();
						
						if (mDatabase != null) mDatabase.close();
						mDatabase = null;
						
						if (mCategoryManager != null) mCategoryManager.close();
						mCategoryManager = null;
					//}
				//}
			} finally { 
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	private synchronized boolean addUser0(IUser user) throws ErrorException {
		if (user == null) return false;
		
		final String username = user.getUserName();
		if (username == null) throw new NullPointerException();
		
		if (user.getUserManager() != this) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"User: " + username + " has wrong manager");
		}
		
		if (user.isClosed()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"User: " + username + " is closed");
		}
		
		//synchronized (mUserByKeys) { 
			//synchronized (mUserByNames) {
				IUser user1 = mUserByNames.get(username);
				IUser user2 = mUserByNames.get(user.getUserKey());
				
				if (user1 != null && user1.isClosed()) 
					user1 = null;
				if (user2 != null && user2.isClosed()) 
					user2 = null;
				
				if (user1 == user && user2 == user) 
					return false;
				
				if (user1 != null && user1 != user) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"User: " + username + " already registered");
				}
				
				if (user2 != null && user2 != user) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"User: " + username + " already registered");
				}
				
				mUserByNames.put(username, user);
				mUserByKeys.put(user.getUserKey(), user);
				
				if (LOG.isDebugEnabled())
					LOG.debug("addUser: " + user);
				
				requestCleanup();
				
				return true;
			//}
		//}
	}
	
	private synchronized IUser removeUser0(IUser user) throws ErrorException { 
		if (user == null) return null;
		
		//synchronized (mUserByKeys) {
			//synchronized (mUserByNames) { 
				IUser user1 = mUserByNames.get(user.getUserName());
				IUser user2 = mUserByKeys.get(user.getUserKey());
				
				if (user1 == user && user2 == user) {
					if (LOG.isDebugEnabled())
						LOG.debug("removeUser: " + user);
					
					user1 = mUserByNames.remove(user.getUserName());
					user2 = mUserByKeys.remove(user.getUserKey());
					
					if (user1 != user || user2 != user) { 
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"User: " + user.getUserName() + "is wrong in manager");
					}
					
					return user;
				}
				
				return null;
			//}
		//}
	}
	
	synchronized IUser removeUser(IUser user) throws ErrorException { 
		if (user == null) return null;
		return removeUser0(user);
	}
	
	public IUser getOrCreate(final String username) throws ErrorException { 
		if (username == null) throw new NullPointerException();
		return getOrCreate(username, new IUserData.Factory() {
				@Override
				public IUserData create(String username) throws ErrorException {
					return getService().searchUser(username);
				}
			});
	}
	
	public synchronized IUser getOrCreate(String username, 
			IUserData.Factory factory) throws ErrorException { 
		if (username == null || factory == null) 
			throw new NullPointerException();
		
		IUser user = getUserByName(username);
		if (user == null || user.isClosed()) { 
			IUserData data = factory.create(username);
			if (data == null) return null;
			
			if (data.getUserType() == IUser.TYPE_GROUP) { 
				Group group = new Group(this, data);
				addUser0(group);
				user = group;
				
				group.getProfile().loadProfile(false);
				
			} else {
				Member member = new Member(this, data);
				addUser0(member);
				user = member;
				
				member.getProfile().loadProfile(false);
				member.getFriendManager().loadFriends(false);
			}
		}
		
		return user;
	}
	
	public synchronized IUser getUserByName(String username) 
			throws ErrorException { 
		if (username == null) return null;
		
		//synchronized (mUserByKeys) { 
			//synchronized (mUserByNames) {
				IUser user = mUserByNames.get(username);
				if (user != null && user.isClosed()) {
					removeUser0(user);
					user = null;
				}
				
				if (user != null && user instanceof User) { 
					User u = (User)user;
					u.setObtainTime(System.currentTimeMillis());
				}
				
				return user;
			//}
		//}
	}
	
	public synchronized IUser getUserByKey(String userkey) 
			throws ErrorException { 
		if (userkey == null) return null;
		
		//synchronized (mUserByKeys) { 
			//synchronized (mUserByNames) {
				IUser user = mUserByKeys.get(userkey);
				if (user != null && user.isClosed()) {
					removeUser0(user);
					user = null;
				}
				
				if (user != null && user instanceof User) { 
					User u = (User)user;
					u.setObtainTime(System.currentTimeMillis());
				}
				
				return user;
			//}
		//}
	}
	
	private final Map<String,WeakReference<IUserClient>> sClients = 
			new HashMap<String,WeakReference<IUserClient>>();
	
	Object getClientLock() { return sClients; }
	
	void addClient(IUserClient client) throws ErrorException { 
		if (client == null) return;
		
		String token = client.getToken();
		
		synchronized (sClients) { 
			WeakReference<IUserClient> ref = sClients.get(token);
			IUserClient uc = ref != null ? ref.get() : null;
			if (uc != null) { 
				if (uc == client) return;
				
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"UserClient: " + uc + " already exists");
			}
			
			sClients.put(token, new WeakReference<IUserClient>(client));
		}
	}
	
	IUserClient getClient(String token) { 
		if (token == null) return null;
		
		synchronized (sClients) { 
			WeakReference<IUserClient> ref = sClients.get(token);
			if (ref != null) {
				IUserClient uc = ref.get();
				if (uc == null) sClients.remove(token);
				return uc;
			}
			return null;
		}
	}
	
	IUserClient removeClient(String token) { 
		if (token == null) return null;
		
		synchronized (sClients) { 
			WeakReference<IUserClient> ref = sClients.remove(token);
			IUserClient uc = ref != null ? ref.get() : null;
			return uc;
		}
	}
	
	private long mLastCleanupTime = 0;
	
	private UserJob.Task mCleanupTask = new UserJob.Task() {
			public String getUser() { return IUser.SYSTEM; }
			public String getMessage() { return "Cleanup idle users"; }
			
			@Override
			public void process(UserJob job, JobContext jc) throws ErrorException {
				if (LOG.isDebugEnabled()) LOG.debug("process: cleanup idle users");
				processCleanup(jc);
			}
			
			@Override
			public void close() {
				if (LOG.isDebugEnabled()) LOG.debug("close");
			}
		};
	
	public synchronized void requestCleanup() {
		long now = System.currentTimeMillis();
		if (now - mLastCleanupTime >= getLong(getConfiguration(), "user.cleanup.intervaltime", CLEANUP_INTERVAL_TIME)) {
			mLastCleanupTime = now;
			
			boolean existed = getJob().existJob(mCleanupTask);
			if (LOG.isDebugEnabled())
				LOG.debug("requestCleanup: task exists: " + existed);
			
			if (existed == false)
				getJob().startJob(mCleanupTask);
		}
	}
	
	private void processCleanup(JobContext jc) throws ErrorException { 
		if (LOG.isDebugEnabled()) LOG.debug("processCleanup");
		
		ArrayList<IUser> list = new ArrayList<IUser>();
		
		synchronized (this) {
			//synchronized (mUserByKeys) { 
				//synchronized (mUserByNames) {
					for (IUser user : mUserByNames.values()) { 
						if (user != null && checkUserTimeout(getConfiguration(), user, true))
							list.add(user);
					}
				//}
			//}
		}
		
		for (IUser user : list) { 
			if (user == null) continue;
			if (LOG.isDebugEnabled())
				LOG.debug("processCleanup: user=" + user);
			
			String text = Strings.get(user.getPreference().getLanguage(), 
					"Timeout from \"%1$s\"");
			MessageHelper.notifyLog(user.getUserKey(), String.format(text, 
					TimeUtils.formatDate(user.getAccessTime())));
			
			user.close();
		}
	}
	
	private static final long CLEANUP_INTERVAL_TIME = 30 * 60 * 1000;
	private static final long USER_IDLE_TIMEOUT = 30 * 60 * 1000;
	private static final long CLIENT_IDLE_TIMEOUT = 30 * 60 * 1000;
	
	static boolean checkUserTimeout(IUser user, boolean checkClient) throws ErrorException { 
		return checkUserTimeout(UserManager.getInstance().getConfiguration(), user, checkClient);
	}
	
	static boolean checkUserTimeout(Configuration conf, IUser user, 
			boolean checkClient) throws ErrorException { 
		if (user != null && user.isClosed() == false) { 
			long utime = user.getAccessTime();
			long now = System.currentTimeMillis();
			
			if (now - utime >= getLong(conf, "user.idle.timeout", USER_IDLE_TIMEOUT))
				return true;
			
			if (checkClient && user instanceof IMember) { 
				IMember member = (IMember)user;
				IUserClient[] clients = member.getClients();
				
				if (clients != null) { 
					for (IUserClient client : clients) { 
						if (client == null) continue;
						if (checkClientTimeout(conf, client) == null) {
							String text = Strings.get(user.getPreference().getLanguage(), 
									"Timeout from \"%1$s\" by \"%2$s\"");
							MessageHelper.notifyLog(user.getUserKey(), String.format(text, 
									TimeUtils.formatDate(client.getAccessTime()), client.getDevice().toReadableTitle()));
						}
					}
				}
			}
		}
		
		return false;
	}
	
	static IUserClient checkClientTimeout(IUserClient client) { 
		return checkClientTimeout(UserManager.getInstance().getConfiguration(), client);
	}
	
	static IUserClient checkClientTimeout(Configuration conf, IUserClient client) { 
		if (client == null) return client;
		
		long ctime = client.getAccessTime();
		long now = System.currentTimeMillis();
		
		if (now - ctime >= getLong(conf, "user.client.idle.timeout", CLIENT_IDLE_TIMEOUT)) {
			if (LOG.isDebugEnabled())
				LOG.debug("checkClientTimeout: close timeout client: " + client);
			
			client.close();
			return null;
		}
		
		return client;
	}
	
	static long getLong(Configuration conf, String name, long def) {
		long val = conf != null ? conf.getLong(name, def) : def;
		return val >= 0 ? val : 0;
	}
	
}
