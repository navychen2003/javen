package org.javenstudio.falcon.datum.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.datum.IFolderInfo;
import org.javenstudio.falcon.datum.ISection;
import org.javenstudio.falcon.datum.ISectionRoot;
import org.javenstudio.falcon.datum.SectionHelper;
import org.javenstudio.falcon.util.ILockable;
import org.javenstudio.raptor.io.Text;

public abstract class SqRoot extends SqSectionDir implements ISectionRoot {
	private static final Logger LOG = Logger.getLogger(SqRoot.class);

	private final Map<String,WeakReference<NameData>> mNameRefs;
	private final Map<String,WeakReference<FileData>> mFileRefs;
	
	private final String mName;
	private final String mKey;
	private final String mPathKey;
	
	private FileLoader mLoader = null;
	private NameData mNameData = null;
	
	private long mCreatedTime = 0;
	private long mModifiedTime = 0;
	private long mIndexedTime = 0;
	
	private int mDirCount = 0;
	private int mFileCount = 0;
	private long mFileLength = 0;
	private long mOptimizeTime = 0;
	private int mCacheVersion = 0;
	
	private final ILockable.Lock mLock =
		new ILockable.Lock() {
			@Override
			public ILockable.Lock getParentLock() {
				return SqRoot.this.getLibrary().getLock();
			}
			@Override
			public String getName() {
				return "Root(" + SqRoot.this.getName() + ")";
			}
		};
	
	protected SqRoot(SqLibrary library, 
			String name, String key, String pathKey) {
		super(library);
		SectionHelper.checkKey(key);
		SectionHelper.checkKey(pathKey);
		mNameRefs = new HashMap<String,WeakReference<NameData>>();
		mFileRefs = new HashMap<String,WeakReference<FileData>>();
		mCreatedTime = System.currentTimeMillis();
		mModifiedTime = mCreatedTime;
		mName = name;
		mKey = key;
		mPathKey = pathKey;
	}
	
	public final ILockable.Lock getLock() { 
		return mLock;
	}
	
	private synchronized FileLoader getLoader() { 
		if (mLoader == null) mLoader = newLoader();
		return mLoader;
	}
	
	public FileLoader newLoader() { 
		return new FileLoader() { 
				public SqRoot getRoot() { return SqRoot.this; }
			};
	}
	
	@Override
	public final SqRoot getRoot() { 
		return this; 
	}
	
	@Override
	protected SqSection[] loadSections(boolean byfolder) 
			throws ErrorException { 
		return byfolder ? loadSectionsInternal(this) : 
			loadSectionsInternal();
	}
	
	@Override
	public final String getParentId() { 
		return null; 
	}
	
	@Override
	public final String getParentKey() { 
		return null; 
	}

	@Override
	public String getName() {
		return mName; 
	}
	
