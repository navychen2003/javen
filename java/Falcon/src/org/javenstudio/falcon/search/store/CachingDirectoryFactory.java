package org.javenstudio.falcon.search.store;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.store.NoLockFactory;
import org.javenstudio.common.indexdb.store.SingleInstanceLockFactory;
import org.javenstudio.common.indexdb.store.local.SimpleFSLockFactory;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.hornet.store.local.NativeFSLockFactory;

/**
 * A {@link DirectoryFactory} impl base class for caching Directory instances
 * per path. Most DirectoryFactory implementations will want to extend this
 * class and simply implement {@link DirectoryFactory#create(String)}.
 * 
 */
public abstract class CachingDirectoryFactory extends DirectoryFactory {
	private static Logger LOG = Logger.getLogger(CachingDirectoryFactory.class);
	
	protected class CacheValue {
		private IDirectory mDirectory;
		private int mRefCount = 1;
		private String mPath;
		private boolean mDoneWithDir = false;
		
		public final IDirectory getDirectory() { return mDirectory; }
		public final int getRefCount() { return mRefCount; }
		public final String getPath() { return mPath; }
		public final boolean doneWithDir() { return mDoneWithDir; }
		
		@Override
		public String toString() {
			return "CachedDir{" + mDirectory.toString() + "; refCount=" + mRefCount 
					+ "; path=" + mPath + "; done=" + mDoneWithDir + "}";
		}
	}
  
	protected Map<String,CacheValue> mByPathCache = 
			new HashMap<String,CacheValue>();
  
	protected Map<IDirectory,CacheValue> mByDirectoryCache = 
			new HashMap<IDirectory,CacheValue>();
  
	protected Map<IDirectory,List<DirectoryCloseListener>> mCloseListeners = 
			new HashMap<IDirectory,List<DirectoryCloseListener>>();
  
	@Override
	public void addCloseListener(IDirectory dir, DirectoryCloseListener closeListener) {
		synchronized (this) {
			if (!mByDirectoryCache.containsKey(dir)) {
				throw new IllegalArgumentException("Unknown directory: " + dir
						+ " " + mByDirectoryCache);
			}
			
			List<DirectoryCloseListener> listeners = mCloseListeners.get(dir);
			if (listeners == null) {
				listeners = new ArrayList<DirectoryCloseListener>();
				mCloseListeners.put(dir, listeners);
			}
			
			listeners.add(closeListener);
			mCloseListeners.put(dir, listeners);
		}
	}
  
	@Override
	public void doneWithDirectory(IDirectory directory) throws IOException {
		synchronized (this) {
			CacheValue cacheValue = mByDirectoryCache.get(directory);
			if (cacheValue == null) {
				throw new IllegalArgumentException("Unknown directory: " + directory
						+ " " + mByDirectoryCache);
			}
			
			cacheValue.mDoneWithDir = true;
			
			if (LOG.isDebugEnabled())
				LOG.debug("doneWithDirectory: " + cacheValue);
			
			if (cacheValue.mRefCount == 0) {
				cacheValue.mRefCount ++; // this will go back to 0 in close
				close(directory);
			}
		}
	}
  
	/**
	 * (non-Javadoc)
	 * 
	 * @see DirectoryFactory#close()
	 */
	@Override
	public void close() throws IOException {
		synchronized (this) {
			if (LOG.isDebugEnabled())
				LOG.debug("Closing ...");
			
			for (CacheValue val : mByDirectoryCache.values()) {
				try {
					if (LOG.isDebugEnabled())
						LOG.debug("Closing " + val);
					
					val.mDirectory.close();
				} catch (Throwable t) {
					if (LOG.isErrorEnabled())
						LOG.error("Error closing directory", t);
				}
			}
			
			mByDirectoryCache.clear();
			mByPathCache.clear();
		}
	}
  
	private void close(IDirectory directory) throws IOException {
		synchronized (this) {
			CacheValue cacheValue = mByDirectoryCache.get(directory);
			if (cacheValue == null) {
				throw new IllegalArgumentException("Unknown directory: " + directory
						+ " " + mByDirectoryCache);
			}

			if (LOG.isDebugEnabled())
				LOG.debug("Closing: " + cacheValue);

			cacheValue.mRefCount --;
			
			if (cacheValue.mRefCount == 0 && cacheValue.mDoneWithDir) {
				if (LOG.isInfoEnabled())
					LOG.info("Closing directory:" + cacheValue.mPath);
				
				directory.close();
				
				mByDirectoryCache.remove(directory);
				mByPathCache.remove(cacheValue.mPath);
				
				List<DirectoryCloseListener> listeners = mCloseListeners.remove(directory);
				if (listeners != null) {
					for (DirectoryCloseListener listener : listeners) {
						listener.onClose();
					}
					mCloseListeners.remove(directory);
				}
			}
		}
	}
  
