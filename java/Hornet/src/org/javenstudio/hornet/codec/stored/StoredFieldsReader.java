package org.javenstudio.hornet.codec.stored;

import java.io.IOException;

import org.javenstudio.common.indexdb.AlreadyClosedException;
import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IFieldInfo;
import org.javenstudio.common.indexdb.IFieldInfos;
import org.javenstudio.common.indexdb.IFieldVisitor;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.ISegmentInfo;
import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.hornet.codec.CodecUtil;
import org.javenstudio.hornet.codec.FieldsReader;

/**
 * Class responsible for access to stored document fields.
 * <p/>
 * It uses &lt;segment&gt;.fdt and &lt;segment&gt;.fdx; files.
 * 
 * @see StoredFieldsFormat
 */
final class StoredFieldsReader extends FieldsReader {
	
	private final StoredFieldsFormat mFormat;
	private final IDirectory mDirectory;
	private final String mSegment;
	
	private final IFieldInfos mFieldInfos;
	private final IIndexInput mFieldsStream;
	private final IIndexInput mIndexStream;
	
	private int mNumTotalDocs;
	private int mSize;
	private boolean mClosed;

	/** 
	 * Returns a cloned FieldsReader that shares open
	 *  IndexInputs with the original one.  It is the caller's
	 *  job not to close the original FieldsReader until all
	 *  clones are called (eg, currently SegmentReader manages
	 *  this logic). 
	 */
	@Override
	public StoredFieldsReader clone() {
		ensureOpen();
		return new StoredFieldsReader(mFormat, mDirectory, mSegment, 
				mFieldInfos, mNumTotalDocs, mSize, 
				(IIndexInput)mFieldsStream.clone(), 
				(IIndexInput)mIndexStream.clone());
	}
  
	// Used only by clone
	private StoredFieldsReader(StoredFieldsFormat format, 
			IDirectory dir, String segment, 
			IFieldInfos fieldInfos, int numTotalDocs, int size, 
			IIndexInput fieldsStream, IIndexInput indexStream) {
		mFormat = format;
		mDirectory = dir;
		mSegment = segment;
		mFieldInfos = fieldInfos;
		mNumTotalDocs = numTotalDocs;
		mSize = size;
		mFieldsStream = fieldsStream;
		mIndexStream = indexStream;
	}

	public StoredFieldsReader(StoredFieldsFormat format, 
			IDirectory dir, String segment, ISegmentInfo si, IFieldInfos fn) 
			throws IOException {
		mFormat = format;
		mDirectory = dir;
		mSegment = segment;
		mFieldInfos = fn;
		
		boolean success = false;
		try {
			mFieldsStream = mDirectory.openInput(
					mFormat.getContext(), mFormat.getFieldsFileName(mSegment));
			mIndexStream = mDirectory.openInput(
					mFormat.getContext(), mFormat.getFieldsIndexFileName(mSegment));
      
			CodecUtil.checkHeader(mIndexStream, mFormat.getIndexCodecName(), 
					StoredFieldsFormat.VERSION_START, StoredFieldsFormat.VERSION_CURRENT);
			CodecUtil.checkHeader(mFieldsStream, mFormat.getDataCodecName(), 
					StoredFieldsFormat.VERSION_START, StoredFieldsFormat.VERSION_CURRENT);
			
			assert StoredFieldsFormat.HEADER_LENGTH_DAT == mFieldsStream.getFilePointer();
			assert StoredFieldsFormat.HEADER_LENGTH_IDX == mIndexStream.getFilePointer();
			
			final long indexSize = mIndexStream.length() - StoredFieldsFormat.HEADER_LENGTH_IDX;
			mSize = (int) (indexSize >> 3);
			// Verify two sources of "maxDoc" agree:
			if (mSize != si.getDocCount()) {
				throw new CorruptIndexException("doc counts differ for segment " 
						+ mSegment + ": fieldsReader shows " + mSize + " but segmentInfo shows " 
						+ si.getDocCount());
			}
			
			mNumTotalDocs = (int) (indexSize >> 3);
			success = true;
			
		} finally {
			// With lock-less commits, it's entirely possible (and
			// fine) to hit a FileNotFound exception above. In
			// this case, we want to explicitly close any subset
			// of things that were opened so that we don't have to
			// wait for a GC to do so.
			if (!success) 
				close();
		}
	}

	/**
	 * @throws AlreadyClosedException if this FieldsReader is closed
	 */
	private void ensureOpen() throws AlreadyClosedException {
		if (mClosed) 
			throw new AlreadyClosedException("this FieldsReader is closed");
	}

	/**
	 * Closes the underlying {@link org.apache.lucene.store.IndexInput} streams.
	 * This means that the Fields values will not be accessible.
	 *
	 * @throws IOException
	 */
	public final void close() throws IOException {
		if (!mClosed) {
			IOUtils.close(mFieldsStream, mIndexStream);
			mClosed = true;
		}
	}

