package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IDataInput;
import org.javenstudio.common.indexdb.util.IOUtils;

/**
 * Abstract base class for performing read operations of Indexdb's low-level
 * data types.
 */
public abstract class DataInput implements IndexUtil.Input, IDataInput {

	/** 
	 * Reads and returns a single byte.
	 * @see DataOutput#writeByte(byte)
	 */
	public abstract byte readByte() throws IOException;

	/** 
	 * Reads a specified number of bytes into an array at the specified offset.
	 * @param b the array to read bytes into
	 * @param offset the offset in the array to start storing bytes
	 * @param len the number of bytes to read
	 * @see DataOutput#writeBytes(byte[],int)
	 */
	public abstract void readBytes(byte[] b, int offset, int len)
			throws IOException;

	/** 
	 * Reads a specified number of bytes into an array at the
	 * specified offset with control over whether the read
	 * should be buffered (callers who have their own buffer
	 * should pass in "false" for useBuffer).  Currently only
	 * {@link BufferedIndexInput} respects this parameter.
	 * @param b the array to read bytes into
	 * @param offset the offset in the array to start storing bytes
	 * @param len the number of bytes to read
	 * @param useBuffer set to false if the caller will handle
	 * buffering.
	 * @see DataOutput#writeBytes(byte[],int)
	 */
	public void readBytes(byte[] b, int offset, int len, boolean useBuffer)
			throws IOException {
		// Default to ignoring useBuffer entirely
		readBytes(b, offset, len);
	}

	/** 
	 * Reads two bytes and returns a short.
	 * @see DataOutput#writeByte(byte)
	 */
	public short readShort() throws IOException {
		return (short) (((readByte() & 0xFF) <<  8) |  (readByte() & 0xFF));
	}

	/** 
	 * Reads four bytes and returns an int.
	 * @see DataOutput#writeInt(int)
	 */
	public int readInt() throws IOException {
		return ((readByte() & 0xFF) << 24) | ((readByte() & 0xFF) << 16)
			 | ((readByte() & 0xFF) <<  8) |  (readByte() & 0xFF);
	}

	/** 
	 * Reads an int stored in variable-length format.  Reads between one and
	 * five bytes.  Smaller values take fewer bytes.  Negative numbers are not
	 * supported.
	 * <p>
	 * The format is described further in {@link DataOutput#writeVInt(int)}.
	 * 
	 * @see DataOutput#writeVInt(int)
	 */
	public int readVInt() throws IOException {
		return IndexUtil.readVInt(this);
	}

	/** 
	 * Reads eight bytes and returns a long.
	 * @see DataOutput#writeLong(long)
	 */
	public long readLong() throws IOException {
		return (((long)readInt()) << 32) | (readInt() & 0xFFFFFFFFL);
	}

	/** 
	 * Reads a long stored in variable-length format.  Reads between one and
	 * nine bytes.  Smaller values take fewer bytes.  Negative numbers are not
	 * supported.
	 * <p>
	 * The format is described further in {@link DataOutput#writeVInt(int)}.
	 * 
	 * @see DataOutput#writeVLong(long)
	 */
	public long readVLong() throws IOException {
		return IndexUtil.readVLong(this);
	}

	/** 
	 * Reads a string.
	 * @see DataOutput#writeString(String)
	 */
	public String readString() throws IOException {
		int length = readVInt();
		final byte[] bytes = new byte[length];
		readBytes(bytes, 0, length);
		return new String(bytes, 0, length, IOUtils.CHARSET_UTF_8);
	}

	/** 
	 * Returns a clone of this stream.
	 *
	 * <p>Clones of a stream access the same data, and are positioned at the same
	 * point as the stream they were cloned from.
	 *
	 * <p>Expert: Subclasses must ensure that clones may be positioned at
	 * different points in the input from each other and from the stream they
	 * were cloned from.
	 */
	@Override
	public DataInput clone() {
		DataInput clone = null;
		try {
			clone = (DataInput)super.clone();
		} catch (CloneNotSupportedException e) {
		}

		return clone;
	}

	/** 
	 * Reads a Map&lt;String,String&gt; previously written
	 *  with {@link DataOutput#writeStringStringMap(Map)}. 
	 */
	@Override
	public Map<String,String> readStringStringMap() throws IOException {
		final Map<String,String> map = new HashMap<String,String>();
		final int count = readInt();
		for (int i=0; i < count; i++) {
			final String key = readString();
			final String val = readString();
			map.put(key, val);
		}

		return map;
	}

	/** 
	 * Reads a Set&lt;String&gt; previously written
	 *  with {@link DataOutput#writeStringSet(Set)}. 
	 */
	@Override
	public Set<String> readStringSet() throws IOException {
		final Set<String> set = new HashSet<String>();
		final int count = readInt();
		for (int i=0; i < count; i++) {
			set.add(readString());
		}

		return set;
	}
	
}
