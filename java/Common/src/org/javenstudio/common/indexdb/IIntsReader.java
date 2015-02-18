package org.javenstudio.common.indexdb;

public interface IIntsReader {

	/**
	 * At most 700% memory overhead, always select a direct implementation.
	 */
	public static final float FASTEST = 7f;

	/**
	 * At most 50% memory overhead, always select a reasonably fast implementation.
	 */
	public static final float FAST = 0.5f;

	/**
	 * At most 20% memory overhead.
	 */
	public static final float DEFAULT = 0.2f;

	/**
	 * No memory overhead at all, but the returned implementation may be slow.
	 */
	public static final float COMPACT = 0f;
	
	/**
	 * @param index the position of the wanted value.
	 * @return the value at the stated index.
	 */
	public long get(int index);

	/**
	 * Bulk get: read at least one and at most <code>len</code> longs starting
	 * from <code>index</code> into <code>arr[off:off+len]</code> and return
	 * the actual number of values that have been read.
	 */
	public int get(int index, long[] arr, int off, int len);

	/**
	 * @return the number of bits used to store any given value.
	 *         Note: This does not imply that memory usage is
	 *         {@code bitsPerValue * #values} as implementations are free to
	 *         use non-space-optimal packing of bits.
	 */
	public int getBitsPerValue();

	/**
	 * @return the number of values.
	 */
	public int size();

	/**
	 * Return the in-memory size in bytes.
	 */
	public long ramBytesUsed();

	/**
	 * Expert: if the bit-width of this reader matches one of
	 * java's native types, returns the underlying array
	 * (ie, byte[], short[], int[], long[]); else, returns
	 * null.  Note that when accessing the array you must
	 * upgrade the type (bitwise AND with all ones), to
	 * interpret the full value as unsigned.  Ie,
	 * bytes[idx]&0xFF, shorts[idx]&0xFFFF, etc.
	 */
	public Object getArray();

	/**
	 * Returns true if this implementation is backed by a
	 * native java array.
	 *
	 * @see #getArray
	 */
	public boolean hasArray();
	
}
