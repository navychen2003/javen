package org.javenstudio.falcon.search.update;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ChannelFastInputStream;
import org.javenstudio.falcon.util.FastInputStream;
import org.javenstudio.falcon.util.FastOutputStream;
import org.javenstudio.falcon.util.JavaBinCodec;
import org.javenstudio.falcon.util.MemOutputStream;

/**
 *  Log Format: List{Operation, Version, ...}
 *  ADD, VERSION, DOC
 *  DELETE, VERSION, ID_BYTES
 *  DELETE_BY_QUERY, VERSION, String
 *
 *  TODO: keep two files, one for [operation, version, id] and the other for the actual
 *  document data.  That way we could throw away document log files more readily
 *  while retaining the smaller operation log files longer (and we can retrieve
 *  the stored fields from the latest documents from the index).
 *
 *  This would require keeping all source fields stored of course.
 *
 *  This would also allow to not log document data for requests with commit=true
 *  in them (since we know that if the request succeeds, all docs will be committed)
 *
 */
public class TransactionLog {
	static final Logger LOG = Logger.getLogger(TransactionLog.class);
	
	public static final String END_MESSAGE = "INDEXDB_TLOG_END";
	public static final String BEGIN_MESSAGE = "INDEXDB_TLOG";

	private AtomicInteger mRefcount = new AtomicInteger(1);
	private Map<String,Integer> mGlobalStringMap = new HashMap<String, Integer>();
	private List<String> mGlobalStringList = new ArrayList<String>();
	
	private File mLogFile;
	private RandomAccessFile mRandFile;
	private FileChannel mChannel;
	private OutputStream mOutput;
	// all accesses to this stream should be synchronized on "this" (The TransactionLog)
	private FastOutputStream mFastOutput; 
	private int mNumRecords;
	@SuppressWarnings("unused")
	private long mId;
  
	// we can delete old tlogs since they are currently only used for 
	// real-time-get (and in the future, recovery)
	private volatile boolean mDeleteOnClose = true; 

	private long mSnapshotSize;
	private int mSnapshotNumRecords;
	private int mLastAddSize;
  
	// write a BytesRef as a byte array
	private JavaBinCodec.ObjectResolver mResolver = new JavaBinCodec.ObjectResolver() {
			@Override
			public Object resolve(Object o, JavaBinCodec codec) throws IOException {
				if (o instanceof BytesRef) {
					BytesRef br = (BytesRef)o;
					codec.writeByteArray(br.getBytes(), br.getOffset(), br.getLength());
					return null;
				}
				return o;
			}
		};

	public static final byte
		INDEXDBINPUTDOC = 16;
		
	public class LogCodec extends JavaBinCodec {
		public LogCodec() {
			super(TransactionLog.this.mResolver);
		}

		@Override
		public void writeExternString(String s) throws IOException {
			if (s == null) {
				writeTag(NULL);
				return;
			}

			// no need to synchronize globalStringMap 
			// - it's only updated before the first record is written to the log
			Integer idx = mGlobalStringMap.get(s);
			if (idx == null) {
				// write a normal string
				writeStr(s);
			} else {
				// write the extern string
				writeTag(EXTERN_STRING, idx);
			}
		}

		@Override
		public String readExternString(FastInputStream fis) throws IOException {
			int idx = readSize(fis);
			if (idx != 0) { 
				// idx != 0 is the index of the extern string
				// no need to synchronize globalStringList - it's only updated before 
				// the first record is written to the log
				return mGlobalStringList.get(idx - 1);
				
			} else {// idx == 0 means it has a string value
				// this shouldn't happen with this codec subclass.
				throw new IOException("Corrupt transaction log");
			}
		}
		
		@Override
		protected Object readKnownType(FastInputStream dis, byte tagByte) throws IOException { 
			switch (mTagByte) {
			case INDEXDBINPUTDOC:
				return readInputDocument(dis);
			}
			return null;
		}
		
