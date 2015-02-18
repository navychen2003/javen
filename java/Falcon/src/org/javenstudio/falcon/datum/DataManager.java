package org.javenstudio.falcon.datum;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.common.util.Strings;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.data.SqHelper;
import org.javenstudio.falcon.datum.index.IndexBuilder;
import org.javenstudio.falcon.datum.index.IndexSearcher;
import org.javenstudio.falcon.datum.table.TableManager;
import org.javenstudio.falcon.datum.util.BlobCacheHelper;
import org.javenstudio.falcon.user.IGroup;
import org.javenstudio.falcon.user.IUser;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.conf.Configuration;
import org.javenstudio.raptor.fs.Path;

public final class DataManager extends BlobCacheHelper 
		implements IDatabase.Manager {
	private static final Logger LOG = Logger.getLogger(DataManager.class);

    private final IUser mUser;
	private final String mStoreDir;
	private final String mCacheDir;
	
	private final IDatumCore mCore;
	private final DataJob mJob;
	private final IndexBuilder mIndexer;
	private final IDatabase mDatabase;
	
	private final Map<String, ILibrary> mLibraries = 
			new LinkedHashMap<String, ILibrary>();
	
	private volatile boolean mClosed = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return DataManager.this.getUser().getLock();
			}
			@Override
			public String getName() {
				return "DataManager(" + DataManager.this.getUser().getUserName() + ")";
			}
		};
	
	public DataManager(IUser user, IDatumCore core) 
			throws ErrorException { 
		this(core, user, core.getStoreDir(), core.getCacheDir());
	}
	
	private DataManager(IDatumCore core, IUser user, 
			String storeDir, String cacheDir) throws ErrorException { 
		if (core == null || user == null || storeDir == null || 
			cacheDir == null) 
			throw new NullPointerException();
		
		mCore = core;
		mUser = user;
		mStoreDir = storeDir;
		mCacheDir = cacheDir;
		mJob = new DataJob(this);
		mIndexer = new IndexBuilder(this);
		mDatabase = new TableManager(this);
	}
	
	public synchronized void loadLibraries() throws ErrorException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("loadLibraries: manager=" + this);
		
		getCore().getLibraryStore().loadLibraryList(this);
		
		if (getLibraryCount() == 0) {
			IUser user = getUser();
			if (user instanceof IGroup) {
				SqHelper.registerDefault(this, 
						Strings.get(user.getPreference().getLanguage(), 
								"Group File"));
			} else {
				SqHelper.registerDefault(this, 
						Strings.get(user.getPreference().getLanguage(), 
								"My File"));
			}
		}
	}
	
	public IDatumCore getCore() { return mCore; }
	public IUser getUser() { return mUser; }
	
	public DataJob getJob() { return mJob; }
	public IndexBuilder getIndexer() { return mIndexer; }
	public IndexSearcher getSearcher() { return mIndexer; }
	public IDatabase getDatabase() { return mDatabase; }
	
	public String getUserKey() { return mUser.getUserKey(); }
	public String getUserName() { return mUser.getUserName(); }
	public String getStoreDir() { return mStoreDir; }
	
	@Override
	public Configuration getConfiguration() { 
		return getCore().getConfiguration();
	}
	
	public ILibraryStore getLibraryStore() { 
		return getCore().getLibraryStore(); 
	}
	
	public Path getLibraryPath(ILibrary library) throws ErrorException { 
		return getLibraryStore().getLibraryPath(this, library);
	}
	
	@Override
	public IDatabaseStore getDatabaseStore() { 
		return getCore().getDatabaseStore(); 
	}
	
	@Override
	public Path getDatabasePath() throws ErrorException { 
		return getDatabaseStore().getDatabasePath(this);
	}
	
	public final boolean isClosed() { return mClosed; }
	public final ILockable.Lock getLock() { return mLock; }
	
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		mClosed = true;
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				getJob().close();
				getIndexer().close();
				
				synchronized (mLibraries) { 
					ILibrary[] libs = mLibraries.values().toArray(new ILibrary[0]);
					boolean changed = false;
					
					for (int i=0; libs != null && i < libs.length; i++) { 
						ILibrary lib = libs[i];
						if (lib != null && lib.isChanged()) 
							changed = true;
					}
					
					try {
						if (changed) saveLibraryList();
					} catch (Throwable e) { 
						if (LOG.isWarnEnabled())
							LOG.warn("close: save library error: " + e, e);
					}
					
					for (int i=0; libs != null && i < libs.length; i++) { 
						ILibrary lib = libs[i];
						if (lib != null) 
							lib.close();
					}
					
					//mDatas.clear();
					mLibraries.clear();
				}
				
				getDatabase().close();
			} finally { 
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	public void addLibrary(ILibrary library) throws ErrorException { 
		addLibrary(library, true);
	}
	
	public void addLibrary(ILibrary library, boolean save) throws ErrorException { 
		synchronized (mLibraries) { 
			if (addLibrary0(library) && save) 
				saveLibraryList();
		}
	}
	
	public ILibrary removeLibrary(String key) throws ErrorException { 
		return removeLibrary(key, true);
	}
	
	public ILibrary removeLibrary(String key, boolean save) throws ErrorException { 
		synchronized (mLibraries) { 
			ILibrary library = removeLibrary0(key);
			if (library != null && save)
				saveLibraryList();
			
			return library;
		}
	}
	
	public void saveLibraryList() throws ErrorException { 
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			synchronized (mLibraries) { 
				getLibraryStore().saveLibraryList(this, getLibraries());
			}
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	private boolean addLibrary0(ILibrary library) throws ErrorException {
		if (library == null) return false;
		
		final String key = library.getContentKey();
		if (key == null) throw new NullPointerException();
		
		if (library.getManager() != this) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Library: " + library.getName() + "(" + key + ")" + " has wrong manager");
		}
		
		synchronized (mLibraries) { 
			ILibrary lib = mLibraries.get(key);
			if (lib == library) 
				return false;
			
			if (lib != null && lib != library) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
						"Library: " + library.getName() + "(" + key + ")" + " already registered");
			}
			
			mLibraries.put(key, library);
			
			if (LOG.isDebugEnabled())
				LOG.debug("addLibrary: " + library);
			
			return true;
		}
	}
	
	private ILibrary removeLibrary0(String key) throws ErrorException { 
		if (key == null) return null;
		
		synchronized (mLibraries) { 
			ILibrary library = mLibraries.get(key);
			if (library == null) return null;
			
			if (mLibraries.size() <= 1) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Library: " + library.getName() + " is the only one and cannot be deleted");
			}
			
			if (library.isDefault()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Library: " + library.getName() + " is default and cannot be deleted");
			}
			
			if (library.getTotalFileLength() > 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Library: " + library.getName() + " is not empty and cannot be deleted");
			}
			
			if (!library.canDelete()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Library: " + library.getName() + " cannot be deleted");
			}
			
			for (int i=0; i < library.getSectionCount(); i++) { 
				ISectionRoot item = library.getSectionAt(i);
				if (item == null) continue;
				
				if (item.getSubCount() > 0 || item.getSubLength() > 0 || 
					item.getTotalFileCount() > 0 || item.getTotalFileLength() > 0) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Root: " + item.getName() + " is not empty and cannot be deleted");
				}
			}
			
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				if (LOG.isDebugEnabled())
					LOG.debug("removeLibrary: " + library);
				
				library.removeAndClose();
				
				ILibrary removed = mLibraries.remove(key);
				if (removed != library) {
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Library: " + library.getName() + " removed is wrong");
				}
				
				return removed;
			} finally {
				getLock().unlock(ILockable.Type.WRITE);
			}
		}
	}
	
	public String[] getLibraryKeys() { 
		synchronized (mLibraries) { 
			return mLibraries.keySet().toArray(new String[mLibraries.size()]);
		}
	}
	
	public ILibrary getLibrary(String key) { 
		synchronized (mLibraries) { 
			return mLibraries.get(key);
		}
	}
	
	public ILibrary[] getLibraries() { 
		synchronized (mLibraries) { 
			return mLibraries.values().toArray(new ILibrary[0]);
		}
	}
	
	public int getLibraryCount() { 
		synchronized (mLibraries) { 
			return mLibraries.size();
		}
	}
	
	public ILibrary getDefaultLibrary() { 
		synchronized (mLibraries) { 
			ILibrary first = null;
			for (ILibrary library : mLibraries.values()) { 
				if (library != null) { 
					if (library.isDefault()) return library;
					if (first == null) first = library;
				}
			}
			return first;
		}
	}
	
	public int getTotalFolderCount() {
		ILibrary[] libraries = getLibraries();
		int folderCount = 0;
		if (libraries != null) {
			for (ILibrary library : libraries) {
				if (library != null)
					folderCount += library.getTotalFolderCount();
			}
		}
		return folderCount;
	}
	
	public int getTotalFileCount() {
		ILibrary[] libraries = getLibraries();
		int fileCount = 0;
		if (libraries != null) {
			for (ILibrary library : libraries) {
				if (library != null)
					fileCount += library.getTotalFileCount();
			}
		}
		return fileCount;
	}
	
	public long getTotalFileLength() {
		ILibrary[] libraries = getLibraries();
		long fileLength = 0;
		if (libraries != null) {
			for (ILibrary library : libraries) {
				if (library != null)
					fileLength += library.getTotalFileLength();
			}
		}
		return fileLength;
	}
	
	@Override
	public synchronized String getBlobCacheDir() { 
		String cacheDir = mCacheDir;
    	String partition = "" + getUserKey().charAt(0);
    	String path = cacheDir + "/" + partition + "/" + getUserKey();
    	
    	File cacheFile = new File(cacheDir);
    	if (!cacheFile.exists())
    		cacheFile.mkdirs();
    	
    	File pathFile = new File(path);
    	if (!pathFile.exists()) 
    		pathFile.mkdirs();
    	
    	return path;
	}
	
}
