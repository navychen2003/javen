package org.javenstudio.hornet.store.local;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.IInputSlicer;
import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.LockFactory;
import org.javenstudio.common.indexdb.NoSuchDirectoryException;
import org.javenstudio.common.indexdb.index.FlushInfo;
import org.javenstudio.common.indexdb.index.IndexContext;
import org.javenstudio.common.indexdb.index.IndexFileNames;
import org.javenstudio.common.indexdb.index.MergeInfo;
import org.javenstudio.common.indexdb.store.Directory;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.store.IndexOutput;
import org.javenstudio.common.indexdb.store.ram.RAMDirectory;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.util.Logger;

// TODO
//   - let subclass dictate policy...?
//   - rename to MergeCacheingDir?  NRTCachingDir

/**
 * Wraps a {@link RAMDirectory}
 * around any provided delegate directory, to
 * be used during NRT search.
 *
 * <p>This class is likely only useful in a near-real-time
 * context, where indexing rate is lowish but reopen
 * rate is highish, resulting in many tiny files being
 * written.  This directory keeps such segments (as well as
 * the segments produced by merging them, as long as they
 * are small enough), in RAM.</p>
 *
 * <p>This is safe to use: when your app calls {IndexWriter#commit},
 * all cached files will be flushed from the cached and sync'd.</p>
 *
 * <p>Here's a simple example usage:
 *
 * <pre class="prettyprint">
 *   Directory fsDir = FSDirectory.open(new File("/path/to/index"));
 *   NRTCachingDirectory cachedFSDir = new NRTCachingDirectory(fsDir, 5.0, 60.0);
 *   IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_32, analyzer);
 *   IndexWriter writer = new IndexWriter(cachedFSDir, conf);
 * </pre>
 *
 * <p>This will cache all newly flushed segments, all merges
 * whose expected segment size is <= 5 MB, unless the net
 * cached bytes exceeds 60 MB at which point all writes will
 * not be cached (until the net bytes falls below 60 MB).</p>
 *
 */
public class NRTCachingDirectory extends Directory {
	private static final Logger LOG = Logger.getLogger(NRTCachingDirectory.class);
	
	private final RAMDirectory mCache = new RAMDirectory();

	private final IIndexContext mContext;
	private final IDirectory mDelegate;

	private final long mMaxMergeSizeBytes;
	private final long mMaxCachedBytes;

	/**
	 *  We will cache a newly created output if 1) it's a
	 *  flush or a merge and the estimated size of the merged segment is <=
	 *  maxMergeSizeMB, and 2) the total cached bytes is <=
	 *  maxCachedMB 
	 */
	public NRTCachingDirectory(IIndexContext context, IDirectory delegate, 
			double maxMergeSizeMB, double maxCachedMB) {
		mContext = context;
		mDelegate = delegate;
		mMaxMergeSizeBytes = (long) (maxMergeSizeMB*1024*1024);
		mMaxCachedBytes = (long) (maxCachedMB*1024*1024);
	}

	public IIndexContext getContext() { 
		return mContext;
	}
  
	public IDirectory getDelegate() {
		return mDelegate;
	}

	@Override
	public LockFactory getLockFactory() {
		return mDelegate.getLockFactory();
	}

	@Override
	public void setLockFactory(LockFactory lf) throws IOException {
		mDelegate.setLockFactory(lf);
	}

	@Override
	public String getLockID() {
		return mDelegate.getLockID();
	}

	@Override
	public Lock makeLock(String name) {
		return mDelegate.makeLock(name);
	}

	@Override
	public void clearLock(String name) throws IOException {
		mDelegate.clearLock(name);
	}

	@Override
	public synchronized String[] listAll() throws IOException {
		final Set<String> files = new HashSet<String>();
		for (String f : mCache.listAll()) {
			files.add(f);
		}
		
		// LUCENE-1468: our NRTCachingDirectory will actually exist (RAMDir!),
		// but if the underlying delegate is an FSDir and mkdirs() has not
		// yet been called, because so far everything is a cached write,
		// in this case, we don't want to throw a NoSuchDirectoryException
		try {
			for (String f : mDelegate.listAll()) {
				// Cannot do this -- if lucene calls createOutput but
				// file already exists then this falsely trips:
				//assert !files.contains(f): "file \"" + f + "\" is in both dirs";
				files.add(f);
			}
		} catch (NoSuchDirectoryException ex) {
			// however, if there are no cached files, then the directory truly
			// does not "exist"
			if (files.isEmpty()) 
				throw ex;
		}
		
		return files.toArray(new String[files.size()]);
	}

	/** 
	 * Returns how many bytes are being used by the
	 *  RAMDirectory cache 
	 */
	public long getSizeInBytes()  {
		return mCache.sizeInBytes();
	}

	@Override
	public synchronized boolean fileExists(String name) throws IOException {
		return mCache.fileExists(name) || mDelegate.fileExists(name);
	}

	@Override
	public synchronized void deleteFile(String name) throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("deleteFile: name=" + name);
		
