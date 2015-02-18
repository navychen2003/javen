package org.javenstudio.falcon.search.query;

/** 
 * The ExtendedQuery interface provides extra metadata to a query.
 *  Implementations of ExtendedQuery must also extend Query.
 */
public interface ExtendedQuery {
	
	/** Should this query be cached in the query cache or filter cache. */
	public boolean getCache();

	public void setCache(boolean cache);

	/** 
	 * Returns the cost of this query, used to order checking of filters that are not cached.
	 * If getCache()==false && getCost()>=100 && this instanceof PostFilter, then
	 * the PostFilter interface will be used for filtering.
	 */
	public int getCost();

	public void setCost(int cost);

	/** 
	 * If true, the clauses of this boolean query should be cached separately. 
	 * This is not yet implemented. 
	 */
	public boolean getCacheSep();
	
	public void setCacheSep(boolean cacheSep);
	
}
