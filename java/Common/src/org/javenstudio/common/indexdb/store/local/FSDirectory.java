package org.javenstudio.common.indexdb.store.local;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import static java.util.Collections.synchronizedSet;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.LockFactory;
import org.javenstudio.common.indexdb.NoSuchDirectoryException;
import org.javenstudio.common.indexdb.ThreadInterruptedException;
import org.javenstudio.common.indexdb.store.Directory;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.store.IndexOutput;
import org.javenstudio.common.indexdb.store.RateLimiter;
import org.javenstudio.common.indexdb.util.JvmUtil;

/**
 * <a name="subclasses"/>
 * Base class for Directory implementations that store index
 * files in the file system.  There are currently three core
 * subclasses:
 *
 * <ul>
 *
 *  <li> {@link SimpleFSDirectory} is a straightforward
 *       implementation using java.io.RandomAccessFile.
 *       However, it has poor concurrent performance
 *       (multiple threads will bottleneck) as it
 *       synchronizes when multiple threads read from the
 *       same file.
 *
 *  <li> {@link NIOFSDirectory} uses java.nio's
 *       FileChannel's positional io when reading to avoid
 *       synchronization when reading from the same file.
 *       Unfortunately, due to a Windows-only <a
 *       href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6265734">Sun
 *       JRE bug</a> this is a poor choice for Windows, but
 *       on all other platforms this is the preferred
 *       choice. Applications using {@link Thread#interrupt()} or
 *       {@link Future#cancel(boolean)} should use
 *       {@link SimpleFSDirectory} instead. See {@link NIOFSDirectory} java doc
 *       for details.
 *        
 *        
 *
 *  <li> {@link MMapDirectory} uses memory-mapped IO when
 *       reading. This is a good choice if you have plenty
 *       of virtual memory relative to your index size, eg
 *       if you are running on a 64 bit JRE, or you are
 *       running on a 32 bit JRE but your index sizes are
 *       small enough to fit into the virtual memory space.
 *       Java has currently the limitation of not being able to
 *       unmap files from user code. The files are unmapped, when GC
 *       releases the byte buffers. Due to
 *       <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038">
 *       this bug</a> in Sun's JRE, MMapDirectory's {@link IndexInput#close}
 *       is unable to close the underlying OS file handle. Only when
 *       GC finally collects the underlying objects, which could be
 *       quite some time later, will the file handle be closed.
 *       This will consume additional transient disk usage: on Windows,
 *       attempts to delete or overwrite the files will result in an
 *       exception; on other platforms, which typically have a &quot;delete on
 *       last close&quot; semantics, while such operations will succeed, the bytes
 *       are still consuming space on disk.  For many applications this
 *       limitation is not a problem (e.g. if you have plenty of disk space,
 *       and you don't rely on overwriting files on Windows) but it's still
 *       an important limitation to be aware of. This class supplies a
 *       (possibly dangerous) workaround mentioned in the bug report,
 *       which may fail on non-Sun JVMs.
 *       
 *       Applications using {@link Thread#interrupt()} or
 *       {@link Future#cancel(boolean)} should use
 *       {@link SimpleFSDirectory} instead. See {@link MMapDirectory}
 *       java doc for details.
 * </ul>
 *
 * Unfortunately, because of system peculiarities, there is
 * no single overall best implementation.  Therefore, we've
 * added the {@link #open} method, to allow Indexdb to choose
 * the best FSDirectory implementation given your
 * environment, and the known limitations of each
 * implementation.  For users who have no reason to prefer a
 * specific implementation, it's best to simply use {@link
 * #open}.  For all others, you should instantiate the
 * desired implementation directly.
 *
 * <p>The locking implementation is by default {@link
 * NativeFSLockFactory}, but can be changed by
 * passing in a custom {@link LockFactory} instance.
 *
 * @see Directory
 */
public abstract class FSDirectory extends Directory {

	/**
	 * Default read chunk size.  This is a conditional default: on 32bit JVMs, 
	 * it defaults to 100 MB.  On 64bit JVMs, it's <code>Integer.MAX_VALUE</code>.
	 *
	 * @see #setReadChunkSize
	 */
	public static final int DEFAULT_READ_CHUNK_SIZE = 
			JvmUtil.JRE_IS_64BIT ? Integer.MAX_VALUE : 100 * 1024 * 1024;

	// The underlying filesystem directory
	protected final File mDirectory; 
	
	// Files written, but not yet sync'ed
	protected final Set<String> mStaleFiles = synchronizedSet(new HashSet<String>()); 
	
	private int mChunkSize = DEFAULT_READ_CHUNK_SIZE; // LUCENE-1566

	// null means no limit
	private volatile RateLimiter mMergeWriteRateLimiter;

	final File getDirectoryFile() { return mDirectory; }

	// returns the canonical version of the directory, creating it if it doesn't exist.
	private static File getCanonicalPath(File file) throws IOException {
		return new File(file.getCanonicalPath());
	}
	
