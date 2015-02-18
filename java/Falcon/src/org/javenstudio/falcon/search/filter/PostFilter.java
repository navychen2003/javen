package org.javenstudio.falcon.search.filter;

import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.falcon.search.hits.DelegatingCollector;
import org.javenstudio.falcon.search.query.ExtendedQuery;

/** 
 * The PostFilter interface provides a mechanism to further filter documents
 * after they have already gone through the main query and other filters.
 * This is appropriate for filters with a very high cost.
 * <p/>
 * The filtering mechanism used is a {@link DelegatingCollector}
 * that allows the filter to not call the delegate for certain documents,
 * thus effectively filtering them out.  This also avoids the normal
 * filter advancing mechanism which asks for the first acceptable document on
 * or after the target (which is undesirable for expensive filters).
 * This collector interface also enables better performance when an external system
 * must be consulted, since document ids may be buffered and batched into
 * a single request to the external system.
 * <p>
 * Implementations of this interface must also be a Query.
 * If an implementation can only support the collector method of
 * filtering through getFilterCollector, then ExtendedQuery.getCached()
 * should always return false, and ExtendedQuery.getCost() should
 * return no less than 100.
 *
 * @see ExtendedQueryBase
 */
public interface PostFilter extends ExtendedQuery {

	/** 
	 * Returns a DelegatingCollector to be run after the main query 
	 * and all of it's filters, but before any sorting or grouping collectors 
	 */
	public DelegatingCollector getFilterCollector(ISearcher searcher);
	
}
