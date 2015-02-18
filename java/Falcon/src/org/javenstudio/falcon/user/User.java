package org.javenstudio.falcon.user;

import java.lang.ref.WeakReference;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.IDatabase;
import org.javenstudio.falcon.message.IMessageSet;
import org.javenstudio.falcon.message.MessageManager;
import org.javenstudio.falcon.publication.IPublicationStore;
import org.javenstudio.falcon.publication.PublicationManager;
import org.javenstudio.falcon.setting.cluster.IHostInfo;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.user.profile.Preference;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.user.profile.UserProfile;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.falcon.util.IdentityUtils;
import org.javenstudio.util.StringUtils;

public abstract class User implements IUser {
	private static final Logger LOG = Logger.getLogger(User.class);
	
	private final UserManager mManager;
	private final String mName;
	private final String mKey;
	private final int mType;
	
	private WeakReference<Profile> mProfileRef = null;
	private DataManager mDataManager = null;
	private MessageManager mMessageManager = null;
	private PublicationManager mPublicationManager = null;
	private Preference mPreference = null;
	
	private volatile IInviteSet mInvites = null;
	private volatile boolean mClosed = false;
	private volatile int mUserFlag = 0;
	private volatile long mObtainTime;
	private volatile long mModifiedTime;
	private volatile long mAccessTime;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return null; // top
			}
			@Override
			public String getName() {
				return "User(" + User.this.getUserName() + ")";
			}
			@Override
			protected void checkCurrent(ILockable.Type type, ILockable.Check check) 
					throws ErrorException { 
				super.checkCurrent(type, check);
				if (type == ILockable.Type.WRITE) {
					if (User.this.getUserFlag() == IUser.FLAG_READONLY) {
						throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
								getName() + " is locked as readonly");
					}
					if (User.this.getUserFlag() == IUser.FLAG_DISABLED) {
						throw new ErrorException(ErrorException.ErrorCode.FORBIDDEN, 
								getName() + " is disabled");
					}
				}
			}
		};
	
	protected User(UserManager manager, IUserData data) 
			throws ErrorException { 
		if (manager == null || data == null) 
			throw new NullPointerException();
		
		mManager = manager;
		mName = data.getUserName();
		mKey = data.getUserKey();
		mType = data.getUserType();
		mUserFlag = data.getUserFlag();
		
		if (mName == null || mName.length() == 0) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Username is empty");
		}
		
		if (UserHelper.checkUserKey(mKey) == false) { 
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"User key is wrong");
		}
		
		mObtainTime = System.currentTimeMillis();
		mModifiedTime = mObtainTime;
		mAccessTime = mObtainTime;
	}
	
	@Override
	public final String getUserName() {
		return mName;
	}
	
	@Override
	public final String getUserEmail() {
		IHostNode host = getUserManager().getStore().getHostNode();
		String name = mName;
		return toUserEmail(host, name);
	}
	
	public static String toUserEmail(IHostInfo host, String username) {
		if (host == null || username == null)
			return null;
		
		String adminUser = host.getAdminUser();
		String name = username;
		
		if (name.equalsIgnoreCase(adminUser)) {
			name = name + "#" + host.getHostKey();
		}
		
		String domain = host.getMailDomain();
		//if (domain == null || domain.length() == 0)
		//	domain = host.getClusterDomain();
		//if (domain == null || domain.length() == 0)
		//	domain = host.getHostDomain();
		//if (domain == null || domain.length() == 0)
		//	domain = host.getHostAddress();
		
		if (domain != null && domain.length() > 0) {
			name = StringUtils.replaceChar(name, '@', '.');
			name = name + "@" + domain;
		}
		
		return name.toLowerCase();
	}
	
	@Override
	public final String getUserKey() {
		return mKey;
	}
	
	@Override
	public final int getUserType() {
		return mType;
	}
	
	@Override
	public final UserManager getUserManager() {
		return mManager;
	}

	@Override
	public final ILockable.Lock getLock() { 
		return mLock;
	}
	
	@Override
	public final String getUserId() {
		return IdentityUtils.toIdentity(
				IdentityUtils.toIdentity(getUserManager().getKey(), 16) + mKey, 
				32);
	}
	
	@Override
	public synchronized final DataManager getDataManager() 
			throws ErrorException { 
		if (mDataManager == null || mDataManager.isClosed()) { 
			mDataManager = new DataManager(this, 
					mManager.getStore().getDatumCore());
			mDataManager.loadLibraries();
		}
		return mDataManager;
	}
	
	@Override
	public synchronized final MessageManager getMessageManager() 
			throws ErrorException {
		if (mMessageManager == null || mMessageManager.isClosed()) { 
			mMessageManager = new MessageManager(this);
		}
		return mMessageManager;
	}
	
	@Override
	public synchronized final PublicationManager getPublicationManager() 
			throws ErrorException {
		if (mPublicationManager == null || mPublicationManager.isClosed()) { 
			mPublicationManager = new PublicationManager(
				new IPublicationStore() {
					@Override
					public IDatabase getDatabase() throws ErrorException {
						return getDataManager().getDatabase();
					}
					@Override
					public String getUserName() {
						return User.this.getUserName();
					}
				});
			mPublicationManager.initUserServices();
		}
		return mPublicationManager;
	}
	
	@Override
	public synchronized final Preference getPreference() 
			throws ErrorException { 
		if (mPreference == null || mPreference.isClosed()) { 
			mPreference = new Preference(this, 
					mManager.getStore());
		}
		return mPreference;
	}
	
	@Override
	public synchronized final Profile getProfile() throws ErrorException { 
		WeakReference<Profile> ref = mProfileRef;
		Profile profile = ref != null ? ref.get() : null;
		if (profile == null || profile.isClosed()) {
			profile = new UserProfile(this, mManager.getStore());
			mProfileRef = new WeakReference<Profile>(profile);
		}
		return profile;
	}
	
	@Override
	public long getUsedSpace() {
		try {
			return getDataManager().getTotalFileLength();
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("getUsedSpace: error: " + e, e);
			
			return 0;
		}
	}
	
	@Override
	public long getUsableSpace() {
		try {
			long totalUsableSpace = getUserManager().getStore()
					.getDatumCore().getTotalUsableSpace();
			
			if (totalUsableSpace < 0)
				totalUsableSpace = 0;
			
			if (this instanceof IMember) {
				IMember member = (IMember)this;
				if (member.isAdministrator())
					return totalUsableSpace;
			}
			
			long freeSpace = getFreeSpace();
			long usedSpace = getUsedSpace();
			long purchasedSpace = getPurchasedSpace();
			
			long usableSpace = freeSpace + purchasedSpace - usedSpace;
			if (usableSpace > totalUsableSpace)
				usableSpace = totalUsableSpace;
			if (usableSpace < 0)
				usableSpace = 0;
			
			return usableSpace;
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("getUsableSpace: error: " + e, e);
		}
		
		return 0;
	}
	
	@Override
	public long getFreeSpace() {
		try {
			return getUserManager().getCategoryManager().getFreeSpace(this);
		} catch (Throwable e) {
			if (LOG.isWarnEnabled())
				LOG.warn("getFreeSpace: error: " + e, e);
			
			return 0;
		}
	}
	
	@Override
	public long getPurchasedSpace() {
		return 0;
	}
	
	@Override
	public long getCapacitySpace() {
		return getUsedSpace() + getUsableSpace();
	}
	
	@Override
	public int getUserFlag() {
		return mUserFlag;
	}
	
	public void setUserFlag(int flag) { 
		mUserFlag = flag;
	}
	
	@Override
	public IInviteSet getInvites() { 
		return mInvites;
	}
	
	public void setInvites(IInviteSet invites) { 
		mInvites = invites;
	}
	
	@Override
	public IMessageSet getMessages() throws ErrorException { 
		return getMessageManager().getNewMessages();
	}
	
	@Override
	public long getObtainTime() {
		return mObtainTime;
	}

	public void setObtainTime(long time) { 
		mObtainTime = time;
	}
	
	@Override
	public long getModifiedTime() {
		return mModifiedTime;
	}

	public void setModifiedTime(long time) { 
		mModifiedTime = time;
	}
	
	@Override
	public long getAccessTime() { 
		return mAccessTime;
	}
	
	public void setAccessTime(long time) { 
		mAccessTime = time;
	}
	
	@Override
	public boolean isClosed() { 
		return mClosed;
	}
	
	@Override
	public synchronized void close() {
		if (LOG.isDebugEnabled()) LOG.debug("close: " + this);
		mClosed = true;
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				DataManager dataManager = mDataManager;
				if (dataManager != null) dataManager.close();
				mDataManager = null;
				
				MessageManager messageManager = mMessageManager;
				if (messageManager != null) messageManager.close();
				mMessageManager = null;
				
				PublicationManager publishManager = mPublicationManager;
				if (publishManager != null) publishManager.close();
				mPublicationManager = null;
				
				Preference preference = mPreference;
				if (preference != null) preference.close();
				mPreference = null;
				
				WeakReference<Profile> ref = mProfileRef;
				Profile profile = ref != null ? ref.get() : null;
				if (profile != null) profile.close();
				mProfileRef = null;
				
				try { 
					getUserManager().removeUser(this);
				} catch (Throwable e) { 
					if (LOG.isWarnEnabled())
						LOG.warn("close: remove user error: " + e, e);
				}
			} finally { 
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}

	@Override
	public String toString() { 
		return getClass().getSimpleName() + "{key=" + mKey 
				+ ",name=" + mName + ",flag=" + mUserFlag + "}";
	}
	
}
