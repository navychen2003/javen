package org.javenstudio.common.indexdb;

import org.javenstudio.common.indexdb.util.Bits;

public interface IFixedBitSet extends IDocIdSet, Bits {

	/** Expert. */
	public long[] getBitsArray();
	
	/** 
	 * Returns number of set bits.  NOTE: this visits every
	 *  long in the backing bits array, and the result is not
	 *  internally cached! 
	 */
	public int cardinality();
	
	public boolean get(int index);
	public void set(int index);
	public boolean getAndSet(int index);
	
	public void clear(int index);
	public boolean getAndClear(int index);
	
	/** 
	 * Returns the index of the first set bit starting at the index specified.
	 *  -1 is returned if there are no more set bits.
	 */
	public int nextSetBit(int index);
	
	/** 
	 * Returns the index of the last set bit before or on the index specified.
	 *  -1 is returned if there are no more set bits.
	 */
	public int prevSetBit(int index);
	
}
