package org.javenstudio.falcon.search.hits;

import java.util.Iterator;

/**
 * Simple Iterator of document Ids which may include score information.
 *
 * <p>
 * The order of the documents is determined by the context in which the
 * DocIterator instance was retrieved.
 * </p>
 *
 *
 */
public interface DocIterator extends Iterator<Integer> {
	
	// already declared in superclass, redeclaring prevents javadoc inheritance
	//public boolean hasNext();

	/**
	 * Returns the next document id if <code>hasNext()==true</code>
	 *
	 * This method is equivalent to <code>next()</code>, but avoids the creation
	 * of an Integer Object.
	 * @see #next()
	 */
	public int nextDoc();

	/**
	 * Returns the score for the document just returned by <code>nextDoc()</code>
	 *
	 * <p>
	 * The value returned may be meaningless depending on the context
	 * in which the DocIterator instance was retrieved.
	 */
	public float score();
	
}