	/** 
	 * Create a new FSDirectory for the named location (ctor for subclasses).
	 * @param path the path of the directory
	 * @param lockFactory the lock factory to use, or null for the default
	 * ({@link NativeFSLockFactory});
	 * @throws IOException
	 */
	protected FSDirectory(File path, LockFactory lockFactory) throws IOException {
		// new ctors use always NativeFSLockFactory as default:
		if (lockFactory == null) 
			lockFactory = new SimpleFSLockFactory(); //new NativeFSLockFactory();
		
		mDirectory = getCanonicalPath(path);

		if (mDirectory.exists() && !mDirectory.isDirectory())
			throw new NoSuchDirectoryException("file '" + mDirectory + "' exists but is not a directory");

		setLockFactory(lockFactory);
	}
	
	@Override
	public void setLockFactory(LockFactory lockFactory) throws IOException {
		super.setLockFactory(lockFactory);

		// for filesystem based LockFactory, delete the lockPrefix, if the locks are placed
		// in index dir. If no index dir is given, set ourselves
		if (lockFactory instanceof FSLockFactory) {
			final FSLockFactory lf = (FSLockFactory) lockFactory;
			final File dir = lf.getLockDir();
			// if the lock factory has no lockDir set, use the this directory as lockDir
			if (dir == null) {
				lf.setLockDir(mDirectory);
				lf.setLockPrefix(null);
			} else if (dir.getCanonicalPath().equals(mDirectory.getCanonicalPath())) {
				lf.setLockPrefix(null);
			}
		}
	}
  
	/** 
	 * Lists all files (not subdirectories) in the
	 *  directory.  This method never returns null (throws
	 *  {@link IOException} instead).
	 *
	 *  @throws NoSuchDirectoryException if the directory
	 *   does not exist, or does exist but is not a
	 *   directory.
	 *  @throws IOException if list() returns null 
	 */
	public static String[] listAll(File dir) throws IOException {
		if (!dir.exists())
			throw new NoSuchDirectoryException("directory '" + dir + "' does not exist");
		else if (!dir.isDirectory())
			throw new NoSuchDirectoryException("file '" + dir + "' exists but is not a directory");

		// Exclude subdirs
		String[] result = dir.list(new FilenameFilter() {
				public boolean accept(File dir, String file) {
					return !new File(dir, file).isDirectory();
				}
			});

		if (result == null)
			throw new IOException("directory '" + dir + "' exists and is a directory, but cannot be listed: list() returned null");

		return result;
	}

	/** 
	 * Lists all files (not subdirectories) in the directory.
	 * @see #listAll(File) 
	 */
	@Override
	public String[] listAll() throws IOException {
		ensureOpen();
		return listAll(mDirectory);
	}

	/** Returns true iff a file with the given name exists. */
	@Override
	public boolean fileExists(String name) {
		ensureOpen();
		File file = new File(mDirectory, name);
		return file.exists();
	}

	/** Returns the time the named file was last modified. */
	public static long fileModified(File directory, String name) {
		File file = new File(directory, name);
		return file.lastModified();
	}

	/** Returns the length in bytes of a file in the directory. */
	@Override
	public long getFileLength(String name) throws IOException {
		ensureOpen();
		
		File file = new File(mDirectory, name);
		final long len = file.length();
		
		if (len == 0 && !file.exists()) 
			throw new FileNotFoundException(name);
		
		return len;
	}

	/** Removes an existing file in the directory. */
	@Override
	public void deleteFile(String name) throws IOException {
		ensureOpen();
		
		File file = new File(mDirectory, name);
		if (!file.delete())
			throw new IOException("Cannot delete " + file);
		
		mStaleFiles.remove(name);
	}

	/** Creates an IndexOutput for the file with the given name. */
	@Override
	protected IndexOutput createIndexOutput(IIndexContext context, String name) throws IOException {
		ensureOpen();
		ensureCanWrite(name);
		
		return new FSIndexOutput(context, this, 
				name, context.isMerge() ? mMergeWriteRateLimiter : null);
	}

	/** 
	 * Sets the maximum (approx) MB/sec allowed by all write
	 *  IO performed by merging.  Pass null to have no limit.
	 *
	 *  <p><b>NOTE</b>: if merges are already running there is
	 *  no guarantee this new rate will apply to them; it will
	 *  only apply for certain to new merges.
	 *
	 */
	public void setMaxMergeWriteMBPerSec(Double mbPerSec) {
		RateLimiter limiter = mMergeWriteRateLimiter;
		if (mbPerSec == null) {
			if (limiter != null) {
				limiter.setMbPerSec(Double.MAX_VALUE);
				mMergeWriteRateLimiter = null;
			}
			
		} else if (limiter != null) {
			limiter.setMbPerSec(mbPerSec);
			
		} else {
			mMergeWriteRateLimiter = new RateLimiter(mbPerSec);
		}
	}

