package org.javenstudio.common.indexdb.store;

import java.io.IOException;

public final class IndexUtil {

	public interface Input { 
		public byte readByte() throws IOException;
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
	public static int readVInt(Input input) throws IOException {
		/* This is the original code of this method,
		 * but a Hotspot bug (see LUCENE-2975) corrupts the for-loop if
		 * readByte() is inlined. So the loop was unwinded!
    		byte b = readByte();
    		int i = b & 0x7F;
    		for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      			b = readByte();
      			i |= (b & 0x7F) << shift;
    		}
    		return i;
		 */
		
		byte b = input.readByte();
		if (b >= 0) return b;
		int i = b & 0x7F;
		b = input.readByte();
		i |= (b & 0x7F) << 7;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7F) << 14;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7F) << 21;
		if (b >= 0) return i;
		b = input.readByte();
		// Warning: the next ands use 0x0F / 0xF0 - beware copy/paste errors:
		i |= (b & 0x0F) << 28;
		if ((b & 0xF0) == 0) return i;
		throw new IOException("Invalid vInt detected (too many bits)");
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
	public static long readVLong(Input input) throws IOException {
		/* This is the original code of this method,
		 * but a Hotspot bug (see LUCENE-2975) corrupts the for-loop if
		 * readByte() is inlined. So the loop was unwinded!
    		byte b = readByte();
    		long i = b & 0x7F;
    		for (int shift = 7; (b & 0x80) != 0; shift += 7) {
      			b = readByte();
      			i |= (b & 0x7FL) << shift;
    		}
    		return i;
		 */
		
		byte b = input.readByte();
		if (b >= 0) return b;
		long i = b & 0x7FL;
		b = input.readByte();
		i |= (b & 0x7FL) << 7;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7FL) << 14;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7FL) << 21;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7FL) << 28;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7FL) << 35;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7FL) << 42;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7FL) << 49;
		if (b >= 0) return i;
		b = input.readByte();
		i |= (b & 0x7FL) << 56;
		if (b >= 0) return i;
		throw new IOException("Invalid vLong detected (negative values disallowed)");
	}
	
}
