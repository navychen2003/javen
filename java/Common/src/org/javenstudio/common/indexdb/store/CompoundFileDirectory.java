package org.javenstudio.common.indexdb.store;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IIndexContext;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.IInputSlicer;
import org.javenstudio.common.indexdb.Lock;
import org.javenstudio.common.indexdb.util.IOUtils;

/**
 * Class for accessing a compound stream.
 * This class implements a directory, but is limited to only read operations.
 * Directory methods that would normally modify data throw an exception.
 * <p>
 * All files belonging to a segment have the same name with varying extensions.
 * The extensions correspond to the different file formats used by the {@link Codec}. 
 * When using the Compound File format these files are collapsed into a 
 * single <tt>.cfs</tt> file (except for the {@link LiveDocsFormat}, with a 
 * corresponding <tt>.cfe</tt> file indexing its sub-files.
 * <p>
 * Files:
 * <ul>
 *    <li><tt>.cfs</tt>: An optional "virtual" file consisting of all the other 
 *    index files for systems that frequently run out of file handles.
 *    <li><tt>.cfe</tt>: The "virtual" compound file's entry table holding all 
 *    entries in the corresponding .cfs file.
 * </ul>
 * <p>Description:</p>
 * <ul>
 *   <li>Compound (.cfs) --&gt; Header, FileData <sup>FileCount</sup></li>
 *   <li>Compound Entry Table (.cfe) --&gt; Header, FileCount, &lt;FileName,
 *       DataOffset, DataLength&gt; <sup>FileCount</sup></li>
 *   <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}</li>
 *   <li>FileCount --&gt; {@link DataOutput#writeVInt VInt}</li>
 *   <li>DataOffset,DataLength --&gt; {@link DataOutput#writeLong UInt64}</li>
 *   <li>FileName --&gt; {@link DataOutput#writeString String}</li>
 *   <li>FileData --&gt; raw file data</li>
 * </ul>
 * <p>Notes:</p>
 * <ul>
 *   <li>FileCount indicates how many files are contained in this compound file. 
 *       The entry table that follows has that many entries. 
 *   <li>Each directory entry contains a long pointer to the start of this file's data
 *       section, the files length, and a String with that file's name.
 * </ul>
 * 
 */
public final class CompoundFileDirectory extends Directory {
  
	private static final byte CODEC_MAGIC_BYTE1 = Constants.CODEC_MAGIC_BYTE1; //(byte) (CodecUtil.CODEC_MAGIC >>> 24);
	private static final byte CODEC_MAGIC_BYTE2 = Constants.CODEC_MAGIC_BYTE2; //(byte) (CodecUtil.CODEC_MAGIC >>> 16);
	private static final byte CODEC_MAGIC_BYTE3 = Constants.CODEC_MAGIC_BYTE3; //(byte) (CodecUtil.CODEC_MAGIC >>> 8);
	private static final byte CODEC_MAGIC_BYTE4 = Constants.CODEC_MAGIC_BYTE4; //(byte) CodecUtil.CODEC_MAGIC;
	
	private static final Map<String,FileEntry> SENTINEL = Collections.emptyMap();
	
	/** Offset/Length for a slice inside of a compound file */
	private final class FileEntry {
		public long mOffset;
		public long mLength;
	}
  
	private final IIndexContext mContext;
	private final IDirectory mDirectory;
	private final String mFileName;
	private final int mReadBufferSize;  
	private final Map<String,FileEntry> mEntries;
	private final boolean mOpenForWrite;
	private final CompoundFileWriter mWriter;
	private final IInputSlicer mHandle;
  
	final IIndexContext getContext() { return mContext; }
	final int getReadBufferSize() { return mReadBufferSize; }
	
	/**
	 * Create a new CompoundFileDirectory.
	 */
	public CompoundFileDirectory(IIndexContext context, IDirectory directory, String fileName, 
			boolean openForWrite) throws IOException {
		mContext = context;
		mDirectory = directory;
		mFileName = fileName;
		mReadBufferSize = context.getInputBufferSize(); 
		mIsOpen = false;
		mOpenForWrite = openForWrite;
		
		if (!openForWrite) {
			boolean success = false;
			mHandle = directory.createSlicer(context, fileName);
			try {
				mEntries = readEntries(mHandle, context, directory, fileName);
				success = true;
			} finally {
				if (!success) 
					IOUtils.closeWhileHandlingException(mHandle);
			}
			mIsOpen = true;
			mWriter = null;
			
		} else {
			assert !(directory instanceof CompoundFileDirectory) : 
				"compound file inside of compound file: " + fileName;
			
			mEntries = SENTINEL;
			mIsOpen = true;
			mWriter = new CompoundFileWriter(context, directory, fileName);
			mHandle = null;
		}
	}