	/**
	 * Sets the rate limiter to be used to limit (approx) MB/sec allowed
	 * by all IO performed when merging. Pass null to have no limit.
	 *
	 * <p>Passing an instance of rate limiter compared to setting it using
	 * {@link #setMaxMergeWriteMBPerSec(Double)} allows to use the same limiter
	 * instance across several directories globally limiting IO when merging
	 * across them.
	 *
	 */
	public void setMaxMergeWriteLimiter(RateLimiter mergeWriteRateLimiter) {
		mMergeWriteRateLimiter = mergeWriteRateLimiter;
	}

	/** 
	 * See {@link #setMaxMergeWriteMBPerSec}.
	 */
	public Double getMaxMergeWriteMBPerSec() {
		RateLimiter limiter = mMergeWriteRateLimiter;
		return limiter == null ? null : limiter.getMbPerSec();
	}

	protected void ensureCanWrite(String name) throws IOException {
		if (!mDirectory.exists()) {
			if (!mDirectory.mkdirs())
				throw new IOException("Cannot create directory: " + mDirectory);
		}

		File file = new File(mDirectory, name);
		if (file.exists() && !file.delete())          // delete existing, if any
			throw new IOException("Cannot overwrite: " + file);
	}

	protected void onIndexOutputClosed(FSIndexOutput io) {
		mStaleFiles.add(io.getName());
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		ensureOpen();
		
		Set<String> toSync = new HashSet<String>(names);
		toSync.retainAll(mStaleFiles);

		for (String name : toSync) {
			fsync(name);
		}

		mStaleFiles.removeAll(toSync);
	}

	@Override
	public String getLockID() {
		ensureOpen();
		
		final String dirName; 	// name to be hashed
		try {
			dirName = mDirectory.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e.toString(), e);
		}

		int digest = 0;
		for (int charIDX=0; charIDX < dirName.length(); charIDX++) {
			final char ch = dirName.charAt(charIDX);
			digest = 31 * digest + ch;
		}
		
		return "indexdb-" + Integer.toHexString(digest);
	}

	/** Closes the store to future operations. */
	@Override
	public synchronized void close() {
		mIsOpen = false;
	}

	/** @return the underlying filesystem directory */
	public File getDirectory() {
		ensureOpen();
		return mDirectory;
	}

	/**
	 * Sets the maximum number of bytes read at once from the
	 * underlying file during {@link IndexInput#readBytes}.
	 * The default value is {@link #DEFAULT_READ_CHUNK_SIZE};
	 *
	 * <p> This was introduced due to <a
	 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6478546">Sun
	 * JVM Bug 6478546</a>, which throws an incorrect
	 * OutOfMemoryError when attempting to read too many bytes
	 * at once.  It only happens on 32bit JVMs with a large
	 * maximum heap size.</p>
	 *
	 * <p>Changes to this value will not impact any
	 * already-opened {@link IndexInput}s.  You should call
	 * this before attempting to open an index on the
	 * directory.</p>
	 *
	 * <p> <b>NOTE</b>: This value should be as large as
	 * possible to reduce any possible performance impact.  If
	 * you still encounter an incorrect OutOfMemoryError,
	 * trying lowering the chunk size.</p>
	 */
	public final void setReadChunkSize(int chunkSize) {
		// LUCENE-1566
		if (chunkSize <= 0) 
			throw new IllegalArgumentException("chunkSize must be positive");
		
		if (!JvmUtil.JRE_IS_64BIT) 
			mChunkSize = chunkSize;
	}

	/**
	 * The maximum number of bytes to read at once from the
	 * underlying file during {@link IndexInput#readBytes}.
	 * @see #setReadChunkSize
	 */
	public final int getReadChunkSize() {
		// LUCENE-1566
		return mChunkSize;
	}

	protected void fsync(String name) throws IOException {
		File fullFile = new File(mDirectory, name);
		
		boolean success = false;
		int retryCount = 0;
		IOException exc = null;
		
		while (!success && retryCount < 5) {
			retryCount ++;
			RandomAccessFile file = null;
			try {
				try {
					file = new RandomAccessFile(fullFile, "rw");
					file.getFD().sync();
					success = true;
				} finally {
					if (file != null)
						file.close();
				}
			} catch (IOException ioe) {
				if (exc == null)
					exc = ioe;
				try {
					// Pause 5 msec
					Thread.sleep(5);
				} catch (InterruptedException ie) {
					throw new ThreadInterruptedException(ie);
				}
			}
		}
		
		if (!success) {
			// Throw original exception
			throw exc;
		}
	}
	
	/** For debug output. */
	@Override
	public String toString() {
		return getIdentityName() + "{directory=" + mDirectory 
				+ ", lockFactory=" + getLockFactory() + "}";
	}
	
}
