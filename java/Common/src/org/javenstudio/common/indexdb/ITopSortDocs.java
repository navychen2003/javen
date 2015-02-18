package org.javenstudio.common.indexdb;

public interface ITopSortDocs extends ITopDocs {

	/** The fields which were used to sort results by. */
	public ISortField[] getSortFields();
	
}
