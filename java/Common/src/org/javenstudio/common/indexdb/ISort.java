package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface ISort {

	/**
	 * Representation of the sort criteria.
	 * @return Array of SortField objects used in this sort criteria
	 */
	public ISortField[] getSortFields();
	
	/**
	 * Rewrites the SortFields in this Sort, returning a new Sort if any of the fields
	 * changes during their rewriting.
	 *
	 * @param searcher IndexSearcher to use in the rewriting
	 * @return {@code this} if the Sort/Fields have not changed, or a new Sort if there
	 *        is a change
	 * @throws IOException Can be thrown by the rewriting
	 */
	public ISort rewrite(ISearcher searcher) throws IOException;
	
}
