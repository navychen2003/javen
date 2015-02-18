package org.javenstudio.hornet.store.local;

import java.io.File;
import java.io.IOException;

import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.LockFactory;
import org.javenstudio.common.indexdb.store.IndexInput;
import org.javenstudio.common.indexdb.store.IndexInputSlicer;
import org.javenstudio.common.indexdb.store.local.FSDescriptor;
import org.javenstudio.common.indexdb.store.local.FSDirectory;

/**
 * An {@link FSDirectory} implementation that uses java.nio's FileChannel's
 * positional read, which allows multiple threads to read from the same file
 * without synchronizing.
 * <p>
 * This class only uses FileChannel when reading; writing is achieved with
 * {@link FSDirectory.FSIndexOutput}.
 * <p>
 * <b>NOTE</b>: NIOFSDirectory is not recommended on Windows because of a bug in
 * how FileChannel.read is implemented in Sun's JRE. Inside of the
 * implementation the position is apparently synchronized. See <a
 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6265734">here</a>
 * for details.
 * </p>
 * <p>
 * <font color="red"><b>NOTE:</b> Accessing this class either directly or
 * indirectly from a thread while it's interrupted can close the
 * underlying file descriptor immediately if at the same time the thread is
 * blocked on IO. The file descriptor will remain closed and subsequent access
 * to {@link NIOFSDirectory} will throw a {@link ClosedChannelException}. If
 * your application uses either {@link Thread#interrupt()} or
 * {@link Future#cancel(boolean)} you should use {@link SimpleFSDirectory} in
 * favor of {@link NIOFSDirectory}.</font>
 * </p>
 */
public class NIOFSDirectory extends FSDirectory {

	/** Create a new NIOFSDirectory for the named location.
	 * 
	 * @param path the path of the directory
	 * @param lockFactory the lock factory to use, or null for the default
	 * ({@link NativeFSLockFactory});
	 * @throws IOException
	 */
	public NIOFSDirectory(File path, LockFactory lockFactory) throws IOException {
		super(path, lockFactory);
	}

	/** Create a new NIOFSDirectory for the named location and {@link NativeFSLockFactory}.
	 *
	 * @param path the path of the directory
	 * @throws IOException
	 */
	public NIOFSDirectory(File path) throws IOException {
		super(path, null);
	}

	/** Creates an IndexInput for the file with the given name. */
	@Override
	protected IndexInput openIndexInput(IIndexContext context, String name) throws IOException {
		ensureOpen();
		return new NIOFSIndexInput(context, new File(getDirectory(), name), getReadChunkSize());
	}
  
	@Override
	public IndexInputSlicer createSlicer(final IIndexContext context, final String name) 
			throws IOException {
		ensureOpen();
		
		final File path = new File(getDirectory(), name);
		final FSDescriptor descriptor = new FSDescriptor(path, "r");
		
		return new IndexInputSlicer() {
				@Override
				public void close() throws IOException {
					descriptor.close();
				}

				@Override
				public IndexInput openSlice(long offset, long length) throws IOException {
					return new NIOFSIndexInput(context, 
							path, descriptor, descriptor.getChannel(), offset, length, 
							context.getInputBufferSize(), getReadChunkSize());
				}

				@Override
				public IndexInput openFullSlice() throws IOException {
					return openSlice(0, descriptor.getLength());
				}
			};
	}

}
