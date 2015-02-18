package org.javenstudio.common.indexdb.store;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IDataOutput;
import org.javenstudio.common.indexdb.IIndexInput;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.common.indexdb.util.UnicodeUtil;

/**
 * Abstract base class for performing write operations of Indexdb's low-level
 * data types.
 */
public abstract class DataOutput implements IDataOutput {

	private static int COPY_BUFFER_SIZE = 16384;
	private byte[] mCopyBuffer;
	
	/** 
	 * Writes a single byte.
	 * <p>
	 * The most primitive data type is an eight-bit byte. Files are 
	 * accessed as sequences of bytes. All other data types are defined 
	 * as sequences of bytes, so file formats are byte-order independent.
	 * 
	 * @see IndexInput#readByte()
	 */
	public abstract void writeByte(byte b) throws IOException;

	/** 
	 * Writes an array of bytes.
	 * @param b the bytes to write
	 * @param length the number of bytes to write
	 * @see DataInput#readBytes(byte[],int,int)
	 */
	public void writeBytes(byte[] b, int length) throws IOException {
		writeBytes(b, 0, length);
	}

	/** 
	 * Writes an array of bytes.
	 * @param b the bytes to write
	 * @param offset the offset in the byte array
	 * @param length the number of bytes to write
	 * @see DataInput#readBytes(byte[],int,int)
	 */
	public abstract void writeBytes(byte[] b, int offset, int length) throws IOException;

	/** 
	 * Writes an int as four bytes.
	 * <p>
	 * 32-bit unsigned integer written as four bytes, high-order bytes first.
	 * 
	 * @see DataInput#readInt()
	 */
	public void writeInt(int i) throws IOException {
		writeByte((byte)(i >> 24));
		writeByte((byte)(i >> 16));
		writeByte((byte)(i >>  8));
		writeByte((byte) i);
	}
  
	/** 
	 * Writes a short as two bytes.
	 * @see DataInput#readShort()
	 */
	public void writeShort(short i) throws IOException {
		writeByte((byte)(i >>  8));
		writeByte((byte) i);
	}

	/** 
	 * Writes an int in a variable-length format.  Writes between one and
	 * five bytes.  Smaller values take fewer bytes.  Negative numbers are
	 * supported, but should be avoided.
	 * <p>VByte is a variable-length format for positive integers is defined where the
	 * high-order bit of each byte indicates whether more bytes remain to be read. The
	 * low-order seven bits are appended as increasingly more significant bits in the
	 * resulting integer value. Thus values from zero to 127 may be stored in a single
	 * byte, values from 128 to 16,383 may be stored in two bytes, and so on.</p>
	 * <p>VByte Encoding Example</p>
	 * <table cellspacing="0" cellpadding="2" border="0">
	 * <col width="64*">
	 * <col width="64*">
	 * <col width="64*">
	 * <col width="64*">
	 * <tr valign="top">
	 *   <th align="left" width="25%">Value</th>
	 *   <th align="left" width="25%">Byte 1</th>
	 *   <th align="left" width="25%">Byte 2</th>
	 *   <th align="left" width="25%">Byte 3</th>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">0</td>
	 *   <td width="25%"><kbd>00000000</kbd></td>
	 *   <td width="25%"></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">1</td>
	 *   <td width="25%"><kbd>00000001</kbd></td>
	 *   <td width="25%"></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">2</td>
	 *   <td width="25%"><kbd>00000010</kbd></td>
	 *   <td width="25%"></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr>
	 *   <td valign="top" width="25%">...</td>
	 *   <td valign="bottom" width="25%"></td>
	 *   <td valign="bottom" width="25%"></td>
	 *   <td valign="bottom" width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">127</td>
	 *   <td width="25%"><kbd>01111111</kbd></td>
	 *   <td width="25%"></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">128</td>
	 *   <td width="25%"><kbd>10000000</kbd></td>
	 *   <td width="25%"><kbd>00000001</kbd></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">129</td>
	 *   <td width="25%"><kbd>10000001</kbd></td>
	 *   <td width="25%"><kbd>00000001</kbd></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">130</td>
	 *   <td width="25%"><kbd>10000010</kbd></td>
	 *   <td width="25%"><kbd>00000001</kbd></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr>
	 *   <td valign="top" width="25%">...</td>
	 *   <td width="25%"></td>
	 *   <td width="25%"></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">16,383</td>
	 *   <td width="25%"><kbd>11111111</kbd></td>
	 *   <td width="25%"><kbd>01111111</kbd></td>
	 *   <td width="25%"></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">16,384</td>
	 *   <td width="25%"><kbd>10000000</kbd></td>
	 *   <td width="25%"><kbd>10000000</kbd></td>
	 *   <td width="25%"><kbd>00000001</kbd></td>
	 * </tr>
	 * <tr valign="bottom">
	 *   <td width="25%">16,385</td>
	 *   <td width="25%"><kbd>10000001</kbd></td>
	 *   <td width="25%"><kbd>10000000</kbd></td>
	 *   <td width="25%"><kbd>00000001</kbd></td>
	 * </tr>
	 * <tr>
	 *   <td valign="top" width="25%">...</td>
	 *   <td valign="bottom" width="25%"></td>
	 *   <td valign="bottom" width="25%"></td>
	 *   <td valign="bottom" width="25%"></td>
	 * </tr>
	 * </table>
	 * <p>This provides compression while still being efficient to decode.</p>
	 * 
	 * @param i Smaller values take fewer bytes.  Negative numbers are
	 * supported, but should be avoided.
	 * @throws IOException If there is an I/O error writing to the underlying medium.
	 * @see DataInput#readVInt()
	 */
	public final void writeVInt(int i) throws IOException {
		while ((i & ~0x7F) != 0) {
			writeByte((byte)((i & 0x7F) | 0x80));
			i >>>= 7;
		}
		writeByte((byte)i);
	}

