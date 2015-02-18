package org.javenstudio.common.indexdb.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IDocIdSet;
import org.javenstudio.common.indexdb.IDocIdSetIterator;
import org.javenstudio.common.indexdb.util.Bits;

/**
 * A DocIdSet contains a set of doc ids. Implementing classes must
 * only implement {@link #iterator} to provide access to the set. 
 */
public abstract class DocIdSet implements IDocIdSet {

	/** An empty {@code DocIdSet} instance for easy use, e.g. in Filters that hit no documents. */
	public static final DocIdSet EMPTY_DOCIDSET = new DocIdSet() {
    
		private final DocIdSetIterator mIterator = new DocIdSetIterator() {
				@Override
				public int advance(int target) throws IOException { return NO_MORE_DOCS; }
				@Override
				public int getDocID() { return NO_MORE_DOCS; }
				@Override
				public int nextDoc() throws IOException { return NO_MORE_DOCS; }
			};
    
		@Override
		public IDocIdSetIterator iterator() {
			return mIterator;
		}
    
		@Override
		public boolean isCacheable() {
			return true;
		}
    
		// we explicitely provide no random access, as this filter is 100% sparse 
		// and iterator exits faster
		@Override
		public Bits getBits() throws IOException {
			return null;
		}
	};
    
	/** 
	 * Provides a {@link DocIdSetIterator} to access the set.
	 * This implementation can return <code>null</code> or
	 * <code>{@linkplain #EMPTY_DOCIDSET}.iterator()</code> if there
	 * are no docs that match. 
	 */
	public abstract IDocIdSetIterator iterator() throws IOException;

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
	public Bits getBits() throws IOException {
		return null;
	}

	/**
	 * This method is a hint for {@link CachingWrapperFilter}, if this <code>DocIdSet</code>
	 * should be cached without copying it into a BitSet. The default is to return
	 * <code>false</code>. If you have an own <code>DocIdSet</code> implementation
	 * that does its iteration very effective and fast without doing disk I/O,
	 * override this method and return <code>true</here>.
	 */
	public boolean isCacheable() {
		return false;
	}
	
}
