package org.javenstudio.common.indexdb.store;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IIndexOutput;
import org.javenstudio.common.indexdb.MergeAbortedException;
import org.javenstudio.common.indexdb.util.IOUtils;

/**
 * Combines multiple files into a single compound file.
 * 
 * @see CompoundFileDirectory
 */
final class CompoundFileWriter implements Closeable{

	private final class FileEntry {
		/** source file */
		public String mFile;
		public long mLength;
		/** temporary holder for the start of this file's data section */
		public long mOffset;
	}

	// Before versioning started.
	static final int FORMAT_PRE_VERSION = 0;

	// Segment name is not written in the file names.
	static final int FORMAT_NO_SEGMENT_PREFIX = -1;
  
	// versioning for the .cfs file
	static final String DATA_CODEC = "CompoundFileWriterData";
	static final int VERSION_START = 0;
	static final int VERSION_CURRENT = VERSION_START;

	// versioning for the .cfe file
	static final String ENTRY_CODEC = "CompoundFileWriterEntries";

	private final IIndexContext mContext;
	private final IDirectory mDirectory;
	private final Map<String, FileEntry> mEntries = new HashMap<String, FileEntry>();
	private final Set<String> mSeenIDs = new HashSet<String>();
	// all entries that are written to a sep. file but not yet moved into CFS
	private final Queue<FileEntry> mPendingEntries = new LinkedList<FileEntry>();
	private boolean mClosed = false;
	private IIndexOutput mDataOut;
	private final AtomicBoolean mOutputTaken = new AtomicBoolean(false);
	private final String mEntryTableName;
	private final String mDataFileName;

	/**
	 * Create the compound stream in the specified file. The file name is the
	 * entire name (no extensions are added).
	 * 
	 * @throws NullPointerException
	 *           if <code>dir</code> or <code>name</code> is null
	 */
	CompoundFileWriter(IIndexContext context, IDirectory dir, String name) throws IOException {
		if (dir == null)
			throw new NullPointerException("directory cannot be null");
		if (name == null)
			throw new NullPointerException("name cannot be null");
		
		mContext = context;
		mDirectory = dir;
		mEntryTableName = context.getCompoundEntriesFileName(name);
		mDataFileName = name;
	}
  
	private synchronized IIndexOutput getOutput() throws IOException {
		if (mDataOut == null) {
			boolean success = false;
			try {
				mDataOut = mDirectory.createOutput(mContext, mDataFileName);
				mContext.writeCodecHeader(mDataOut, DATA_CODEC, VERSION_CURRENT);
				success = true;
			} finally {
				if (!success) 
					IOUtils.closeWhileHandlingException(mDataOut);
			}
		} 
		return mDataOut;
	}

	/** Returns the directory of the compound file. */
	final IDirectory getDirectory() {
		return mDirectory;
	}

	/** Returns the name of the compound file. */
	final String getName() {
		return mDataFileName;
	}

	/**
	 * Closes all resources and writes the entry table
	 * 
	 * @throws IllegalStateException
	 *           if close() had been called before or if no file has been added to
	 *           this object
	 */
	public void close() throws IOException {
		if (mClosed) return;
		
		IOException priorException = null;
		IIndexOutput entryTableOut = null;
		try {
			if (!mPendingEntries.isEmpty() || mOutputTaken.get()) 
				throw new IllegalStateException("CFS has pending open files");
			
			mClosed = true;
			// open the compound stream
			getOutput();
			assert mDataOut != null;
			
			long finalLength = mDataOut.getFilePointer();
			assert assertFileLength(finalLength, mDataOut);
			
		} catch (IOException e) {
			priorException = e;
		} finally {
			IOUtils.closeWhileHandlingException(priorException, mDataOut);
		}
		
		try {
			entryTableOut = mDirectory.createOutput(mContext, mEntryTableName);
			writeEntryTable(mEntries.values(), entryTableOut);
			
		} catch (IOException e) {
			priorException = e;
		} finally {
			IOUtils.closeWhileHandlingException(priorException, entryTableOut);
		}
	}

	private static boolean assertFileLength(long expected, IIndexOutput out)
			throws IOException {
		out.flush();
		assert expected == out.length() : "expected: " + expected + " was " + out.length();
		return true;
	}

	private final void ensureOpen() {
		if (mClosed) 
			throw new AlreadyClosedException("CFS Directory is already closed");
	}

	/**
	 * Copy the contents of the file with specified extension into the provided
	 * output stream.
	 */
	private final long copyFileEntry(IIndexOutput dataOut, FileEntry fileEntry)
			throws IOException, MergeAbortedException {
		final IIndexInput is = mDirectory.openInput(mContext, fileEntry.mFile);
		
		boolean success = false;
		try {
			final long startPtr = dataOut.getFilePointer();
			final long length = fileEntry.mLength;
			
			dataOut.copyBytes(is, length);
			
			// Verify that the output length diff is equal to original file
			long endPtr = dataOut.getFilePointer();
			long diff = endPtr - startPtr;
			if (diff != length) {
				throw new IOException("Difference in the output file offsets " + diff
						+ " does not match the original file length " + length);
			}
			
			fileEntry.mOffset = startPtr;
			success = true;
			
			return length;
		} finally {
			if (success) {
				IOUtils.close(is);
				// copy successful - delete file
				mDirectory.deleteFile(fileEntry.mFile);
			} else {
				IOUtils.closeWhileHandlingException(is);
			}
		}
	}