	protected abstract IDirectory create(String path) throws IOException;
  
	@Override
	public boolean exists(String path) {
		// back compat behavior
		File dirFile = new File(path);
		return dirFile.canRead() && dirFile.list().length > 0;
	}
  
	/**
	 * (non-Javadoc)
	 * 
	 * @see DirectoryFactory#get(java.lang.String,java.lang.String)
	 */
	@Override
	public final IDirectory get(String path, String rawLockType)
			throws IOException {
		return get(path, rawLockType, false);
	}
  
	/**
	 * (non-Javadoc)
	 * 
	 * @see DirectoryFactory#get(java.lang.String,java.lang.String, boolean)
	 */
	@Override
	public final IDirectory get(String path, String rawLockType, boolean forceNew)
			throws IOException {
		String fullPath = new File(path).getAbsolutePath();
		
		synchronized (this) {
			CacheValue cacheValue = mByPathCache.get(fullPath);
			IDirectory directory = null;
			
			if (cacheValue != null) {
				directory = cacheValue.mDirectory;
				if (forceNew) {
					cacheValue.mDoneWithDir = true;
					
					if (LOG.isDebugEnabled()) {
						LOG.debug("doneWithDirectory: " + cacheValue 
								+ " for forceNew=" + forceNew);
					}
					
					if (cacheValue.mRefCount == 0) 
						close(cacheValue.mDirectory);
				}
			}
      
			if (directory == null || forceNew) {
				directory = create(fullPath);
        
				CacheValue newCacheValue = new CacheValue();
				newCacheValue.mDirectory = directory;
				newCacheValue.mPath = fullPath;
        
				injectLockFactory(directory, path, rawLockType);
        
				mByDirectoryCache.put(directory, newCacheValue);
				mByPathCache.put(fullPath, newCacheValue);
				
				if (LOG.isInfoEnabled()) {
					LOG.info("return new directory for " + fullPath 
							+ " forceNew:" + forceNew);
				}
			} else {
				cacheValue.mRefCount ++;
				
				if (LOG.isDebugEnabled())
					LOG.debug("return cached directory: " + cacheValue);
			}
      
			return directory;
		}
	}
  
	/**
	 * (non-Javadoc)
	 * 
	 * @see DirectoryFactory#increaseRef(Directory)
	 */
	@Override
	public void increaseRef(IDirectory directory) {
		synchronized (this) {
			CacheValue cacheValue = mByDirectoryCache.get(directory);
			if (cacheValue == null) 
				throw new IllegalArgumentException("Unknown directory: " + directory);
      
			cacheValue.mRefCount ++;
			
			if (LOG.isDebugEnabled())
				LOG.debug("increaseRef: " + cacheValue);
		}
	}
  
	@Override
	public void init(NamedList<?> args) throws ErrorException { 
		// do nothing
	}
  
	/**
	 * (non-Javadoc)
	 * 
	 * @see DirectoryFactory#release(Directory)
	 */
	@Override
	public void release(IDirectory directory) throws IOException {
		if (directory == null) 
			throw new NullPointerException();
		
		if (LOG.isDebugEnabled())
			LOG.debug("release directory: " + directory);
		
		close(directory);
	}
  
	private static IDirectory injectLockFactory(IDirectory dir, String lockPath,
			String rawLockType) throws IOException {
		if (rawLockType == null) {
			if (LOG.isInfoEnabled())
				LOG.info("No lockType configured for " + dir + " assuming 'simple'");
			
			// we default to "simple" for backwards compatibility
			rawLockType = "simple";
		}
		
		final String lockType = rawLockType.toLowerCase(Locale.ROOT).trim();
    
		if ("simple".equals(lockType)) {
			// multiple SimpleFSLockFactory instances should be OK
			dir.setLockFactory(new SimpleFSLockFactory(lockPath));
			
		} else if ("native".equals(lockType)) {
			dir.setLockFactory(new NativeFSLockFactory(lockPath));
			
		} else if ("single".equals(lockType)) {
			if (!(dir.getLockFactory() instanceof SingleInstanceLockFactory)) 
				dir.setLockFactory(new SingleInstanceLockFactory());
			
		} else if ("none".equals(lockType)) {
			// Recipe for disaster
			if (LOG.isErrorEnabled())
				LOG.error("CONFIGURATION WARNING: locks are disabled on " + dir);
			
			dir.setLockFactory(NoLockFactory.getNoLockFactory());
			
		} else {
			throw new IOException("Unrecognized lockType: " + rawLockType);
		}
		
		return dir;
	}
	
}