	/** 
	 * Writes a long as eight bytes.
	 * <p>
	 * 64-bit unsigned integer written as eight bytes, high-order bytes first.
	 * 
	 * @see DataInput#readLong()
	 */
	public void writeLong(long i) throws IOException {
		writeInt((int) (i >> 32));
    	writeInt((int) i);
	}

	/** 
	 * Writes an long in a variable-length format.  Writes between one and nine
	 * bytes.  Smaller values take fewer bytes.  Negative numbers are not
	 * supported.
	 * <p>
	 * The format is described further in {@link DataOutput#writeVInt(int)}.
	 * @see DataInput#readVLong()
	 */
	public final void writeVLong(long i) throws IOException {
		assert i >= 0L;
		while ((i & ~0x7FL) != 0L) {
			writeByte((byte)((i & 0x7FL) | 0x80L));
			i >>>= 7;
		}
		writeByte((byte)i);
	}

	/** 
	 * Writes a string.
	 * <p>
	 * Writes strings as UTF-8 encoded bytes. First the length, in bytes, is
	 * written as a {@link #writeVInt VInt}, followed by the bytes.
	 * 
	 * @see DataInput#readString()
	 */
	public void writeString(String s) throws IOException {
		final BytesRef utf8Result = new BytesRef(10);
		UnicodeUtil.UTF16toUTF8(s, 0, s.length(), utf8Result);
		writeVInt(utf8Result.getLength());
		writeBytes(utf8Result.getBytes(), 0, utf8Result.getLength());
	}

	/** Copy numBytes bytes from input to ourself. */
	public void copyBytes(IIndexInput input, long numBytes) throws IOException {
		assert numBytes >= 0: "numBytes=" + numBytes;
		long left = numBytes;
		if (mCopyBuffer == null)
			mCopyBuffer = new byte[COPY_BUFFER_SIZE];
		
		while (left > 0) {
			int toCopy = (int) left;
			if (left > COPY_BUFFER_SIZE)
				toCopy = COPY_BUFFER_SIZE;
			
			input.readBytes(mCopyBuffer, 0, toCopy);
			writeBytes(mCopyBuffer, 0, toCopy);
			
			left -= toCopy;
		}
	}

	/**
	 * Writes a String map.
	 * <p>
	 * First the size is written as an {@link #writeInt(int) Int32},
	 * followed by each key-value pair written as two consecutive 
	 * {@link #writeString(String) String}s.
	 * 
	 * @param map Input map. May be null (equivalent to an empty map)
	 */
	@Override
	public void writeStringStringMap(Map<String,String> map) throws IOException {
		if (map == null) {
			writeInt(0);
		} else {
			writeInt(map.size());
			for (final Map.Entry<String, String> entry: map.entrySet()) {
				writeString(entry.getKey());
				writeString(entry.getValue());
			}
		}
	}

	/**
	 * Writes a String set.
	 * <p>
	 * First the size is written as an {@link #writeInt(int) Int32},
	 * followed by each value written as a
	 * {@link #writeString(String) String}.
	 * 
	 * @param set Input set. May be null (equivalent to an empty set)
	 */
	@Override
	public void writeStringSet(Set<String> set) throws IOException {
		if (set == null) {
			writeInt(0);
		} else {
			writeInt(set.size());
			for (String value : set) {
				writeString(value);
			}
		}
	}
	
}
