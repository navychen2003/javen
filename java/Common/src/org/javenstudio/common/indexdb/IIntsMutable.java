package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IIntsMutable extends IIntsReader {

	/**
	 * Set the value at the given index in the array.
	 * @param index where the value should be positioned.
	 * @param value a value conforming to the constraints set by the array.
	 */
	public abstract void set(int index, long value);

	/**
	 * Bulk set: set at least one and at most <code>len</code> longs starting
	 * at <code>off</code> in <code>arr</code> into this mutable, starting at
	 * <code>index</code>. Returns the actual number of values that have been
	 * set.
	 */
	public abstract int set(int index, long[] arr, int off, int len);

	/**
	 * Fill the mutable from <code>fromIndex</code> (inclusive) to
	 * <code>toIndex</code> (exclusive) with <code>val</code>.
	 */
	public abstract void fill(int fromIndex, int toIndex, long val);

	/**
	 * Sets all values to 0.
	 */
	public abstract void clear();

	/**
	 * Save this mutable into <code>out</code>. Instantiating a reader from
	 * the generated data will return a reader with the same number of bits
	 * per value.
	 */
	public abstract void save(IDataOutput out) throws IOException;
	
}