	public boolean setName(String name) throws ErrorException { 
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"Root: " + getName() + " cannot change name");
		//return false;
	}
	
	@Override
	public final String getPathKey() { 
		return mPathKey;
	}
	
	@Override
	public final String getContentKey() { 
		return mKey;
	}
	
	@Override
	public String getContentType() { 
		return "application/x-root";
	}
	
	@Override
	public final String getParentPath() {
		return "/"; 
	}
	
	@Override
	public final String getContentPath() {
		return getParentPath() + getName(); 
	}
	
	private int getCacheVersion() { 
		return mCacheVersion;
	}
	
	@Override
	public synchronized NameData getNameData() { 
		try {
			if (mNameData == null)
				mNameData = getLoader().loadNameData(getPathKey());
			return mNameData;
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("getNameData: key=" + getPathKey() + " error: " + e, e);
			return null;
		}
	}
	
	@Override
	public synchronized void reset() throws ErrorException { 
		if (LOG.isDebugEnabled())
			LOG.debug("reset: root=" + getContentKey());
		
		getLock().lock(ILockable.Type.WRITE, null);
		try {
			mCacheVersion ++; //= sVersion.incrementAndGet();
			
			synchronized (mNameRefs) {
				mNameRefs.clear();
			}
			synchronized (mFileRefs) {
				mFileRefs.clear();
			}
			
			if (mLoader != null) mLoader.close();
			mLoader = null;
			mNameData = null;
			
			super.reset();
		} finally {
			getLock().unlock(ILockable.Type.WRITE);
		}
	}
	
	@Override
	public synchronized void close() { 
		if (LOG.isDebugEnabled()) LOG.debug("close");
		
		try {
			getLock().lock(ILockable.Type.WRITE, null);
			try {
				reset(); //getLoader().close();
				super.close();
			} finally {
				getLock().unlock(ILockable.Type.WRITE);
			}
		} catch (Throwable e) { 
			if (LOG.isWarnEnabled())
				LOG.warn("close: error: " + e, e);
		}
	}
	
	@Override
	public void listSection(final Collector collector) 
			throws ErrorException { 
		if (collector == null) return;
		
		final SqRootNames names = new SqRootNames();
		names.reloadNames(this);
		
		names.getNameDatas(new SqRootNames.Collector() {
				@Override
				public void add(Text key, NameData data) throws ErrorException {
					if (key == null || data == null) return;
					SqSection section = getSection(key.toString());
					if (section != null)
						collector.addSection(section);
				}
			});
	}
	
	@Override
	public SqSection getSection(String key) throws ErrorException { 
		if (key == null) return null;
		
		//String rootKey = null;
		String fileKey = key;
		
		if (key.length() != 8) {
			String[] keys = SectionHelper.splitKeys(key);
			if (keys != null) {
				if (keys.length == 1) { 
					//rootKey = keys[0];
				} else if (keys.length == 4) { 
					//rootKey = keys[2];
					fileKey = keys[3];
				}
			}
		}
		
		getLock().lock(ILockable.Type.READ, ILockable.Check.CURRENT);
		try {
			ISection cacheData = SectionHelper.getCache(fileKey, getCacheVersion());
			if (cacheData != null && cacheData instanceof SqSection) {
				SqSection data = (SqSection)cacheData;
				if (data.getRoot() == this) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("getSection: return cached: key=" + fileKey 
								+ " section=" + data);
					}
					
					return data;
				}
			}
			
			SqSection data = loadSectionInternal(fileKey);
			if (data != null)
				SectionHelper.putCache(fileKey, data, getCacheVersion());
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("getSection: return loaded: key=" + fileKey 
						+ " section=" + data);
			}
			
			return data;
		} finally {
			getLock().unlock(ILockable.Type.READ);
		}
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

	@Override
	public void setIndexedTime(long time) {
		mIndexedTime = time;
	}
	
	@Override
	public int getSubCount() { 
		//NameData data = getNameData();
		//return data != null ? data.getFileCount() : 0;
		return getTotalFolderCount() + getTotalFileCount();
	}
	
	@Override
	public long getSubLength() { 
		return getTotalFileLength();
	}
	
	@Override
	public int getTotalFolderCount() {
		return mDirCount;
	}

	@Override
	public int getTotalFileCount() {
		return mFileCount;
	}

	@Override
	public long getTotalFileLength() {
		return mFileLength;
	}
	
	@Override
	public long getContentLength() { 
		return mFileLength; 
	}
	
	@Override
	public long getOptimizedTime() { 
		return mOptimizeTime;
	}
	
	public void setTotalFolderCount(int count) { mDirCount = count; }
	public void setTotalFileCount(int count) { mFileCount = count; }
	public void setTotalFileLength(long length) { mFileLength = length; }
	public void setOptimizeTime(long time) { mOptimizeTime = time; }

	@Override
	public IFolderInfo getParentInfo() throws ErrorException {
		return getLibrary();
	}
	
	final NameData getCacheNameData(String pathKey) {
		synchronized (mNameRefs) {
			WeakReference<NameData> ref = mNameRefs.get(pathKey);
			NameData dataRef = ref != null ? ref.get() : null;
			if (dataRef != null)
				return dataRef;
		}
		return null;
	}
	
	final void putCacheNameData(String pathKey, NameData data) {
		synchronized (mNameRefs) {
			if (pathKey != null && data != null)
				mNameRefs.put(pathKey, new WeakReference<NameData>(data));
			else if (pathKey != null)
				mNameRefs.remove(pathKey);
		}
	}
	
	final FileData getCacheFileData(String pathKey) {
		synchronized (mFileRefs) {
			WeakReference<FileData> ref = mFileRefs.get(pathKey);
			FileData dataRef = ref != null ? ref.get() : null;
			if (dataRef != null)
				return dataRef;
		}
		return null;
	}

	final void putCacheFileData(String pathKey, FileData data) {
		synchronized (mFileRefs) {
			if (pathKey != null && data != null)
				mFileRefs.put(pathKey, new WeakReference<FileData>(data));
			else if (pathKey != null)
				mFileRefs.remove(pathKey);
		}
	}
	
	public abstract FileStorer newStorer();
	
	public abstract SqRootDir newRootDir(NameData name);
	public abstract SqRootFile newRootFile(NameData name);
	
	private final SqSection createSection(NameData data) 
			throws ErrorException { 
		if (data != null) { 
			if (data.isDirectory())
				return newRootDir(data);
			else
				return newRootFile(data);
		}
		return null;
	}
	
	private SqSection[] loadSectionsInternal() throws ErrorException { 
		final ArrayList<SqSection> list = new ArrayList<SqSection>();
		getLock().checkLocked(ILockable.Type.WRITE, ILockable.Check.CURRENT);
		
		try {
			getLoader().loadNameDatas(new NameData.Collector() {
					@Override
					public void addNameData(Text key, NameData data) throws ErrorException {
						if (key == null || data == null) return;
						if (!data.isDirectory()) { 
							SqSection section = createSection(data);
							if (section != null) 
								list.add(section);
						}
					}
				});
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		
		return list.toArray(new SqSection[list.size()]);
	}
	
	final SqSection[] loadSectionsInternal(SqSectionDir dir) 
			throws ErrorException { 
		if (dir == null) return null;
		getLock().checkLocked(ILockable.Type.WRITE, ILockable.Check.CURRENT);
		
		try {
			String nameKey = dir.getPathKey();
			NameData dirData = getLoader().loadNameData(nameKey);
			
			if (dirData != null && dirData.isDirectory()) { 
				Text[] values = dirData.getFileKeys();
				if (values != null && values.length > 0) { 
					ArrayList<SqSection> list = new ArrayList<SqSection>();
					
					for (int i=0; values != null && i < values.length; i++) { 
						Text fileKey = values[i];
						if (fileKey == null) continue;
						
						NameData data = getLoader().loadNameData(fileKey.toString());
						SqSection section = createSection(data);
						if (section != null) 
							list.add(section);
					}
					
					return list.toArray(new SqSection[list.size()]);
				}
			}
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		
		return new SqSection[0];
	}
	
	private final SqSection loadSectionInternal(String fileKey) 
			throws ErrorException { 
		if (fileKey == null) return null;
		try {
			String nameKey = fileKey;
			NameData nameData = getLoader().loadNameData(nameKey);
			return createSection(nameData);
		} catch (IOException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	InputStream openFile(int index, String fileKey) 
			throws ErrorException { 
		return getLoader().openFile(index, fileKey);
	}
	
	FileData loadFileData(int index, String pathKey) 
			throws IOException, ErrorException { 
		return getLoader().loadFileData(index, pathKey);
	}
	
}
