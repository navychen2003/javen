package org.javenstudio.common.indexdb;

import java.io.IOException;

public interface IFieldComparatorSource {

	/**
	 * Creates a comparator for the field in the given index.
	 * 
	 * @param fieldname
	 *          Name of the field to create comparator for.
	 * @return FieldComparator.
	 * @throws IOException
	 *           If an error occurs reading the index.
	 */
	public IFieldComparator<?> newComparator(String fieldname, 
			int numHits, int sortPos, boolean reversed) throws IOException;
	
}
