package org.javenstudio.hornet.store.packed;

import org.javenstudio.common.indexdb.IIntsReader;

/**
 * A read-only random access array of positive integers.
 * 
 */
abstract class IntsReader implements IIntsReader {
  
	/**
	 * @param index the position of the wanted value.
	 * @return the value at the stated index.
	 */
	public abstract long get(int index);

	/**
	 * Bulk get: read at least one and at most <code>len</code> longs starting
	 * from <code>index</code> into <code>arr[off:off+len]</code> and return
	 * the actual number of values that have been read.
	 */
	public abstract int get(int index, long[] arr, int off, int len);

	/**
	 * @return the number of bits used to store any given value.
	 *         Note: This does not imply that memory usage is
	 *         {@code bitsPerValue * #values} as implementations are free to
	 *         use non-space-optimal packing of bits.
	 */
	public abstract int getBitsPerValue();

	/**
	 * @return the number of values.
	 */
	public abstract int size();

	/**
	 * Return the in-memory size in bytes.
	 */
	public abstract long ramBytesUsed();

	/**
	 * Expert: if the bit-width of this reader matches one of
	 * java's native types, returns the underlying array
	 * (ie, byte[], short[], int[], long[]); else, returns
	 * null.  Note that when accessing the array you must
	 * upgrade the type (bitwise AND with all ones), to
	 * interpret the full value as unsigned.  Ie,
	 * bytes[idx]&0xFF, shorts[idx]&0xFFFF, etc.
	 */
	public abstract Object getArray();

	/**
	 * Returns true if this implementation is backed by a
	 * native java array.
	 *
	 * @see #getArray
	 */
	public abstract boolean hasArray();

}
