package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.DataCache;
import org.javenstudio.falcon.datum.DataManager;
import org.javenstudio.falcon.datum.IData;
import org.javenstudio.falcon.datum.IFileInfo;
import org.javenstudio.falcon.datum.IFolderInfo;
import org.javenstudio.falcon.datum.ILibrary;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionCollector;
import org.javenstudio.falcon.datum.ISectionQuery;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.datum.ISectionSet;
import org.javenstudio.falcon.datum.LibraryBase;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.datum.SectionQuery;
import org.javenstudio.falcon.datum.data.recycle.SqRecycleRoot;
import org.javenstudio.falcon.datum.util.BytesBufferPool;
import org.javenstudio.falcon.datum.util.ImageUtils;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.fs.FileSystem;

public final class SqLibrary extends LibraryBase 
		implements ILibrary, IFolderInfo {
	private static final Logger LOG = Logger.getLogger(SqLibrary.class);

	static SqLibrary create(DataManager manager, 
			FileSystem fs, SqAcceptor filter, String contentType, 
			String key, String name, String hostName, int maxEntries) 
			throws ErrorException { 
		if (manager == null || fs == null || contentType == null || name == null) 
			throw new NullPointerException();
		
		if (key == null) {
			key = SectionHelper.newLibraryKey("SqLibrary@" 
				+ System.currentTimeMillis() + "{" + name + "}");
		}
		
		return new SqLibrary(manager, fs, key, filter, contentType, 
				name, hostName, maxEntries);
	}
	
	private final FileSystem mFs;
	private final SqAcceptor mFilter;
	private final String mContentType;
	private final String mHostName;
	private final boolean mLocalFs;
	private final String mKey;
	private String mName;
	private int mMaxEntries;
	
	private final List<SqRoot> mRootList;
	private final Map<String,SqRoot> mRootMap;
	private SqSectionList mSectionList = null;
	
	private DataCache mCache = null;
	private long mCreatedTime = 0;
	private long mModifiedTime = 0;
	private long mIndexedTime = 0;
	private long mOptimizeTime = 0;
	private boolean mCanRead = true;
	private boolean mCanWrite = true;
	private boolean mCanMove = false;
	private boolean mCanDelete = true;
	private boolean mCanCopy = false;
	private boolean mChanged = false;
	private boolean mIsDefault = false;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return SqLibrary.this.getManager().getLock();
			}
			@Override
			public String getName() {
				return "Library(" + SqLibrary.this.getName() + ")";
			}
		};
	
	private SqLibrary(DataManager manager, FileSystem fs, 
			String key, SqAcceptor filter, String contentType, 
			String name, String hostName, int maxEntries) 
			throws ErrorException { 
		super(manager);
		SectionHelper.checkKey(key);
		mKey = key;
		mRootList = new ArrayList<SqRoot>();
		mRootMap = new HashMap<String,SqRoot>();
		mFs = fs;
		mLocalFs = fs.getUri().getScheme().equalsIgnoreCase("file");
		mFilter = filter;
		mContentType = contentType;
		mHostName = hostName;
		mName = name;
		mMaxEntries = maxEntries > 0 ? maxEntries : DataCache.MAX_ENTRIES;
		mCreatedTime = System.currentTimeMillis();
		mModifiedTime = mCreatedTime;
		onCreated();
	}
	
	public final FileSystem getFs() { return mFs; }
	public final FileSystem getStoreFs() { return mFs; }
	
	public final ILockable.Lock getLock() { 
		return mLock; 
	}
	
	@Override
	public String getOwner() { 
		try {
			return getManager().getUser().getUserName();
		} catch (Throwable e) { 
			if (LOG.isDebugEnabled())
				LOG.debug("getOwner: error: " + e, e);
			
			return "";
		}
	}
	
	@Override
	public String getHostName() { 
		if (mLocalFs)
			return getManager().getCore().getFriendlyName();
		
		return mHostName;
	}
	
	public SqAcceptor getAcceptor() { 
		return mFilter;
	}
	
	@Override
	public final String getContentKey() { 
		return mKey;
	}
	
	@Override
	public String getContentType() { 
		return mContentType;
	}
	
	@Override
	public int getMaxEntries() { 
		return mMaxEntries;
	}
	
	@Override
	public boolean equals(Object o) { 
		if (this == o) return true;
		if (o == null || !(o instanceof SqLibrary)) 
			return false;
		
		SqLibrary other = (SqLibrary)o;
		return this.getManager() == other.getManager() && 
				this.getContentKey().equals(other.getContentKey());
	}
	
	@Override
	public synchronized DataCache getCache() { 
		if (mCache == null) {
			mCache = new DataCache(getManager(), 
					getContentKey(), getMaxEntries());
		}
		return mCache;
	}

	public void putImageData(String key, byte[] data) { 
		getCache().putImageData(key, data);
	}
	
	public BytesBufferPool.BytesBuffer getImageData(String key) { 
		BytesBufferPool.BytesBuffer buffer = getManager().getCore().getBufferPool().get();
		if (getCache().getImageData(key, buffer)) 
			return buffer;
		
		return null;
	}
	
	public void putMetaData(String key, byte[] data) { 
		getCache().putMetaData(key, data);
	}
	
	public BytesBufferPool.BytesBuffer getMetaData(String key) { 
		BytesBufferPool.BytesBuffer buffer = getManager().getCore().getBufferPool().get();
		if (getCache().getMetaData(key, buffer)) 
			return buffer;
		
		return null;
	}
	
	public void clearCacheData(String key) { 
		if (LOG.isDebugEnabled())
			LOG.debug("clearCacheData: key=" + key);
		
		getCache().clearMetaData(key);
		getCache().clearImageData(key);
		
		String[] cacheKeys = ImageUtils.getThumbnailCacheKeys(key);
		if (cacheKeys != null) { 
			for (String cacheKey : cacheKeys) { 
				getCache().clearImageData(cacheKey);
			}
		}
	}
	
	@Override
	public String getName() {
		return mName;
	}

	public void setName(String name) { 
		if (name != null && name.length() > 0)
			mName = name;
	}
	
	@Override
	public long getCreatedTime() { 
		return mCreatedTime;
	}
	
	public void setCreatedTime(long time) { 
		mCreatedTime = time;
	}
	
	@Override
	public long getModifiedTime() { 
		return mModifiedTime;
	}
	
	public void setModifiedTime(long time) { 
		mModifiedTime = time;
	}
	
	@Override
	public long getIndexedTime() { 
		return mIndexedTime;
	}
	
	public void setIndexedTime(long time) { 
		mIndexedTime = time;
	}
	
	@Override
	public long getOptimizedTime() { 
		return mOptimizeTime;
	}
	
	public void setOptimizedTime(long time) { 
		mOptimizeTime = time;
	}
	
	@Override
	public boolean isChanged() { 
		return mChanged;
	}
	
	public void setChanged(boolean changed) { 
		mChanged = changed;
	}
	
	@Override
	public boolean isDefault() { 
		return mIsDefault;
	}
	
	public void setDefault(boolean def) { 
		mIsDefault = def;
	}
	
	@Override
	public int getTotalFolderCount() { 
		int count = 0;
		for (int i=0; i < getSectionCount(); i++) { 
			SqRoot root = getSectionAt(i);
			if (root != null) 
				count += root.getTotalFolderCount();
		}
		return count;
	}
	
	@Override
	public int getTotalFileCount() { 
		int count = 0;
		for (int i=0; i < getSectionCount(); i++) { 
			SqRoot root = getSectionAt(i);
			if (root != null) 
				count += root.getTotalFileCount();
		}
		return count;
	}
	
	@Override
	public long getTotalFileLength() { 
		long length = 0;
		for (int i=0; i < getSectionCount(); i++) { 
			SqRoot root = getSectionAt(i);
			if (root != null) 
				length += root.getTotalFileLength();
		}
		return length;
	}
	
	@Override
	public int getSectionCount() {
		synchronized (mRootList) {
			return mRootList.size();
		}
	}

	@Override
	public SqRoot getSectionAt(int index) { 
		synchronized (mRootList) {
			return index >= 0 && index < mRootList.size() ? 
				mRootList.get(index) : null;
		}
	}
	
	@Override
	public SqSection getSection(String key) throws ErrorException { 
		if (key == null) return null;
		
		if (LOG.isDebugEnabled())
			LOG.debug("getSection: key=" + key);
		
		String rootKey = key;
		String fileKey = null;
		
		if (key.length() != 8) {
			String[] keys = SectionHelper.splitKeys(key);
			if (keys != null) {
				if (keys.length == 1) { 
					rootKey = keys[0];
				} else if (keys.length == 4) { 
					rootKey = keys[2];
					fileKey = keys[3];
				}
			}
		}
		
		if (rootKey != null) {
			final SqRoot root;
			getLock().lock(ILockable.Type.READ, null);
			try {
				synchronized (mRootList) {
					root = mRootMap.get(rootKey);
				}
			} finally {
				getLock().unlock(ILockable.Type.READ);
			}
			
			if (root != null) { 
				if (fileKey == null || key.equals(root.getContentId()))
					return (SqSection)root;
				
				return (SqSection)root.getSection(fileKey);
			}
		}
		
		return null; 
	}
	
	@Override
	public SqSectionSet getSections(ISectionQuery query) 
			throws ErrorException { 
		SqSectionList list = getSectionList(query.getCollector());
		return list != null ? list.getSectionSet(query) : null;
	}
	
	public boolean addRoot(SqRoot root) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("addRoot: root=" + root);
		
		if (root == null) 
			throw new NullPointerException("SectionRoot is null");
		if (root.getLibrary() != this) 
			throw new IllegalArgumentException("SectionRoot has wrong library");
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			synchronized (mRootList) {
				SqRoot r = mRootMap.get(root.getContentKey());
				if (r != null) { 
					if (r == root) return false;
					
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Root: " + root.getContentKey() + " already exists");
				}
				
				for (SqRoot rt : mRootList) { 
					if (rt == root) return false;
				}
				
				mRootList.add(root);
				mRootMap.put(root.getContentKey(), root);
			}
			reset();
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
		
		return true;
	}
	
	public boolean removeRoot(SqRoot root) throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("removeRoot: root=" + root);
		
		if (root == null) 
			throw new NullPointerException("SectionRoot is null");
		if (root.getLibrary() != this) 
			throw new IllegalArgumentException("SectionRoot has wrong library");
		
		boolean result = false;
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			synchronized (mRootList) {
				SqRoot r = mRootMap.get(root.getContentKey());
				if (r != root) { 
					throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
							"Root: " + root.getContentKey() + " not found");
				}
				
				result = mRootList.remove(root);
			}
			reset();
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
		
		return result;
	}
	
	@Override
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				DataCache cache = mCache;
				if (cache != null)
					cache.close();
				mCache = null;
				
				synchronized (mRootList) {
					for (SqRoot rt : mRootList) { 
						if (rt != null) rt.close();
					}
					
					mRootList.clear();
					mRootMap.clear();
				}
				
				reset();
				super.close();
			} finally {
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (ErrorException e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	public void reset() throws ErrorException { 
		if (LOG.isDebugEnabled()) LOG.debug("reset");
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			synchronized (mRootList) {
				mSectionList = null;
				mFolders = null;
				mFiles = null;
				
				for (SqRoot rt : mRootList) { 
					if (rt == null) continue;
					if (!rt.getLock().isLocked(ILockable.Type.WRITE, false)) 
						rt.reset();
				}
			}
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	SqSectionList getSectionList(ISectionCollector collector) 
			throws ErrorException { 
		synchronized (mRootList) {
			if (mSectionList == null || collector != null) { 
				SqSection[] sections = mRootList.toArray(new SqSection[mRootList.size()]);
				if (sections == null) 
					sections = new SqSection[0];
				
				if (collector != null) { 
					for (SqSection section : sections) { 
						if (section == null) continue;
						collector.addSection(section);
						if (section.getModifiedTime() > getIndexedTime())
							collector.addModified(section);
					}
					
					//for (SqSection section : mRemoved) { 
					//	if (section == null) continue;
					//	collector.addDeleted(section.getSectionId());
					//}
				}
				
				mSectionList = new SqSectionList(sections);
			}
			return mSectionList;
		}
	}

	@Override
	public IFolderInfo getFolderInfo() {
		return this;
	}
	
	private final Object mFileLock = new Object();
	private IFolderInfo[] mFolders = null;
	private IFileInfo[] mFiles = null;
	
	@Override
	public final boolean isHomeFolder() {
		return false;
	}

	@Override
	public final boolean isRootFolder() {
		return true;
	}
	
	protected ISectionSet getSectionSet() throws ErrorException { 
		return getSections(new SectionQuery(0, 0));
	}
	
	protected boolean acceptSection(ISection section) { 
		return section != null && (section instanceof IFileInfo) 
				&& !(section instanceof SqRecycleRoot);
	}
	
	private void initList(boolean refresh, Filter filter) 
			throws ErrorException { 
		synchronized (mFileLock) {
			if (mFolders == null || mFiles == null || refresh || filter != null) {
				ISectionSet set = getSectionSet();
				
				ArrayList<IFolderInfo> folders = new ArrayList<IFolderInfo>();
				ArrayList<IFileInfo> files = new ArrayList<IFileInfo>();
				
				for (int i=0; set != null && i < set.getSectionCount(); i++) { 
					ISection section = set.getSectionAt(i);
					if (section != null && acceptSection(section)) { 
						IFileInfo info = (IFileInfo)section;
						if (info != null && (filter == null || filter.accept(info))) { 
							if (info instanceof IFolderInfo)
								folders.add((IFolderInfo)info);
							else
								files.add(info);
						}
					}
				}
				
				mFolders = folders.toArray(new IFolderInfo[folders.size()]);
				mFiles = files.toArray(new IFileInfo[files.size()]);
			}
		}
	}
	
	@Override
	public IFolderInfo[] listFolderInfos(boolean refresh, 
			Filter filter) throws ErrorException {
		synchronized (mFileLock) {
			initList(refresh, filter);
			return mFolders;
		}
	}
	
	@Override
	public IFileInfo[] listFileInfos(boolean refresh, 
			Filter filter) throws ErrorException {
		synchronized (mFileLock) {
			initList(refresh, filter);
			return mFiles;
		}
	}

	@Override
	public String getContentPath() {
		return "/";
	}

	@Override
	public final IFolderInfo getRootInfo() throws ErrorException {
		return this;
	}

	@Override
	public final IFolderInfo getParentInfo() throws ErrorException {
		return null;
	}

	@Override
	public boolean canRead() { 
		try {
			return mCanRead && !getLock().isLocked(ILockable.Type.WRITE, false);
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("canRead: error: " + e, e);
			
			return false;
		}
	}
	
	void setCanRead(boolean cando) { 
		mCanRead = cando;
	}
	
	@Override
	public boolean canMove() {
		return mCanMove;
	}

	void setCanMove(boolean cando) { 
		mCanMove = cando;
	}
	
	@Override
	public boolean canDelete() {
		return mCanDelete;
	}
	
	void setCanDelete(boolean cando) { 
		mCanDelete = cando;
	}

	@Override
	public boolean canWrite() {
		return mCanWrite;
	}
	
	void setCanWrite(boolean cando) { 
		mCanWrite = cando;
	}
	
	@Override
	public boolean canCopy() {
		return mCanCopy;
	}
	
	void setCanCopy(boolean cando) { 
		mCanCopy = cando;
	}
	
	@Override
	public boolean supportOperation(IData.Operation op) { 
		if (op != null) { 
			switch (op) { 
			case MOVE: return false;
			case COPY: return false;
			case DELETE: return true;
			case UPLOAD: return true;
			case NEWFOLDER: return true;
			default: return false;
			}
		}
		return false;
	}
	
	@Override
	public void onIndexed(long time) { 
		if (LOG.isDebugEnabled()) 
			LOG.debug("onIndexed: time=" + time);
		
		setIndexedTime(time);
		setChanged(true);
	}
	
	@Override
	public synchronized void removeAndClose() throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("removeAndClose: library=" + this);
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			if (isDefault()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Library: " + getName() + " is default and cannot be deleted");
			}
			
			if (getTotalFileLength() > 0) {
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Library: " + getName() + " is not empty and cannot be deleted");
			}
			
			if (!canDelete()) { 
				throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
						"Library: " + getName() + " cannot be deleted");
			}
			
			for (int i=0; i < getSectionCount(); i++) { 
				ISectionRoot item = getSectionAt(i);
				if (item == null) continue;
				
				if (item.getSubCount() > 0 || item.getSubLength() > 0 || 
					item.getTotalFileCount() > 0 || item.getTotalFileLength() > 0) {
					throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
							"Root: " + item.getName() + " is not empty and cannot be deleted");
				}
			}
			
			close();
			
			try {
				FileIO.removeFiles(getFs(), getManager().getLibraryPath(this));
			} catch (IOException e) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
}