	protected void writeEntryTable(Collection<FileEntry> entries, IIndexOutput entryOut) 
			throws IOException {
		mContext.writeCodecHeader(entryOut, ENTRY_CODEC, VERSION_CURRENT);
		entryOut.writeVInt(entries.size());
		for (FileEntry fe : entries) {
			entryOut.writeString(mContext.stripSegmentName(fe.mFile));
			entryOut.writeLong(fe.mOffset);
			entryOut.writeLong(fe.mLength);
		}
	}

	IndexOutput createOutput(IIndexContext context, String name) throws IOException {
		ensureOpen();
		
		boolean success = false;
		boolean outputLocked = false;
		try {
			assert name != null : "name must not be null";
			if (mEntries.containsKey(name)) 
				throw new IllegalArgumentException("File " + name + " already exists");
			
			final FileEntry entry = new FileEntry();
			entry.mFile = name;
			mEntries.put(name, entry);
			
			final String id = mContext.stripSegmentName(name);
			assert !mSeenIDs.contains(id): "file=\"" + name + "\" maps to id=\"" + id + "\", which was already written";
			mSeenIDs.add(id);
			
			final DirectCFSIndexOutput out;
			if ((outputLocked = mOutputTaken.compareAndSet(false, true))) {
				out = new DirectCFSIndexOutput(getOutput(), entry, false);
				
			} else {
				if (mDirectory.fileExists(name)) 
					throw new IllegalArgumentException("File " + name + " already exists");
				
				out = new DirectCFSIndexOutput(
						mDirectory.createOutput(context, name), entry, true);
			}
			
			success = true;
			return out;
		} finally {
			if (!success) {
				mEntries.remove(name);
				if (outputLocked) { // release the output lock if not successful
					assert mOutputTaken.get();
					releaseOutputLock();
				}
			}
		}
	}

	final void releaseOutputLock() {
		mOutputTaken.compareAndSet(true, false);
	}

	private final void prunePendingEntries() throws IOException {
		// claim the output and copy all pending files in
		if (mOutputTaken.compareAndSet(false, true)) {
			try {
				while (!mPendingEntries.isEmpty()) {
					FileEntry entry = mPendingEntries.poll();
					copyFileEntry(getOutput(), entry);
					mEntries.put(entry.mFile, entry);
				}
			} finally {
				final boolean compareAndSet = mOutputTaken.compareAndSet(true, false);
				assert compareAndSet;
			}
		}
	}

	final long fileLength(String name) throws IOException {
		FileEntry fileEntry = mEntries.get(name);
		if (fileEntry == null) 
			throw new FileNotFoundException(name + " does not exist");
		
		return fileEntry.mLength;
	}

	final boolean fileExists(String name) {
		return mEntries.containsKey(name);
	}

	final String[] listAll() {
		return mEntries.keySet().toArray(new String[0]);
	}

	private final class DirectCFSIndexOutput extends IndexOutput {
		private final IIndexOutput mDelegate;
		private final long mOffset;
		private boolean mClosed;
		private FileEntry mEntry;
		private long mWrittenBytes;
		private final boolean mIsSeparate;

		DirectCFSIndexOutput(IIndexOutput delegate, FileEntry entry, boolean isSeparate) {
			super(delegate.getContext());
			mDelegate = delegate;
			mEntry = entry;
			mIsSeparate = isSeparate;
			entry.mOffset = mOffset = delegate.getFilePointer();
		}

		@Override
		public void flush() throws IOException {
			mDelegate.flush();
		}

		@Override
		public void close() throws IOException {
			if (!mClosed) {
				mClosed = true;
				mEntry.mLength = mWrittenBytes;
				if (mIsSeparate) {
					mDelegate.close();
					// we are a separate file - push into the pending entries
					mPendingEntries.add(mEntry);
				} else {
					// we have been written into the CFS directly - release the lock
					releaseOutputLock();
				}
				// now prune all pending entries and push them into the CFS
				prunePendingEntries();
			}
		}

		@Override
		public long getFilePointer() {
			return mDelegate.getFilePointer() - mOffset;
		}

		@Override
		public void seek(long pos) throws IOException {
			assert !mClosed;
			mDelegate.seek(mOffset + pos);
		}

		@Override
		public long length() throws IOException {
			assert !mClosed;
			return mDelegate.length() - mOffset;
		}

		@Override
		public void writeByte(byte b) throws IOException {
			assert !mClosed;
			mWrittenBytes ++;
			mDelegate.writeByte(b);
		}

		@Override
		public void writeBytes(byte[] b, int offset, int length) throws IOException {
			assert !mClosed;
			mWrittenBytes += length;
			mDelegate.writeBytes(b, offset, length);
		}
	}

}
