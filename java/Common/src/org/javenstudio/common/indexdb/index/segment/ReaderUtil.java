package org.javenstudio.common.indexdb.index.segment;

import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IIndexReaderRef;

/**
 * Common util methods for dealing with {@link IndexReader}s and 
 * {@link IndexReaderContext}s.
 */
public final class ReaderUtil {
	private ReaderUtil() {} // no instance

	/**
	 * Walks up the reader tree and return the given context's top level reader
	 * context, or in other words the reader tree's root context.
	 */
	public static IIndexReaderRef getTopLevel(IIndexReaderRef context) {
		while (context.getParent() != null) {
			context = context.getParent();
		}
		return context;
	}
  
	/**
	 * Returns index of the searcher/reader for document <code>n</code> in the
	 * array used to construct this searcher/reader.
	 */
	public static int subIndex(int n, int[] docStarts) { // find
		// searcher/reader for doc n:
		int size = docStarts.length;
		int lo = 0; // search starts array
		int hi = size - 1; // for first element less than n, return its index
		while (hi >= lo) {
			int mid = (lo + hi) >>> 1;
			int midValue = docStarts[mid];
			if (n < midValue) {
				hi = mid - 1;
			} else if (n > midValue) {
				lo = mid + 1;
			} else { // found a match
				while (mid + 1 < size && docStarts[mid + 1] == midValue) {
					mid ++; // scan to last match
				}
				return mid;
			}
		}
		return hi;
	}
  
	/**
	 * Returns index of the searcher/reader for document <code>n</code> in the
	 * array used to construct this searcher/reader.
	 */
	public static int subIndex(int n, List<IAtomicReaderRef> leaves) { // find
		// searcher/reader for doc n:
		int size = leaves.size();
		int lo = 0; // search starts array
		int hi = size - 1; // for first element less than n, return its index
		while (hi >= lo) {
			int mid = (lo + hi) >>> 1;
			int midValue = leaves.get(mid).getDocBase();
			if (n < midValue) {
				hi = mid - 1;
			} else if (n > midValue) {
				lo = mid + 1;
			} else { // found a match
				while (mid + 1 < size && leaves.get(mid + 1).getDocBase() == midValue) {
					mid ++; // scan to last match
				}
				return mid;
			}
		}
		return hi;
	}
  
}
