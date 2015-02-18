package org.javenstudio.common.indexdb;

import java.util.Iterator;

public interface IBooleanQuery extends IQuery, Iterable<IBooleanClause> {

	/** 
	 * Adds a clause to a boolean query.
	 *
	 * @throws TooManyClauses if the new number of clauses exceeds the maximum clause number
	 * @see #getMaxClauseCount()
	 */
	public void add(IQuery query, IBooleanClause.Occur occur);
	
	/** 
	 * Adds a clause to a boolean query.
	 * @throws TooManyClauses if the new number of clauses exceeds the maximum clause number
	 * @see #getMaxClauseCount()
	 */
	public void add(IBooleanClause clause);
	
	/** Returns the set of clauses in this query. */
	public IBooleanClause[] getClauses();
	
	/** 
	 * Returns an iterator on the clauses in this query. It implements the {@link Iterable} interface to
	 * make it possible to do:
	 * <pre>for (BooleanClause clause : booleanQuery) {}</pre>
	 */
	public Iterator<IBooleanClause> iterator();
	
}