	/** Helper method that reads CFS entries from an input stream */
	final Map<String, FileEntry> readEntries(IInputSlicer handle, 
			IIndexContext context, IDirectory dir, String name) throws IOException {
		// read the first VInt. If it is negative, it's the version number
		// otherwise it's the count (pre-3.1 indexes)
		final IIndexInput stream = handle.openFullSlice();
		final Map<String, FileEntry> mapping;
		
		boolean success = false;
		try {
			final int firstInt = stream.readVInt();
			// impossible for 3.0 to have 63 files in a .cfs, CFS writer was not visible
			// and separate norms/etc are outside of cfs.
			if (firstInt == CODEC_MAGIC_BYTE1) {
				byte secondByte = stream.readByte();
				byte thirdByte = stream.readByte();
				byte fourthByte = stream.readByte();
				if (secondByte != CODEC_MAGIC_BYTE2 || thirdByte != CODEC_MAGIC_BYTE3 || fourthByte != CODEC_MAGIC_BYTE4) {
					throw new CorruptIndexException("Illegal/impossible header for CFS file: " 
							+ secondByte + "," + thirdByte + "," + fourthByte);
				}
				
				mContext.checkCodecHeaderNoMagic(stream, 
						CompoundFileWriter.DATA_CODEC, CompoundFileWriter.VERSION_START, 
						CompoundFileWriter.VERSION_START);
				
				IIndexInput input = null;
				try {
					final String entriesFileName = mContext.getCompoundEntriesFileName(name);
					
					input = dir.openInput(context, entriesFileName); //, IOContext.READONCE);
					mContext.checkCodecHeader(input, CompoundFileWriter.ENTRY_CODEC, CompoundFileWriter.VERSION_START, CompoundFileWriter.VERSION_START);
					
					final int numEntries = input.readVInt();
					mapping = new HashMap<String, CompoundFileDirectory.FileEntry>(numEntries);
					
					for (int i = 0; i < numEntries; i++) {
						final FileEntry fileEntry = new FileEntry();
						final String id = input.readString();
						
						assert !mapping.containsKey(id): "id=" + id + " was written multiple times in the CFS";
						mapping.put(id, fileEntry);
						
						fileEntry.mOffset = input.readLong();
						fileEntry.mLength = input.readLong();
					}
					
					return mapping;
				} finally {
					IOUtils.close(input);
				}
			} else {
				// TODO remove once 3.x is not supported anymore
				mapping = readLegacyEntries(stream, firstInt);
			}
			
			success = true;
			return mapping;
			
		} finally {
			if (success) 
				IOUtils.close(stream);
			else 
				IOUtils.closeWhileHandlingException(stream);
		}
	}

	final Map<String, FileEntry> readLegacyEntries(IIndexInput stream, 
			int firstInt) throws CorruptIndexException, IOException {
		final Map<String,FileEntry> entries = new HashMap<String,FileEntry>();
		final boolean stripSegmentName;
		final int count;
		
		if (firstInt < CompoundFileWriter.FORMAT_PRE_VERSION) {
			if (firstInt < CompoundFileWriter.FORMAT_NO_SEGMENT_PREFIX) {
				throw new CorruptIndexException("Incompatible format version: "
						+ firstInt + " expected >= " + CompoundFileWriter.FORMAT_NO_SEGMENT_PREFIX 
						+ " (resource: " + stream + ")");
			}
			// It's a post-3.1 index, read the count.
			count = stream.readVInt();
			stripSegmentName = false;
		} else {
			count = firstInt;
			stripSegmentName = true;
		}
    
		// read the directory and init files
		long streamLength = stream.length();
		FileEntry entry = null;
		for (int i=0; i < count; i++) {
			long offset = stream.readLong();
			if (offset < 0 || offset > streamLength) {
				throw new CorruptIndexException("Invalid CFS entry offset: " 
						+ offset + " (resource: " + stream + ")");
			}
			
			String id = stream.readString();
			if (stripSegmentName) {
				// Fix the id to not include the segment names. This is relevant for
				// pre-3.1 indexes.
				id = mContext.stripSegmentName(id);
			}
      
			// set length of the previous entry
			if (entry != null) 
				entry.mLength = offset - entry.mOffset;
      
			entry = new FileEntry();
			entry.mOffset = offset;

			assert !entries.containsKey(id);
			entries.put(id, entry);
		}
    
		// set the length of the final entry
		if (entry != null) 
			entry.mLength = streamLength - entry.mOffset;
    
		return entries;
	}
  