		@Override
		public boolean writeKnownType(Object val) throws IOException {
			if (val instanceof InputDocument) {
				writeInputDocument((InputDocument)val);
				return true;
			}
			
			return super.writeKnownType(val);
		}
		
		public InputDocument readInputDocument(FastInputStream dis) throws IOException {
		    int sz = readVInt(dis);
		    float docBoost = (Float)readVal(dis);
		    InputDocument sdoc = new InputDocument();
		    sdoc.setDocumentBoost(docBoost);
		    for (int i = 0; i < sz; i++) {
		    	float boost = 1.0f;
		    	String fieldName;
		    	Object boostOrFieldName = readVal(dis);
		    	if (boostOrFieldName instanceof Float) {
		    		boost = (Float)boostOrFieldName;
		    		fieldName = (String)readVal(dis);
		    	} else {
		    		fieldName = (String)boostOrFieldName;
		    	}
		    	Object fieldVal = readVal(dis);
		    	sdoc.setField(fieldName, fieldVal, boost);
		    }
		    return sdoc;
		}

		public void writeInputDocument(InputDocument sdoc) throws IOException {
		    writeTag(INDEXDBINPUTDOC, sdoc.size());
		    writeFloat(sdoc.getDocumentBoost());
		    for (InputField inputField : sdoc.values()) {
		    	if (inputField.getBoost() != 1.0f) {
		    		writeFloat(inputField.getBoost());
		    	}
		    	writeExternString(inputField.getName());
		    	writeVal(inputField.getValue());
		    }
		}
	}

	TransactionLog(File tlogFile, Collection<String> globalStrings) 
			throws ErrorException {
		this(tlogFile, globalStrings, false);
	}

	TransactionLog(File tlogFile, Collection<String> globalStrings, 
			boolean openExisting) throws ErrorException {
		boolean success = false;
		try {
			if (LOG.isDebugEnabled()) {
				LOG.debug("New TransactionLog file=" + tlogFile + ", exists=" + tlogFile.exists() 
						+ ", size=" + tlogFile.length() + ", openExisting=" + openExisting);
			}

			mLogFile = tlogFile;
			mRandFile = new RandomAccessFile(mLogFile, "rw");
			
			long start = mRandFile.length();
			
			mChannel = mRandFile.getChannel();
			mOutput = Channels.newOutputStream(mChannel);
			mFastOutput = new FastOutputStream(mOutput, new byte[65536], 0);
			// mFastOutput = FastOutputStream.wrap(mOutput);

			if (openExisting) {
				if (start > 0) {
					readHeader(null);
					
					mRandFile.seek(start);
					assert mChannel.position() == start;
					
					// reflect that we aren't starting at the beginning
					mFastOutput.setWritten(start); 
					assert mFastOutput.size() == mChannel.size();
					
				} else {
					addGlobalStrings(globalStrings);
				}
			} else {
				if (start > 0) {
					if (LOG.isDebugEnabled()) {
						LOG.error("New transaction log already exists:" + tlogFile 
								+ " size=" + mRandFile.length());
					}
				}
				
				assert start == 0;
				if (start > 0) 
					mRandFile.setLength(0);
        
				addGlobalStrings(globalStrings);
			}

			success = true;
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			
		} finally {
			if (!success && mRandFile != null) {
				try {
					mRandFile.close();
				} catch (Exception e) {
					if (LOG.isDebugEnabled())
						LOG.error("Error closing tlog file (after error opening)", e);
				}
			}
		}
	}

	public void setDeleteOnClose(boolean b) { mDeleteOnClose = b; }
	
	/** 
	 * Returns the number of records in the log (currently includes 
	 * the header and an optional commit).
	 * Note: currently returns 0 for reopened existing log files.
	 */
	public int getNumRecords() {
		synchronized (this) {
			return mNumRecords;
		}
	}

