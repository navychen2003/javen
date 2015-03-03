package org.javenstudio.lightning.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IDatumCore;
import org.javenstudio.falcon.datum.StoreInfo;
import org.javenstudio.falcon.setting.cluster.IClusterManager;
import org.javenstudio.falcon.setting.cluster.IHostNode;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.user.IAuthService;
import org.javenstudio.falcon.user.IUserStore;
import org.javenstudio.falcon.user.UserManager;
import org.javenstudio.falcon.user.auth.AuthService;
import org.javenstudio.falcon.user.device.DeviceManager;
import org.javenstudio.falcon.user.global.AnnouncementManager;
import org.javenstudio.falcon.user.profile.ContactManager;
import org.javenstudio.falcon.user.profile.FriendManager;
import org.javenstudio.falcon.user.profile.GroupManager;
import org.javenstudio.falcon.user.profile.HistoryManager;
import org.javenstudio.falcon.user.profile.MemberManager;
import org.javenstudio.falcon.user.profile.Preference;
import org.javenstudio.falcon.user.profile.Profile;
import org.javenstudio.falcon.util.FsUtils;
import org.javenstudio.falcon.util.IOUtils;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedMap;
import org.javenstudio.falcon.util.SimpleXMLParser;
import org.javenstudio.falcon.util.SimpleXMLWriter;
import org.javenstudio.lightning.core.user.UserCore;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.dfs.DistributedFileSystem;
import org.javenstudio.raptor.dfs.DistributedFileSystem.DiskStatus;
import org.javenstudio.raptor.fs.FileSystem;
import org.javenstudio.raptor.fs.Path;
import org.javenstudio.raptor.util.StringUtils;

public class CoreStore implements IUserStore {
	private static final Logger LOG = Logger.getLogger(CoreStore.class);

	private final CoreContainers mContainers;
	private final UserManager mUserManager;
	private final String mLocalDir;
	private final String mCloudDir;
	
	private IDatumCore mDatumCore = null;
	private IAuthService mUserService = null;
	private StoreInfo[] mStoreInfos = null;
	
	public CoreStore(CoreContainers containers) throws ErrorException { 
		if (containers == null) throw new NullPointerException();
		mContainers = containers;
		mLocalDir = containers.getAdminConfig().getLocalStoreDir();
		mCloudDir = containers.getAdminConfig().getCloudStoreDir();
		mUserManager = new UserManager(this, mLocalDir);
	}
	
	public CoreContainers getContainers() { return mContainers; }
	public String getLocalStoreDir() { return mLocalDir; }
	public String getCloudStoreDir() { return mCloudDir; }
	
	@Override
	public Configuration getConfiguration() { 
		return getContainers().getAdminConfig().getConf(); 
	}
	
	public UserManager getUserManager() { 
		return mUserManager;
	}
	
	@Override
	public IHostNode getHostNode() { 
		return getContainers().getCluster().getHostSelf();
	}
	
	@Override
	public IClusterManager getClusterManager() {
		return getContainers().getCluster();
	}
	
