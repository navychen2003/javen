package org.javenstudio.hornet.store.packed;

import java.io.Closeable;
import java.io.IOException;

import org.javenstudio.common.indexdb.util.LongsRef;

/**
 * Run-once iterator interface, to decode previously saved PackedInts.
 */
public interface ReaderIterator extends Closeable {
	
	/** Returns next value */
	public long next() throws IOException;
	
    /** 
     * Returns at least 1 and at most <code>count</code> next values,
     * the returned ref MUST NOT be modified 
     */
	public LongsRef next(int count) throws IOException;
	
    /** Returns number of bits per value */
	public int getBitsPerValue();
	
    /** Returns number of values */
	public int size();
	
    /** Returns the current position */
	public int ord();
	
}