	public boolean endsWithCommit() throws IOException {
		long size;
		synchronized (this) {
			mFastOutput.flush();
			size = mFastOutput.size();
		}

		// the end of the file should have the end message 
		// (added during a commit) plus a 4 byte size
		byte[] buf = new byte[ END_MESSAGE.length() ];
		long pos = size - END_MESSAGE.length() - 4;
		if (pos < 0) 
			return false;
		
		@SuppressWarnings("resource")
		ChannelFastInputStream is = new ChannelFastInputStream(mChannel, pos);
		is.read(buf);
		
		for (int i=0; i < buf.length; i++) {
			if (buf[i] != END_MESSAGE.charAt(i)) 
				return false;
		}
		
		return true;
	}

	/** 
	 * takes a snapshot of the current position and number of records
	 * for later possible rollback, and returns the position 
	 */
	public long snapshot() {
		synchronized (this) {
			mSnapshotSize = mFastOutput.size();
			mSnapshotNumRecords = mNumRecords;
			return mSnapshotSize;
		}    
	}
  
	// This could mess with any readers or reverse readers that are open, 
	// or anything that might try to do a log lookup.
	// This should only be used to roll back buffered updates, 
	// not actually applied updates.
	public void rollback(long pos) throws IOException {
		synchronized (this) {
			assert mSnapshotSize == pos;
			mFastOutput.flush();
			mRandFile.setLength(pos);
			mFastOutput.setWritten(pos);
			assert mFastOutput.size() == pos;
			mNumRecords = mSnapshotNumRecords;
		}
	}

