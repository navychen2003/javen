package org.javenstudio.hornet.search;

import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.search.Sort;

public class AdvancedSort extends Sort {

	/**
	 * Represents sorting by computed relevance. Using this sort criteria returns
	 * the same results as calling
	 * {@link IndexSearcher#search(Query,int) IndexSearcher#search()}without a sort criteria,
	 * only with slightly more overhead.
	 */
	public static final Sort RELEVANCE = new AdvancedSort();

	/** Represents sorting by index order. */
	public static final Sort INDEXORDER = new AdvancedSort(AdvancedSortField.FIELD_DOC);
	
	/**
	 * Sorts by computed relevance. This is the same sort criteria as calling
	 * {@link IndexSearcher#search(Query,int) IndexSearcher#search()}without a sort criteria,
	 * only with slightly more overhead.
	 */
	public AdvancedSort() {
		this(AdvancedSortField.FIELD_SCORE);
	}
	
	/** Sorts by the criteria in the given SortField. */
	public AdvancedSort(ISortField field) {
		super(field);
	}
	
	/** Sorts in succession by the criteria in each SortField. */
	public AdvancedSort(ISortField... fields) {
		super(fields);
	}
	
}