	public final int size() {
		return mSize;
	}

	private void seekIndex(int docID) throws IOException {
		mIndexStream.seek(StoredFieldsFormat.HEADER_LENGTH_IDX + docID * 8L);
	}

	public final void visitDocument(int n, IFieldVisitor visitor) throws IOException {
		seekIndex(n);
		
		mFieldsStream.seek(mIndexStream.readLong());
		final int numFields = mFieldsStream.readVInt();
		
		for (int fieldIDX = 0; fieldIDX < numFields; fieldIDX++) {
			int fieldNumber = mFieldsStream.readVInt();
			IFieldInfo fieldInfo = mFieldInfos.getFieldInfo(fieldNumber);
      
			int bits = mFieldsStream.readByte() & 0xFF;
			assert bits <= (StoredFieldsWriter.FIELD_IS_NUMERIC_MASK | StoredFieldsWriter.FIELD_IS_BINARY): 
				"bits=" + Integer.toHexString(bits);

			switch(visitor.needsField(fieldInfo)) {
			case YES:
				readField(visitor, fieldInfo, bits);
				break;
			case NO: 
				skipField(bits);
				break;
			case STOP: 
				return;
			}
		}
	}

	private void readField(IFieldVisitor visitor, IFieldInfo info, int bits) throws IOException {
		final int numeric = bits & StoredFieldsWriter.FIELD_IS_NUMERIC_MASK;
		if (numeric != 0) {
			switch(numeric) {
			case StoredFieldsWriter.FIELD_IS_NUMERIC_INT:
				visitor.addIntField(info, mFieldsStream.readInt());
				return;
			case StoredFieldsWriter.FIELD_IS_NUMERIC_LONG:
				visitor.addLongField(info, mFieldsStream.readLong());
				return;
			case StoredFieldsWriter.FIELD_IS_NUMERIC_FLOAT:
				visitor.addFloatField(info, Float.intBitsToFloat(mFieldsStream.readInt()));
				return;
			case StoredFieldsWriter.FIELD_IS_NUMERIC_DOUBLE:
				visitor.addDoubleField(info, Double.longBitsToDouble(mFieldsStream.readLong()));
				return;
			default:
				throw new CorruptIndexException("Invalid numeric type: " + 
						Integer.toHexString(numeric));
			}
		} else { 
			final int length = mFieldsStream.readVInt();
			byte bytes[] = new byte[length];
			mFieldsStream.readBytes(bytes, 0, length);
			if ((bits & StoredFieldsWriter.FIELD_IS_BINARY) != 0) {
				visitor.addBinaryField(info, bytes, 0, bytes.length);
			} else {
				visitor.addStringField(info, new String(bytes, 0, bytes.length, 
						IOUtils.CHARSET_UTF_8));
			}
		}
	}
  
	private void skipField(int bits) throws IOException {
		final int numeric = bits & StoredFieldsWriter.FIELD_IS_NUMERIC_MASK;
		if (numeric != 0) {
			switch(numeric) {
			case StoredFieldsWriter.FIELD_IS_NUMERIC_INT:
			case StoredFieldsWriter.FIELD_IS_NUMERIC_FLOAT:
				mFieldsStream.readInt();
				return;
			case StoredFieldsWriter.FIELD_IS_NUMERIC_LONG:
			case StoredFieldsWriter.FIELD_IS_NUMERIC_DOUBLE:
				mFieldsStream.readLong();
				return;
			default: 
				throw new CorruptIndexException("Invalid numeric type: " + 
						Integer.toHexString(numeric));
			}
		} else {
			final int length = mFieldsStream.readVInt();
			mFieldsStream.seek(mFieldsStream.getFilePointer() + length);
		}
	}

	/** 
	 * Returns the length in bytes of each raw document in a
	 *  contiguous range of length numDocs starting with
	 *  startDocID.  Returns the IndexInput (the fieldStream),
	 *  already seeked to the starting point for startDocID.
	 */
	final IIndexInput rawDocs(int[] lengths, int startDocID, int numDocs) 
			throws IOException {
		seekIndex(startDocID);
		
		long startOffset = mIndexStream.readLong();
		long lastOffset = startOffset;
		
		int count = 0;
		while (count < numDocs) {
			final long offset;
			final int docID = startDocID + count + 1;
			assert docID <= mNumTotalDocs;
			
			if (docID < mNumTotalDocs) 
				offset = mIndexStream.readLong();
			else
				offset = mFieldsStream.length();
			
			lengths[count++] = (int) (offset-lastOffset);
			lastOffset = offset;
		}

		mFieldsStream.seek(startOffset);

		return mFieldsStream;
	}
	
}
