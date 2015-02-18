package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface IDataOutput {

	/** 
	 * Writes a single byte.
	 * <p>
	 * The most primitive data type is an eight-bit byte. Files are 
	 * accessed as sequences of bytes. All other data types are defined 
	 * as sequences of bytes, so file formats are byte-order independent.
	 * 
	 * @see IndexInput#readByte()
	 */
	public void writeByte(byte b) throws IOException;
	
	/** 
	 * Writes an array of bytes.
	 * @param b the bytes to write
	 * @param length the number of bytes to write
	 * @see DataInput#readBytes(byte[],int,int)
	 */
	public void writeBytes(byte[] b, int length) throws IOException;
	
	/** 
	 * Writes an array of bytes.
	 * @param b the bytes to write
	 * @param offset the offset in the byte array
	 * @param length the number of bytes to write
	 * @see DataInput#readBytes(byte[],int,int)
	 */
	public void writeBytes(byte[] b, int offset, int length) throws IOException;
	
	/** 
	 * Writes an int as four bytes.
	 * <p>
	 * 32-bit unsigned integer written as four bytes, high-order bytes first.
	 * 
	 * @see DataInput#readInt()
	 */
	public void writeInt(int i) throws IOException;
	
	/** 
	 * Writes a short as two bytes.
	 * @see DataInput#readShort()
	 */
	public void writeShort(short i) throws IOException;
	
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
	public void writeVInt(int i) throws IOException;
	
	/** 
	 * Writes a long as eight bytes.
	 * <p>
	 * 64-bit unsigned integer written as eight bytes, high-order bytes first.
	 * 
	 * @see DataInput#readLong()
	 */
	public void writeLong(long i) throws IOException;
	
	/** 
	 * Writes an long in a variable-length format.  Writes between one and nine
	 * bytes.  Smaller values take fewer bytes.  Negative numbers are not
	 * supported.
	 * <p>
	 * The format is described further in {@link DataOutput#writeVInt(int)}.
	 * @see DataInput#readVLong()
	 */
	public void writeVLong(long i) throws IOException;
	
	/** 
	 * Writes a string.
	 * <p>
	 * Writes strings as UTF-8 encoded bytes. First the length, in bytes, is
	 * written as a {@link #writeVInt VInt}, followed by the bytes.
	 * 
	 * @see DataInput#readString()
	 */
	public void writeString(String s) throws IOException;
	
	/**
	 * Writes a String map.
	 * <p>
	 * First the size is written as an {@link #writeInt(int) Int32},
	 * followed by each key-value pair written as two consecutive 
	 * {@link #writeString(String) String}s.
	 * 
	 * @param map Input map. May be null (equivalent to an empty map)
	 */
	public void writeStringStringMap(Map<String,String> map) throws IOException;
	
	/**
	 * Writes a String set.
	 * <p>
	 * First the size is written as an {@link #writeInt(int) Int32},
	 * followed by each value written as a
	 * {@link #writeString(String) String}.
	 * 
	 * @param set Input set. May be null (equivalent to an empty set)
	 */
	public void writeStringSet(Set<String> set) throws IOException;
	
}