	public IDirectory getDirectory() {
		return mDirectory;
	}
  
	public String getFileName() {
		return mFileName;
	}
  
	@Override
	public synchronized void close() throws IOException {
		if (!mIsOpen) {
			// allow double close - usually to be consistent with other closeables
			return; // already closed
		}
		mIsOpen = false;
		
		if (mWriter != null) {
			assert mOpenForWrite;
			mWriter.close();
		} else {
			IOUtils.close(mHandle);
		}
	}
  
	@Override
	protected synchronized IndexInput openIndexInput(IIndexContext context, String name) 
			throws IOException {
		ensureOpen();
		assert !mOpenForWrite;
		
		final String id = mContext.stripSegmentName(name);
		final FileEntry entry = mEntries.get(id);
		if (entry == null) {
			throw new FileNotFoundException("No sub-file with id " + id + " found (fileName=" 
					+ name + " files: " + mEntries.keySet() + ")");
		}
		
		return (IndexInput)mHandle.openSlice(entry.mOffset, entry.mLength);
	}
  
	/** Returns an array of strings, one for each file in the directory. */
	@Override
	public String[] listAll() {
		ensureOpen();
		
		final String[] res;
		if (mWriter != null) {
			res = mWriter.listAll(); 
			
		} else {
			res = mEntries.keySet().toArray(new String[mEntries.size()]);
			// Add the segment name
			String seg = mContext.parseSegmentName(mFileName);
			for (int i = 0; i < res.length; i++) {
				res[i] = seg + res[i];
			}
		}
		
		return res;
	}
  
	/** Returns true iff a file with the given name exists. */
	@Override
	public boolean fileExists(String name) {
		ensureOpen();
		if (mWriter != null) 
			return mWriter.fileExists(name);
		
		return mEntries.containsKey(mContext.stripSegmentName(name));
	}
  
	/** 
	 * Not implemented
	 * @throws UnsupportedOperationException 
	 */
	@Override
	public void deleteFile(String name) {
		throw new UnsupportedOperationException();
	}
  
	/** 
	 * Not implemented
	 * @throws UnsupportedOperationException 
	 */
	public void renameFile(String from, String to) {
		throw new UnsupportedOperationException();
	}
  
	/** 
	 * Returns the length of a file in the directory.
	 * @throws IOException if the file does not exist 
	 */
	@Override
	public long getFileLength(String name) throws IOException {
		ensureOpen();
		if (mWriter != null) 
			return mWriter.fileLength(name);
		
		FileEntry e = mEntries.get(mContext.stripSegmentName(name));
		if (e == null)
			throw new FileNotFoundException(name);
		
		return e.mLength;
	}
  
	@Override
	protected IndexOutput createIndexOutput(IIndexContext context, String name) throws IOException {
		ensureOpen();
		return mWriter.createOutput(context, name);
	}
  
	@Override
	public void sync(Collection<String> names) throws IOException {
		throw new UnsupportedOperationException();
	}
  
	/** 
	 * Not implemented
	 * @throws UnsupportedOperationException 
	 */
	@Override
	public Lock makeLock(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IInputSlicer createSlicer(IIndexContext context, final String name)
			throws IOException {
		ensureOpen();
		assert !mOpenForWrite;
		
		final String id = mContext.stripSegmentName(name);
		final FileEntry entry = mEntries.get(id);
		if (entry == null) {
			throw new FileNotFoundException("No sub-file with id " + id + " found (fileName=" 
					+ name + " files: " + mEntries.keySet() + ")");
		}
		
		return new IndexInputSlicer() {
				@Override
				public void close() throws IOException {}
	      
				@Override
				public IIndexInput openSlice(long offset, long length) throws IOException {
					return mHandle.openSlice(entry.mOffset + offset, length);
				}
	
				@Override
				public IIndexInput openFullSlice() throws IOException {
					return openSlice(0, entry.mLength);
				}
			};
	}

	@Override
	public String toString() {
		return "CompoundFileDirectory{file=\"" + mFileName + "\", dir=" + mDirectory + "}";
	}
	
}
