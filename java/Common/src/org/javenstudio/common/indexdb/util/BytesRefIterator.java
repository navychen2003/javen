package org.javenstudio.common.indexdb.util;

import java.io.IOException;
import java.util.Comparator;

/**
 * A simple iterator interface for {@link BytesRef} iteration.
 */
public interface BytesRefIterator {

  /**
   * Increments the iteration to the next {@link BytesRef} in the iterator.
   * Returns the resulting {@link BytesRef} or <code>null</code> if the end of
   * the iterator is reached. The returned BytesRef may be re-used across calls
   * to next. After this method returns null, do not call it again: the results
   * are undefined.
   * 
   * @return the next {@link BytesRef} in the iterator or <code>null</code> if
   *         the end of the iterator is reached.
   * @throws IOException
   */
  public BytesRef next() throws IOException;
  
  /**
   * Return the {@link BytesRef} Comparator used to sort terms provided by the
   * iterator. This may return null if there are no items or the iterator is not
   * sorted. Callers may invoke this method many times, so it's best to cache a
   * single instance & reuse it.
   */
  public Comparator<BytesRef> getComparator();

  /** Singleton BytesRefIterator that iterates over 0 BytesRefs. */
  public static final BytesRefIterator EMPTY = new BytesRefIterator() {
	    @Override
	    public BytesRef next() throws IOException {
	      return null;
	    }
	    
	    public Comparator<BytesRef> getComparator() {
	      return null;
	    }
	  };

}
