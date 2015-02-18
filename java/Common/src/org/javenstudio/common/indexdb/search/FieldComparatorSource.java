package org.javenstudio.common.indexdb.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IFieldComparatorSource;

/**
 * Provides a {@link FieldComparator} for custom field sorting.
 *
 */
public abstract class FieldComparatorSource implements IFieldComparatorSource {

	/**
	 * Creates a comparator for the field in the given index.
	 * 
	 * @param fieldname
	 *          Name of the field to create comparator for.
	 * @return FieldComparator.
	 * @throws IOException
	 *           If an error occurs reading the index.
	 */
	public abstract IFieldComparator<?> newComparator(String fieldname, 
			int numHits, int sortPos, boolean reversed) throws IOException;
}
