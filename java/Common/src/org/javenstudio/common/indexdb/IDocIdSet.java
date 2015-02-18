package org.javenstudio.common.indexdb;

import java.io.IOException;

import org.javenstudio.common.indexdb.util.Bits;

public interface IDocIdSet {

	/** 
	 * Provides a {@link DocIdSetIterator} to access the set.
	 * This implementation can return <code>null</code> or
	 * <code>{@linkplain #EMPTY_DOCIDSET}.iterator()</code> if there
	 * are no docs that match. 
	 */
	public IDocIdSetIterator iterator() throws IOException;
	
	/** 
	 * Optionally provides a {@link Bits} interface for random access
	 * to matching documents.
	 * @return {@code null}, if this {@code DocIdSet} does not support random access.
	 * In contrast to {@link #iterator()}, a return value of {@code null}
	 * <b>does not</b> imply that no documents match the filter!
	 * The default implementation does not provide random access, so you
	 * only need to implement this method if your DocIdSet can
	 * guarantee random access to every docid in O(1) time without
	 * external disk access (as {@link Bits} interface cannot throw
	 * {@link IOException}). This is generally true for bit sets
	 * like {@link FixedBitSet}, which return
	 * itsself if they are used as {@code DocIdSet}.
	 */
	public Bits getBits() throws IOException;
	
	/**
	 * This method is a hint for {@link CachingWrapperFilter}, if this <code>DocIdSet</code>
	 * should be cached without copying it into a BitSet. The default is to return
	 * <code>false</code>. If you have an own <code>DocIdSet</code> implementation
	 * that does its iteration very effective and fast without doing disk I/O,
	 * override this method and return <code>true</here>.
	 */
	public boolean isCacheable();
	
}