		if (mCache.fileExists(name)) {
			assert !mDelegate.fileExists(name): "name=" + name;
			mCache.deleteFile(name);
		} else {
			mDelegate.deleteFile(name);
		}
	}

	@Override
	public synchronized long getFileLength(String name) throws IOException {
		if (mCache.fileExists(name)) {
			return mCache.getFileLength(name);
		} else {
			return mDelegate.getFileLength(name);
		}
	}

	public String[] listCachedFiles() {
		return mCache.listAll();
	}

	@Override
	public IIndexOutput createOutput(IIndexContext context, String name) throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("createOutput: name=" + name);
		
		if (doCacheWrite(context, name)) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("createOutput:   to cache");
			
			try {
				mDelegate.deleteFile(name);
			} catch (IOException ioe) {
				// This is fine: file may not exist
			}
			
			return mCache.createOutput(context, name);
		} else {
			try {
				mCache.deleteFile(name);
			} catch (IOException ioe) {
				// This is fine: file may not exist
			}
			
			return mDelegate.createOutput(context, name);
		}
	}

	@Override
	public void sync(Collection<String> fileNames) throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("sync: files=" + fileNames);
		
		for (String fileName : fileNames) {
			unCache(getContext(), fileName);
		}
		
		mDelegate.sync(fileNames);
	}

	@Override
	public synchronized IIndexInput openInput(IIndexContext context, String name) throws IOException {
		if (LOG.isDebugEnabled()) 
			LOG.debug("openInput: name=" + name);
		
		if (mCache.fileExists(name)) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("openInput:   from cache");
			
			return mCache.openInput(context, name);
			
		} else {
			return mDelegate.openInput(context, name);
		}
	}

	public synchronized IInputSlicer createSlicer(final IIndexContext context, 
			final String name) throws IOException {
		ensureOpen();
		
		if (LOG.isDebugEnabled()) 
			LOG.debug("createSlicer: name=" + name);
		
		if (mCache.fileExists(name)) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("createSlicer:   from cache");
			
			return mCache.createSlicer(context, name);
			
		} else {
			return mDelegate.createSlicer(context, name);
		}
	}
  
	/** 
	 * Close this directory, which flushes any cached files
	 *  to the delegate and then closes the delegate. 
	 */
	@Override
	public void close() throws IOException {
		// NOTE: technically we shouldn't have to do this, ie,
		// IndexWriter should have sync'd all files, but we do
		// it for defensive reasons... or in case the app is
		// doing something custom (creating outputs directly w/o
		// using IndexWriter):
		for (String fileName : mCache.listAll()) {
			unCache(getContext(), fileName);
		}
		
		mCache.close();
		mDelegate.close();
	}

	/** 
	 * Subclass can override this to customize logic; return
	 *  true if this file should be written to the RAMDirectory. 
	 */
	protected boolean doCacheWrite(IIndexContext context, String name) {
		long bytes = 0;
		
		if (context instanceof IndexContext) { 
			IndexContext ctx = (IndexContext)context;
			MergeInfo mergeInfo = ctx.getMergeInfo();
			FlushInfo flushInfo = ctx.getFlushInfo();
			
			if (mergeInfo != null) { 
				bytes = mergeInfo.getEstimatedMergeBytes();
				
				if (LOG.isDebugEnabled())
					LOG.debug("doCacheWrite: mergeInfo=" + mergeInfo);
				
			} else if (flushInfo != null) { 
				bytes = flushInfo.getEstimatedSegmentSize();
				
				if (LOG.isDebugEnabled())
					LOG.debug("doCacheWrite: flushInfo=" + flushInfo);
			}
		}

		return !name.equals(IndexFileNames.SEGMENTS_GEN) && 
				(bytes <= mMaxMergeSizeBytes) && (bytes + mCache.sizeInBytes()) <= mMaxCachedBytes;
	}

	private final Object mUnCacheLock = new Object();

	private void unCache(IIndexContext context, String fileName) throws IOException {
		// Only let one thread uncache at a time; this only
		// happens during commit() or close():
		synchronized (mUnCacheLock) {
			if (LOG.isDebugEnabled()) 
				LOG.debug("unCache: name=" + fileName);
			
			if (!mCache.fileExists(fileName)) {
				// Another thread beat us...
				return;
			}
			
			if (mDelegate.fileExists(fileName)) {
				throw new IOException("cannot uncache file=\"" + fileName 
						+ "\": it was separately also created in the delegate directory");
			}
			
			//final IContext context = IOContext.DEFAULT;
			final IIndexOutput out = mDelegate.createOutput(context, fileName);
			
			IIndexInput in = null;
			try {
				in = mCache.openInput(context, fileName);
				out.copyBytes(in, in.length());
			} finally {
				IOUtils.close(in, out);
			}

			// Lock order: uncacheLock -> this
			synchronized(this) {
				// Must sync here because other sync methods have
				// if (cache.fileExists(name)) { ... } else { ... }:
				mCache.deleteFile(fileName);
			}
		}
	}

	@Override
	protected IndexOutput createIndexOutput(IIndexContext context, String name)
			throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected IndexInput openIndexInput(IIndexContext context, String name)
			throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public String toString() {
		return getIdentityName() + "{" + mDelegate + ", maxCacheMB=" + (mMaxCachedBytes/1024/1024.) 
				+ ", maxMergeSizeMB=" + (mMaxMergeSizeBytes/1024/1024.) + "}";
	}
	
}