	public long writeData(Object o) throws ErrorException {
		LogCodec codec = new LogCodec();
		try {
			// if we had flushed, this should be equal to channel.position()
			long pos = mFastOutput.size(); 
			codec.init(mFastOutput);
			codec.writeVal(o);
			return pos;
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void readHeader(FastInputStream fis) throws IOException {
		// read existing header
		fis = fis != null ? fis : new ChannelFastInputStream(mChannel, 0);
		
		LogCodec codec = new LogCodec();
		Map<?,?> header = (Map<?,?>)codec.unmarshal(fis);

		fis.readInt(); // skip size
		// needed to read other records

		synchronized (this) {
			mGlobalStringList = (List<String>)header.get("strings");
			mGlobalStringMap = new HashMap<String, Integer>(mGlobalStringList.size());
			
			for (int i=0; i < mGlobalStringList.size(); i++) {
				mGlobalStringMap.put(mGlobalStringList.get(i), i+1);
			}
		}
	}

	private void addGlobalStrings(Collection<String> strings) {
		if (strings == null) return;
		int origSize = mGlobalStringMap.size();
		
		for (String s : strings) {
			Integer idx = null;
			if (origSize > 0) 
				idx = mGlobalStringMap.get(s);
      
			if (idx != null) 
				continue;  // already in list
			
			mGlobalStringList.add(s);
			mGlobalStringMap.put(s, mGlobalStringList.size());
		}
		
		assert mGlobalStringMap.size() == mGlobalStringList.size();
	}

	protected Collection<String> getGlobalStrings() {
		synchronized (this) {
			return new ArrayList<String>(mGlobalStringList);
		}
	}

	private void writeLogHeader(LogCodec codec) throws IOException {
		long pos = mFastOutput.size();
		assert pos == 0;

		Map<String,Object> header = new LinkedHashMap<String,Object>();
		header.put(BEGIN_MESSAGE, 1); // a magic string + version number
		header.put("strings", mGlobalStringList);
		
		codec.marshal(header, mFastOutput);
		endRecord(pos);
	}

	private void endRecord(long startRecordPosition) throws IOException {
		mFastOutput.writeInt((int)(mFastOutput.size() - startRecordPosition));
		mNumRecords ++;
	}

	private void checkWriteHeader(LogCodec codec, InputDocument optional) 
			throws IOException {
		// Unsynchronized access.  We can get away with an unsynchronized access here
		// since we will never get a false non-zero when the position is in fact 0.
		// rollback() is the only function that can reset to zero, and it blocks updates.
		if (mFastOutput.size() != 0) 
			return;

		synchronized (this) {
			if (mFastOutput.size() != 0) 
				return;  // check again while synchronized
			
			if (optional != null) 
				addGlobalStrings(optional.getFieldNames());
      
			writeLogHeader(codec);
		}
	}

	public long write(AddCommand cmd, int flags) throws ErrorException {
		LogCodec codec = new LogCodec();
		InputDocument sdoc = cmd.getInputDocument();

		try {
			checkWriteHeader(codec, sdoc);

			// adaptive buffer sizing
			int bufSize = mLastAddSize; // unsynchronized access of lastAddSize should be fine
			bufSize = Math.min(1024*1024, bufSize+(bufSize>>3)+256);

			MemOutputStream out = new MemOutputStream(new byte[bufSize]);
			codec.init(out);
			codec.writeTag(JavaBinCodec.ARR, 3);
			codec.writeInt(UpdateLog.ADD | flags);  // should just take one byte
			codec.writeLong(cmd.getVersion());
			codec.writeInputDocument(cmd.getInputDocument());
			
			mLastAddSize = (int)out.size();

			synchronized (this) {
				// if we had flushed, this should be equal to channel.position()
				long pos = mFastOutput.size(); 
				assert pos != 0;

				out.writeAll(mFastOutput);
				endRecord(pos);
				// mFastOutput.flushBuffer();  // flush later
				
				return pos;
			}

		} catch (IOException e) {
			// TODO: reset our file pointer back to "pos", the start of this record.
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error logging add", e);
		}
	}

	public long writeDelete(DeleteCommand cmd, int flags) throws ErrorException {
		LogCodec codec = new LogCodec();

		try {
			checkWriteHeader(codec, null);
			BytesRef br = cmd.getIndexedId();

			MemOutputStream out = new MemOutputStream(new byte[20 + br.getLength()]);
			codec.init(out);
			codec.writeTag(JavaBinCodec.ARR, 3);
			codec.writeInt(UpdateLog.DELETE | flags);  // should just take one byte
			codec.writeLong(cmd.getVersion());
			codec.writeByteArray(br.getBytes(), br.getOffset(), br.getLength());

			synchronized (this) {
				// if we had flushed, this should be equal to channel.position()
				long pos = mFastOutput.size(); 
				assert pos != 0;
				
				out.writeAll(mFastOutput);
				endRecord(pos);
				// mFastOutput.flushBuffer();  // flush later
				
				return pos;
			}
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	public long writeDeleteByQuery(DeleteCommand cmd, int flags) throws ErrorException {
		LogCodec codec = new LogCodec();
		try {
			checkWriteHeader(codec, null);

			MemOutputStream out = new MemOutputStream(
					new byte[20 + (cmd.getQueryString().length())]);
			
			codec.init(out);
			codec.writeTag(JavaBinCodec.ARR, 3);
			codec.writeInt(UpdateLog.DELETE_BY_QUERY | flags);  // should just take one byte
			codec.writeLong(cmd.getVersion());
			codec.writeStr(cmd.getQueryString());

			synchronized (this) {
				// if we had flushed, this should be equal to channel.position()
				long pos = mFastOutput.size(); 
				
				out.writeAll(mFastOutput);
				endRecord(pos);
				// mFastOutput.flushBuffer();  // flush later
				
				return pos;
			}
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	public long writeCommit(CommitCommand cmd, int flags) throws ErrorException {
		LogCodec codec = new LogCodec();
		synchronized (this) {
			try {
				// if we had flushed, this should be equal to channel.position()
				long pos = mFastOutput.size(); 

				if (pos == 0) {
					writeLogHeader(codec);
					pos = mFastOutput.size();
				}
				
				codec.init(mFastOutput);
				codec.writeTag(JavaBinCodec.ARR, 3);
				codec.writeInt(UpdateLog.COMMIT | flags);  // should just take one byte
				codec.writeLong(cmd.getVersion());
				codec.writeStr(END_MESSAGE);  // ensure these bytes are (almost) last in the file

				endRecord(pos);
        
				mFastOutput.flush();  // flush since this will be the last record in a log fill
				assert mFastOutput.size() == mChannel.size();

				return pos;
			} catch (IOException e) {
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
			}
		}
	}

	/** This method is thread safe */
	public Object lookup(long pos) throws ErrorException {
		// A negative position can result from a log replay (which does not re-log, but does
		// update the version map.  This is OK since the node won't be ACTIVE when this happens.
		if (pos < 0) return null;

		try {
			// make sure any unflushed buffer has been flushed
			synchronized (this) {
				// TODO: optimize this by keeping track of what we have flushed up to
				mFastOutput.flushBuffer();
			}

			ChannelFastInputStream fis = new ChannelFastInputStream(mChannel, pos);
			LogCodec codec = new LogCodec();
			
			return codec.readVal(fis);
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	public void increaseRef() throws ErrorException {
		int result = mRefcount.incrementAndGet();
		if (result <= 1) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"incref on a closed log: " + this);
		}
	}

	public boolean try_increaseRef() {
		return mRefcount.incrementAndGet() > 1;
	}

	public void decreaseRef() throws ErrorException {
		if (mRefcount.decrementAndGet() == 0) 
			close();
	}

	/** returns the current position in the log file */
	public long position() {
		synchronized (this) {
			return mFastOutput.size();
		}
	}

	public void finish(UpdateLog.SyncLevel syncLevel) throws ErrorException {
		if (syncLevel == UpdateLog.SyncLevel.NONE) 
			return;
		
		try {
			synchronized (this) {
				mFastOutput.flushBuffer();
			}

			if (syncLevel == UpdateLog.SyncLevel.FSYNC) {
				// Since fsync is outside of synchronized block, we can end up with a partial
				// last record on power failure (which is OK, and does not represent an error...
				// we just need to be aware of it when reading).
				mRandFile.getFD().sync();
			}

		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}

	private void close() throws ErrorException {
		try {
			if (LOG.isDebugEnabled()) 
				LOG.debug("Closing TransactionLog " + this);

			synchronized (this) {
				mFastOutput.flush();
				mFastOutput.close();
			}

			if (mDeleteOnClose) 
				mLogFile.delete();
      
		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
  
	public void forceClose() throws ErrorException {
		if (mRefcount.get() > 0) {
			if (LOG.isErrorEnabled())
				LOG.error("Error: Forcing close of " + this);
			
			mRefcount.set(0);
			close();
		}
	}

	/** 
	 * Returns a reader that can be used while a log is still in use.
	 * Currently only *one* LogReader may be outstanding, and that log may only
	 * be used from a single thread. 
	 */
	public LogReader getReader(long startingPos) throws ErrorException {
		return new LogReader(startingPos);
	}

	/** Returns a single threaded reverse reader */
	public ReverseReader getReverseReader() throws ErrorException {
		return new ReverseReader();
	}

	@Override
	public String toString() {
		return "TransactionLog{file=" + mLogFile.toString() 
				+ " refcount=" + mRefcount.get() + "}";
	}
	
	public class LogReader {
		private ChannelFastInputStream mFastInput;
		private LogCodec mCodec = new LogCodec();

		public LogReader(long startingPos) throws ErrorException {
			increaseRef();
			mFastInput = new ChannelFastInputStream(mChannel, startingPos);
		}

		/** 
		 * Returns the next object from the log, or null if none available.
		 *
		 * @return The log record, or null if EOF
		 * @throws IOException If there is a low-level I/O error.
		 */
		public Object next() throws IOException, InterruptedException {
			long pos = mFastInput.position();

			synchronized (TransactionLog.this) {
				if (LOG.isDebugEnabled()) 
					LOG.debug("Reading log record. pos=" + pos + " currentSize=" + mFastOutput.size());
        
				if (pos >= mFastOutput.size()) 
					return null;

				mFastOutput.flushBuffer();
			}

			if (pos == 0) {
				readHeader(mFastInput);

				// shouldn't currently happen - header and first record 
				// are currently written at the same time
				synchronized (TransactionLog.this) {
					if (mFastInput.position() >= mFastOutput.size()) 
						return null;
					
					pos = mFastInput.position();
				}
			}

			Object o = mCodec.readVal(mFastInput);

			// skip over record size
			int size = mFastInput.readInt();
			assert size == mFastInput.position() - pos - 4;

			return o;
		}

		public void close() throws ErrorException {
			decreaseRef();
		}

		@Override
		public String toString() {
			synchronized (TransactionLog.this) {
				return "LogReader{" + "file=" + mLogFile + ", position=" + mFastInput.position() 
						+ ", end=" + mFastOutput.size() + "}";
			}
		}
	}

	public class ReverseReader {
		private ChannelFastInputStream mFastInput;
		
		private LogCodec mCodec = new LogCodec() {
				@Override
				public InputDocument readInputDocument(FastInputStream dis) {
					// Given that the InputDocument is last in an add record, it's OK to just skip
					// reading it completely.
					return null;
				}
			};

		// length of the next record (the next one closer to the start of the log file)
		private int mNextLength; 
		// where we started reading from last time (so prevPos - nextLength == start of next record)
		private long mPrevPos; 

		public ReverseReader() throws ErrorException {
			increaseRef();

			try {
				long sz;
				synchronized (TransactionLog.this) {
					mFastOutput.flushBuffer();
					sz = mFastOutput.size();
					assert sz == mChannel.size();
				}
	
				mFastInput = new ChannelFastInputStream(mChannel, 0);
				if (sz >= 4) {
					// readHeader(fis);  // should not be needed
					mPrevPos = sz - 4;
					mFastInput.seek(mPrevPos);
					mNextLength = mFastInput.readInt();
				}
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			}
		}

		/** 
		 * Returns the next object from the log, or null if none available.
		 *
		 * @return The log record, or null if EOF
		 * @throws IOException If there is a low-level I/O error.
		 */
		public Object next() throws ErrorException {
			if (mPrevPos <= 0) return null;

			long endOfThisRecord = mPrevPos;
			int thisLength = mNextLength;

			long recordStart = mPrevPos - thisLength;  // back up to the beginning of the next record
			mPrevPos = recordStart - 4;  // back up 4 more to read the length of the next record

			if (mPrevPos <= 0) 
				return null;  // this record is the header

			long bufferPos = mFastInput.getBufferPos();
			
			try { 
				if (mPrevPos >= bufferPos) {
					// nothing to do... we're within the current buffer
					
				} else {
					// Position buffer so that this record is at the end.
					// For small records, this will cause subsequent calls to next() to be within the buffer.
					long seekPos =  endOfThisRecord - mFastInput.getBufferSize();
					// seek to the start of the record if it's larger then the block size.
					seekPos = Math.min(seekPos, mPrevPos); 
					seekPos = Math.max(seekPos, 0);
					
					mFastInput.seek(seekPos);
					mFastInput.peek();  // cause buffer to be filled
				}
	
				mFastInput.seek(mPrevPos);
				// this is the length of the *next* record (i.e. closer to the beginning)
				mNextLength = mFastInput.readInt(); 
	
				// TODO: optionally skip document data
				Object o = mCodec.readVal(mFastInput);
	
				// assert fis.position() == prevPos + 4 + thisLength;  
				// this is only true if we read all the data (and we currently skip reading InputDocument
	
				return o;
			} catch (IOException ex) { 
				throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, ex);
			}
		}

		/** returns the position in the log file of the last record returned by next() */
		public long position() {
			return mPrevPos + 4;  // skip the length
		}

		public void close() throws ErrorException {
			decreaseRef();
		}

		@Override
		public String toString() {
			synchronized (TransactionLog.this) {
				return "LogReader{" + "file=" + mLogFile + ", position=" + mFastInput.position() 
						+ ", end=" + mFastOutput.size() + "}";
			}
		}
	}

}