	@Override
	public synchronized IDatumCore getDatumCore() throws ErrorException { 
		if (mDatumCore == null) { 
			for (Core core : getContainers().getCores()) { 
				if (core != null && core instanceof IDatumCore) { 
					mDatumCore = (IDatumCore)core;
					break;
				}
			}
			if (mDatumCore == null) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"DatumCore not found");
			}
		}
		return mDatumCore;
	}
	
	@Override
	public synchronized IAuthService getService() throws ErrorException { 
		if (mUserService == null) { 
			for (Core core : getContainers().getCores()) { 
				if (core != null && core instanceof UserCore) { 
					mUserService = new AuthService(this, core.getDataDir());
					break;
				}
			}
			if (mUserService == null) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"UserService init error");
			}
		}
		return mUserService;
	}
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		mUserManager.close();
		if (mUserService != null) mUserService.close();
	}
	
	@Override
	public String[] loadManagers() throws ErrorException { 
		ArrayList<String> list = new ArrayList<String>();
		
		String adminUser = getContainers().getAdminConfig().getAdminUser();
		if (adminUser == null) adminUser = "";
		
		String adminList = getContainers().getSetting().getGlobal().getAdminList();
		if (adminList != null && adminList.length() > 0)
			adminUser += " " + adminList;
		
		if (LOG.isDebugEnabled())
			LOG.debug("loadAdministrators: users=" + adminUser);
		
		StringTokenizer st = new StringTokenizer(adminUser, " \t\r\n,");
		while (st.hasMoreTokens()) { 
			String name = StringUtils.trim(st.nextToken());
			if (name != null && name.length() > 0)
				list.add(name);
		}
		
		return list.toArray(new String[list.size()]);
	}
	
	@Override
	public Path getAuthStorePath(String dirname, String name, 
			boolean forWrite) throws ErrorException { 
		FileSystem fs = getAuthStoreFs();
		boolean isLocal = FsUtils.isLocalFs(fs);
		
		if (isLocal) { 
			File authDir = getAuthDirFile(dirname, forWrite);
			if (name != null && name.length() > 0) { 
				File dir = new File(authDir, name);
				if (forWrite) {
					if (!dir.exists() && !dir.mkdirs()) {
						throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
								"Dir: " + dir + " create failed");
					}
				}
				authDir = dir;
			}
			return new Path(authDir.getAbsolutePath());
			
		} else { 
			Path authDir = getAuthDirPath(dirname, forWrite);
			if (name != null && name.length() > 0)
				authDir = new Path(authDir, name);
			
			return authDir;
		}
	}
	
	@Override
	public FileSystem getAuthStoreFs() throws ErrorException { 
		return getStoreFs(getStoreUri(null));
	}
	
	private class LocalStoreInfo extends StoreInfo {
		private final File mStorePath;
		private volatile long mUpdateTime = 0;
		private volatile long mTotalSpace = 0;
		private volatile long mFreeSpace = 0;
		private volatile long mUsableSpace = 0;
		
		public LocalStoreInfo(String scheme, String name, String location, File path) {
			super(scheme, name, location);
			if (path == null) throw new NullPointerException();
			if (!path.exists()) throw new IllegalArgumentException("Store path: " + path + " not existed");
			if (!path.isDirectory()) throw new IllegalArgumentException("Store path: " + path + " is not directory");
			mStorePath = path;
		}
		
		private void checkSpaces() {
			synchronized (mStorePath) {
				long current = System.currentTimeMillis();
				if (current - mUpdateTime < 60 * 1000)
					return;
				
				try {
					mTotalSpace = mStorePath.getTotalSpace();
					mFreeSpace = mStorePath.getFreeSpace();
					mUsableSpace = mStorePath.getUsableSpace();
				} catch (Throwable e) {
					if (LOG.isWarnEnabled())
						LOG.warn("checkSpaces: error: " + e, e);
				}
				
				mUpdateTime = current;
			}
		}
		
		@Override
		public long getUsedSpace() {
			checkSpaces();
			return mTotalSpace - mFreeSpace;
		}
		
		@Override
		public long getUsableSpace() {
			checkSpaces();
			return mUsableSpace;
		}
		
		@Override
		public long getCapacitySpace() {
			checkSpaces();
			return mTotalSpace;
		}
	}
	
	private class DfsStoreInfo extends StoreInfo {
		private final Object mLock = new Object();
		private volatile long mUpdateTime = 0;
		private volatile long mTotalSpace = 0;
		private volatile long mFreeSpace = 0;
		private volatile long mUsableSpace = 0;
		
		public DfsStoreInfo(String scheme, String name, String location) {
			super(scheme, name, location);
		}
		
		private void checkSpaces() {
			synchronized (mLock) {
				long current = System.currentTimeMillis();
				if (current - mUpdateTime < 60 * 1000)
					return;
				
				try {
					FileSystem fs = getStoreFs(getStoreUri());
					if (fs != null && fs instanceof DistributedFileSystem) {
						DistributedFileSystem dfs = (DistributedFileSystem) fs;
						DiskStatus ds = dfs.getDiskStatus();
						@SuppressWarnings("unused")
						long capacity = ds.getCapacity();
						long used = ds.getDfsUsed();
						long remaining = ds.getRemaining();
						long presentCapacity = used + remaining;
						
						mTotalSpace = presentCapacity;
						mFreeSpace = remaining;
						mUsableSpace = remaining;
					}
				} catch (Throwable e) {
					if (LOG.isWarnEnabled())
						LOG.warn("checkSpaces: error: " + e, e);
				}
				
				mUpdateTime = current;
			}
		}
		
		@Override
		public long getUsedSpace() {
			checkSpaces();
			return mTotalSpace - mFreeSpace;
		}
		
		@Override
		public long getUsableSpace() {
			checkSpaces();
			return mUsableSpace;
		}
		
		@Override
		public long getCapacitySpace() {
			checkSpaces();
			return mTotalSpace;
		}
	}
	
	private final Object mStoreLock = new Object();
	private long mTotalUsableSpace = 0;
	private long mStoreUpdateTime = 0;
	
	public long getTotalUsableSpace() {
		synchronized (mStoreLock) {
			long current = System.currentTimeMillis();
			if (current - mStoreUpdateTime >= 60 * 1000) {
				long usableSpace = 0;
				try {
					StoreInfo[] storeInfos = getStoreInfos();
					if (storeInfos != null) {
						for (StoreInfo storeInfo : storeInfos) {
							if (storeInfo == null) continue;
							usableSpace += storeInfo.getUsableSpace();
						}
					}
				} catch (Throwable e) {
					if (LOG.isWarnEnabled())
						LOG.warn("getTotalUsableSpace: error: " + e, e);
				}
				
				mTotalUsableSpace = usableSpace;
				mStoreUpdateTime = current;
			}
			
			return mTotalUsableSpace;
		}
	}
	
	public synchronized StoreInfo[] getStoreInfos() throws ErrorException { 
		if (mStoreInfos != null) return mStoreInfos;
		if (LOG.isDebugEnabled()) LOG.debug("getStoreInfos");
		
		List<StoreInfo> storeInfos = new ArrayList<StoreInfo>();
		String[] uris = getContainers().getAdminConfig().getStoreUris();
		
		for (int i=0; uris != null && i < uris.length; i++) { 
			String name = uris[i];
			if (name != null && name.startsWith("dfs:")) {
				String location = name;
				storeInfos.add(new DfsStoreInfo("dfs", name, location));
			}
		}
		
		if (storeInfos.size() == 0) { 
			String storeDir = getLocalStoreDir();
			if (storeDir != null && storeDir.length() > 0) {
				String path = getContainers().getAdminConfig().toCanonicalDir(storeDir);
				if (path == null || path.length() == 0) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Local store dir: " + path + " is empty");
				}
				
				String storeUri = path;
				String name = FsUtils.normalizePath(path);
				
				if (storeUri.startsWith("file:") == false)
					storeUri = "file:/" + storeUri;
				
				storeInfos.add(new LocalStoreInfo("file", name, storeUri, 
						getRootDirFile()));
			}
		}
		
		mStoreInfos = storeInfos.toArray(new StoreInfo[storeInfos.size()]);
		return mStoreInfos;
	}
	
	public String getStoreUri(String uri) throws ErrorException { 
		if (!getContainers().isInited()) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"CoreContainers not initialized");
		}
		
		if (uri == null || uri.length() == 0) { 
			StoreInfo[] stores = getStoreInfos();
			for (int i=0; stores != null && i < stores.length; i++) { 
				StoreInfo storeInfo = stores[i];
				if (storeInfo != null) { 
					uri = storeInfo.getStoreUri();
					break;
				}
			}
			
			if (uri == null || uri.length() == 0)
				uri = "file:///";
		}
		
		return uri;
	}
	
	public FileSystem getStoreFs(String uri) throws ErrorException { 
		if (!getContainers().isInited()) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"CoreContainers not initialized");
		}
		
		if (uri == null || uri.length() == 0) 
			uri = getStoreUri(uri);
		
		if (uri == null || uri.length() == 0 || uri.startsWith("file:")) {
			//uri = "file:///";
			return getLocalFs();
		} else if (uri.startsWith("dfs:")) { 
			return getCloudFs(uri);
		} else { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Store system: " + uri + " not supported");
		}
	}
	
	public FileSystem getLocalFs() throws ErrorException {
		try { 
			return FileSystem.getLocal(getConfiguration());
		}  catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public FileSystem getCloudFs(String uri) throws ErrorException {
		try { 
			if (uri != null && uri.length() > 0)
				return FileSystem.get(new URI(uri), getConfiguration());
			else
				return FileSystem.get(getConfiguration());
		//} catch (ErrorException e) {
		//	throw e;
		} catch (Throwable e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public final File getRootDirFile() throws ErrorException { 
		File rootDir = new File(getLocalStoreDir());
		if (!rootDir.exists() && !rootDir.mkdir()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Dir: " + rootDir + " create failed");
		}
		
		return rootDir;
	}
	
	public final File getAuthDirFile(String dirname, 
			boolean forWrite) throws ErrorException { 
		File dir = new File(getRootDirFile(), "auth");
		if (!dir.exists() && !dir.mkdir()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Dir: " + dir + " create failed");
		}
		
		if (dirname != null && dirname.length() > 0) { 
			dir = new File(dir, dirname);
			if (forWrite) {
				if (!dir.exists() && !dir.mkdirs()) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Dir: " + dir + " create failed");
				}
			}
		}
		
		return dir;
	}
	
	public final File getDataDirFile() throws ErrorException { 
		File dir = new File(getRootDirFile(), "data");
		if (!dir.exists() && !dir.mkdir()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Dir: " + dir + " create failed");
		}
		
		return dir;
	}
	
	public final Path getRootDirPath() throws ErrorException { 
		return new Path(getCloudStoreDir(), getHostNode().getHostKey());
	}
	
	public final Path getAuthDirPath(String dirname, 
			boolean forWrite) throws ErrorException { 
		Path path = new Path(getRootDirPath(), "auth");
		
		if (dirname != null && dirname.length() > 0) 
			path = new Path(path, dirname);
		
		return path;
	}
	
	public final Path getDataDirPath() throws ErrorException { 
		return new Path(getRootDirPath(), "data");
	}
	
	public final File getUserDirFile(String userId) throws ErrorException { 
		if (userId == null || userId.length() == 0)
			throw new NullPointerException("UserId is null or empty");
		
		if (userId.equals(IUser.SYSTEM)) {
			File userDir = new File(getDataDirFile(), ".sys");
			if (!userDir.exists() && !userDir.mkdir()) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Dir: " + userDir + " create failed");
			}
			
			return userDir;
		} else {
			String partition = "" + userId.charAt(0);
			File partitionDir = new File(getDataDirFile(), partition);
			if (!partitionDir.exists() && !partitionDir.mkdir()) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Dir: " + partitionDir + " create failed");
			}
			
			File userDir = new File(partitionDir, userId);
			if (!userDir.exists() && !userDir.mkdir()) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Dir: " + userDir + " create failed");
			}
			
			return userDir;
		}
	}
	
	public final Path getUserDirPath(String userId) throws ErrorException { 
		if (userId == null || userId.length() == 0)
			throw new NullPointerException("UserId is null or empty");
		
		if (userId.equals(IUser.SYSTEM)) {
			return new Path(getDataDirPath(), ".sys");
			
		} else {
			String partition = "" + userId.charAt(0);
			return new Path(getDataDirPath(), partition + "/" + userId);
		}
	}
	
	private static final String PROFILE = "profile";
	private static final String PREFERENCE = "preference";
	private static final String CONTACTLIST = "contactlist";
	private static final String FRIENDLIST = "friendlist";
	private static final String MEMBERLIST = "memberlist";
	private static final String DEVICESETTING = "devicesetting";
	private static final String GROUPLIST = "grouplist";
	private static final String GROUPINVITELIST = "groupinvitelist";
	private static final String FRIENDINVITELIST = "friendinvitelist";
	private static final String ANNOUNCEMENTLIST = "announcementlist";
	private static final String HISTORYLIST = "historylist";
	
	@Override
	public NamedList<Object> loadAnnouncementList(AnnouncementManager manager) 
			throws ErrorException { 
		return loadXml(ANNOUNCEMENTLIST);
	}
	
	@Override
	public void saveAnnouncementList(AnnouncementManager manager, 
			NamedList<Object> items) throws ErrorException { 
		saveXml(ANNOUNCEMENTLIST, items);
	}
	
	@Override
	public NamedList<Object> loadHistoryList(HistoryManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), HISTORYLIST);
	}
	
	@Override
	public void saveHistoryList(HistoryManager manager, 
			NamedList<Object> items) throws ErrorException { 
		saveXml(manager.getUser(), HISTORYLIST, items);
	}
	
	@Override
	public NamedList<Object> loadGroupList(GroupManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), GROUPLIST);
	}
	
	@Override
	public void saveGroupList(GroupManager manager, 
			NamedList<Object> items) throws ErrorException { 
		saveXml(manager.getUser(), GROUPLIST, items);
	}
	
	@Override
	public NamedList<Object> loadInviteList(GroupManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), GROUPINVITELIST);
	}
	
	@Override
	public void saveInviteList(GroupManager manager, 
			NamedList<Object> items) throws ErrorException { 
		saveXml(manager.getUser(), GROUPINVITELIST, items);
	}
	
	@Override
	public void saveProfile(Profile profile, 
			NamedList<Object> items) throws ErrorException { 
		saveXml(profile.getUser(), PROFILE, items);
	}
	
	@Override
	public NamedList<Object> loadProfile(Profile profile) 
			throws ErrorException { 
		return loadXml(profile.getUser(), PROFILE);
	}
	
	@Override
	public void savePreference(Preference preference, 
			NamedList<Object> items) throws ErrorException { 
		saveXml(preference.getUser(), PREFERENCE, items);
	}
	
	@Override
	public NamedList<Object> loadPreference(Preference preference) 
			throws ErrorException { 
		return loadXml(preference.getUser(), PREFERENCE);
	}
	
	@Override
	public void saveContactList(ContactManager manager, 
			NamedList<Object> items) throws ErrorException {
		saveXml(manager.getUser(), CONTACTLIST, items);
	}
	
	@Override
	public NamedList<Object> loadContactList(ContactManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), CONTACTLIST);
	}
	
	@Override
	public void saveFriendList(FriendManager manager, 
			NamedList<Object> items) throws ErrorException {
		saveXml(manager.getUser(), FRIENDLIST, items);
	}
	
	@Override
	public NamedList<Object> loadFriendList(FriendManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), FRIENDLIST);
	}
	
	@Override
	public void saveInviteList(FriendManager manager, 
			NamedList<Object> items) throws ErrorException {
		saveXml(manager.getUser(), FRIENDINVITELIST, items);
	}
	
	@Override
	public NamedList<Object> loadInviteList(FriendManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), FRIENDINVITELIST);
	}
	
	@Override
	public void saveMemberList(MemberManager manager, 
			NamedList<Object> items) throws ErrorException {
		saveXml(manager.getUser(), MEMBERLIST, items);
	}
	
	@Override
	public NamedList<Object> loadMemberList(MemberManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), MEMBERLIST);
	}
	
	@Override
	public void saveInviteList(MemberManager manager, 
			NamedList<Object> items) throws ErrorException {
		saveXml(manager.getUser(), GROUPINVITELIST, items);
	}
	
	@Override
	public NamedList<Object> loadInviteList(MemberManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), GROUPINVITELIST);
	}
	
	@Override
	public void saveDeviceSetting(DeviceManager manager, 
			NamedList<Object> items) throws ErrorException {
		saveXml(manager.getUser(), DEVICESETTING, items);
	}
	
	@Override
	public NamedList<Object> loadDeviceSetting(DeviceManager manager) 
			throws ErrorException { 
		return loadXml(manager.getUser(), DEVICESETTING);
	}
	
	private File getXmlFile(String name) throws ErrorException { 
		return new File(getRootDirFile(), name + ".xml");
	}
	
	private Path getXmlPath(String name) throws ErrorException { 
		return new Path(getRootDirPath(), name + ".xml");
	}
	
	private File getXmlFile(String userId, String name) throws ErrorException { 
		return new File(getUserDirFile(userId), name + ".xml");
	}
	
	private Path getXmlPath(String userId, String name) throws ErrorException { 
		return new Path(getUserDirPath(userId), name + ".xml");
	}
	
	public void saveXml(String name, NamedList<Object> items) 
			throws ErrorException {
		saveXml(name, name, items);
	}
	
	public void saveXml(String filename, String rootname, 
			NamedList<Object> items) throws ErrorException {
		FileSystem fs = getStoreFs(null);
		if (fs != null && !FsUtils.isLocalFs(fs)) { 
			Path fileXml = getXmlPath(filename);
			CoreStore.writeXml(fs, fileXml, items, rootname);
		} else {
			File fileXml = getXmlFile(filename);
			CoreStore.writeXml(fileXml, items, rootname);
		}
	}
	
	public NamedList<Object> loadXml(String name) throws ErrorException { 
		return loadXml(name, name);
	}
	
	public NamedList<Object> loadXml(String filename, 
			String rootname) throws ErrorException { 
		FileSystem fs = getStoreFs(null);
		if (fs != null && !FsUtils.isLocalFs(fs)) { 
			Path fileXml = getXmlPath(filename);
			return CoreStore.readXml(fs, fileXml, rootname);
		} else {
			File fileXml = getXmlFile(filename);
			return CoreStore.readXml(fileXml, rootname);
		}
	}
	
	public void saveXml(IUser user, String name, 
			NamedList<Object> items) throws ErrorException {
		saveXml(user, name, name, items);
	}
	
	public void saveXml(IUser user, String filename, String rootname, 
			NamedList<Object> items) throws ErrorException {
		FileSystem fs = getStoreFs(null);
		if (fs != null && !FsUtils.isLocalFs(fs)) { 
			Path fileXml = getXmlPath(user.getUserKey(), filename);
			CoreStore.writeXml(fs, fileXml, items, rootname);
		} else {
			File fileXml = getXmlFile(user.getUserKey(), filename);
			CoreStore.writeXml(fileXml, items, rootname);
		}
	}
	
	public NamedList<Object> loadXml(IUser user, String name) 
			throws ErrorException { 
		return loadXml(user, name, name);
	}
	
	public NamedList<Object> loadXml(IUser user, String filename, 
			String rootname) throws ErrorException { 
		FileSystem fs = getStoreFs(null);
		if (fs != null && !FsUtils.isLocalFs(fs)) { 
			Path fileXml = getXmlPath(user.getUserKey(), filename);
			return CoreStore.readXml(fs, fileXml, rootname);
		} else {
			File fileXml = getXmlFile(user.getUserKey(), filename);
			return CoreStore.readXml(fileXml, rootname);
		}
	}
	
	public static NamedList<Object> readXml(File xmlfile, 
			String rootTag) throws ErrorException { 
		NamedList<Object> items = null;
		try {
			items = readXml(new FileInputStream(xmlfile), rootTag);
			
			if (LOG.isDebugEnabled())
				LOG.debug("load: " + xmlfile.getName() + ": " + items);
		} catch (FileNotFoundException ex) {
			// ignore
		}
		
		if (items == null)
			items = new NamedMap<Object>();
		
		return items;
	}
	
	public static NamedList<Object> readXml(FileSystem fs, Path path, 
			String rootTag) throws ErrorException { 
		NamedList<Object> items = null;
		try {
			items = readXml(fs.open(path), rootTag);
			
			if (LOG.isDebugEnabled())
				LOG.debug("load: " + path + ": " + items);
		} catch (FileNotFoundException ex) {
			// ignore
		} catch (IOException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("readXml: " + path + " error: " + e, e);
		}
		
		if (items == null)
			items = new NamedMap<Object>();
		
		return items;
	}
	
	public static NamedList<Object> readXml(InputStream in, 
			String rootTag) throws ErrorException { 
		NamedList<Object> items = null;
		try { 
			SimpleXMLParser parser = new SimpleXMLParser(rootTag);
			items = parser.parse(in, "UTF-8");
		} finally { 
			IOUtils.closeQuietly(in);
		}
		
		if (items == null)
			items = new NamedMap<Object>();
		
		return items;
	}
	
	public static void writeXml(File xmlfile, NamedList<Object> items, 
			String rootTag) throws ErrorException { 
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("save: " + xmlfile + ": " + items);
			
			writeXml(new FileOutputStream(xmlfile), items, rootTag);
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public static void writeXml(FileSystem fs, Path path, 
			NamedList<Object> items, String rootTag) throws ErrorException { 
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("save: " + path + ": " + items);
			
			writeXml(fs.create(path), items, rootTag);
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public static void writeXml(OutputStream out, NamedList<Object> items, 
			String rootTag) throws ErrorException { 
		try {
			OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
			SimpleXMLWriter xmlwriter = new SimpleXMLWriter(
					writer, rootTag, true);
			
			xmlwriter.write(items);
			xmlwriter.close();
			
			writer.flush();
			writer.close();
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
}
