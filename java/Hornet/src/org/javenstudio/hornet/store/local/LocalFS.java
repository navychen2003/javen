package org.javenstudio.hornet.store.local;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.LockFactory;
import org.javenstudio.common.indexdb.store.local.FSDirectory;
import org.javenstudio.common.indexdb.store.local.SimpleFSDirectory;
import org.javenstudio.common.indexdb.util.JvmUtil;
import org.javenstudio.common.util.Logger;

public class LocalFS {
	private static Logger LOG = Logger.getLogger(LocalFS.class);
	
	/** 
	 * Creates an FSDirectory instance, trying to pick the
	 *  best implementation given the current environment.
	 *  The directory returned uses the {@link NativeFSLockFactory}.
	 *
	 *  <p>Currently this returns {@link MMapDirectory} for most Solaris
	 *  and Windows 64-bit JREs, {@link NIOFSDirectory} for other
	 *  non-Windows JREs, and {@link SimpleFSDirectory} for other
	 *  JREs on Windows. It is highly recommended that you consult the
	 *  implementation's documentation for your platform before
	 *  using this method.
	 *
	 * <p><b>NOTE</b>: this method may suddenly change which
	 * implementation is returned from release to release, in
	 * the event that higher performance defaults become
	 * possible; if the precise implementation is important to
	 * your application, please instantiate it directly,
	 * instead. For optimal performance you should consider using
	 * {@link MMapDirectory} on 64 bit JVMs.
	 *
	 * <p>See <a href="#subclasses">above</a> 
	 */
	public static FSDirectory open(File path) throws IOException {
		return open(path, null);
	}

	/** 
	 * Just like {@link #open(File)}, but allows you to
	 *  also specify a custom {@link LockFactory}. 
	 */
	public static FSDirectory open(File path, LockFactory lockFactory) throws IOException {
		final FSDirectory directory;
		
		if (lockFactory == null)
			lockFactory = new NativeFSLockFactory();
		
		if ((JvmUtil.WINDOWS || JvmUtil.SUN_OS || JvmUtil.LINUX)
	          && JvmUtil.JRE_IS_64BIT && MMapDirectory.UNMAP_SUPPORTED) {
			directory = new MMapDirectory(path, lockFactory);
			
		} else if (JvmUtil.WINDOWS) {
			directory = new SimpleFSDirectory(path, lockFactory);
			
		} else {
			directory = new NIOFSDirectory(path, lockFactory);
		}
		
		if (LOG.isDebugEnabled())
			LOG.debug("opened Directory: " + directory);
		
		return directory;
	}
	
}
