package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Set;

public interface IQuery extends Cloneable {

	/** 
	 * Sets the boost for this query clause to <code>b</code>.  Documents
	 * matching this clause will (in addition to the normal weightings) have
	 * their score multiplied by <code>b</code>.
	 */
	public void setBoost(float b);
	
	/** 
	 * Gets the boost for this clause.  Documents matching
	 * this clause will (in addition to the normal weightings) have their score
	 * multiplied by <code>b</code>.   The boost is 1.0 by default.
	 */
	public float getBoost();
	
	/**
	 * Expert: Constructs an appropriate Weight implementation for this query.
	 * 
	 * <p>
	 * Only implemented by primitive queries, which re-write to themselves.
	 */
	public IWeight createWeight(ISearcher searcher) throws IOException;
	
	/** 
	 * Expert: called to re-write queries into primitive queries. For example,
	 * a PrefixQuery will be rewritten into a BooleanQuery that consists
	 * of TermQuerys.
	 */
	public IQuery rewrite(IIndexReader reader) throws IOException;
	
	/**
	 * Expert: adds all terms occurring in this query to the terms set. Only
	 * works if this query is in its {@link #rewrite rewritten} form.
	 * 
	 * @throws UnsupportedOperationException if this query is not yet rewritten
	 */
	public void extractTerms(Set<ITerm> terms);
	
	/** 
	 * Prints a query to a string, with <code>field</code> assumed to be the 
	 * default field and omitted.
	 */
	public String toString(String field);
	
}
