package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface IDataInput extends Cloneable {

	/** 
	 * Reads and returns a single byte.
	 * @see DataOutput#writeByte(byte)
	 */
	public byte readByte() throws IOException;
	
	/** 
	 * Reads a specified number of bytes into an array at the specified offset.
	 * @param b the array to read bytes into
	 * @param offset the offset in the array to start storing bytes
	 * @param len the number of bytes to read
	 * @see DataOutput#writeBytes(byte[],int)
	 */
	public void readBytes(byte[] b, int offset, int len)
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
			throws IOException;
	
	/** 
	 * Reads two bytes and returns a short.
	 * @see DataOutput#writeByte(byte)
	 */
	public short readShort() throws IOException;
	
	/** 
	 * Reads four bytes and returns an int.
	 * @see DataOutput#writeInt(int)
	 */
	public int readInt() throws IOException;
	
	/** 
	 * Reads an int stored in variable-length format.  Reads between one and
	 * five bytes.  Smaller values take fewer bytes.  Negative numbers are not
	 * supported.
	 * <p>
	 * The format is described further in {@link DataOutput#writeVInt(int)}.
	 * 
	 * @see DataOutput#writeVInt(int)
	 */
	public int readVInt() throws IOException;
	
	/** 
	 * Reads eight bytes and returns a long.
	 * @see DataOutput#writeLong(long)
	 */
	public long readLong() throws IOException;
	
	/** 
	 * Reads a long stored in variable-length format.  Reads between one and
	 * nine bytes.  Smaller values take fewer bytes.  Negative numbers are not
	 * supported.
	 * <p>
	 * The format is described further in {@link DataOutput#writeVInt(int)}.
	 * 
	 * @see DataOutput#writeVLong(long)
	 */
	public long readVLong() throws IOException;
	
	/** 
	 * Reads a string.
	 * @see DataOutput#writeString(String)
	 */
	public String readString() throws IOException;
	
	/** 
	 * Reads a Map&lt;String,String&gt; previously written
	 *  with {@link DataOutput#writeStringStringMap(Map)}. 
	 */
	public Map<String,String> readStringStringMap() throws IOException;
	
	/** 
	 * Reads a Set&lt;String&gt; previously written
	 *  with {@link DataOutput#writeStringSet(Set)}. 
	 */
	public Set<String> readStringSet() throws IOException;
	
	/** Cloneable interface */
	public IDataInput clone();
	
}
