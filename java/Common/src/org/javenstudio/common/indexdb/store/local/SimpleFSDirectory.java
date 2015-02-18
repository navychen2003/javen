package org.javenstudio.common.indexdb.store.local;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.LockFactory;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.store.IndexInputSlicer;

/** 
 * A straightforward implementation of {@link FSDirectory}
 *  using java.io.RandomAccessFile.  However, this class has
 *  poor concurrent performance (multiple threads will
 *  bottleneck) as it synchronizes when multiple threads
 *  read from the same file.  It's usually better to use
 *  {@link NIOFSDirectory} or {@link MMapDirectory} instead. 
 */
public class SimpleFSDirectory extends FSDirectory {
    
	/** 
	 * Create a new SimpleFSDirectory for the named location.
	 *
	 * @param path the path of the directory
	 * @param lockFactory the lock factory to use, or null for the default
	 * ({@link NativeFSLockFactory});
	 * @throws IOException
	 */
	public SimpleFSDirectory(File path, LockFactory lockFactory) throws IOException {
		super(path, lockFactory);
	}
  
	/** 
	 * Create a new SimpleFSDirectory for the named location and {@link NativeFSLockFactory}.
	 *
	 * @param path the path of the directory
	 * @throws IOException
	 */
	public SimpleFSDirectory(File path) throws IOException {
		super(path, null);
	}

	/** Creates an IndexInput for the file with the given name. */
	@Override
	protected IndexInput openIndexInput(IIndexContext context, String name) throws IOException {
		ensureOpen();
		
		final File path = new File(mDirectory, name);
		return new SimpleFSIndexInput(context, path, getReadChunkSize());
	}

	@Override
	public IndexInputSlicer createSlicer(final IIndexContext context, final String name) 
			throws IOException {
		ensureOpen();
		
		final File file = new File(getDirectory(), name);
		final FSDescriptor descriptor = new FSDescriptor(file, "r");
		
		return new IndexInputSlicer() {
				@Override
				public void close() throws IOException {
					descriptor.close();
				}

				@Override
				public IndexInput openSlice(long offset, long length) throws IOException {
					return new SimpleFSIndexInput(context, descriptor, offset, length, context.getInputBufferSize(), getReadChunkSize());
				}

				@Override
				public IndexInput openFullSlice() throws IOException {
					return openSlice(0, descriptor.getLength());
				}
			};
	}

}
