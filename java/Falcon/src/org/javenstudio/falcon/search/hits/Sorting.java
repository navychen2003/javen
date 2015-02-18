package org.javenstudio.falcon.search.hits;

import org.javenstudio.common.indexdb.search.FieldComparatorSource;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.hornet.search.AdvancedSortField;
import org.javenstudio.falcon.search.comparator.MissingStringLastSource;

/**
 * Extra indexdb sorting utilities & convenience methods
 *
 */
public class Sorting {

	static final FieldComparatorSource sNullStringLastComparatorSource = 
			new MissingStringLastSource(null);
	
	/** 
	 * Returns a {@link SortField} for a string field.
	 *  If nullLast and nullFirst are both false, then default indexdb string sorting is used where
	 *  null strings sort first in an ascending sort, and last in a descending sort.
	 *
	 * @param fieldName   the name of the field to sort on
	 * @param reverse     true for a reverse (desc) sort
	 * @param nullLast    true if null should come last, regardless of sort order
	 * @param nullFirst   true if null should come first, regardless of sort order
	 * @return SortField
	 */
	public static SortField getStringSortField(String fieldName, boolean reverse, 
			boolean nullLast, boolean nullFirst) {
		if (nullLast) {
			if (!reverse) {
				return new AdvancedSortField(fieldName, 
						sNullStringLastComparatorSource);
			} else {
				return new AdvancedSortField(fieldName, 
						SortField.Type.STRING, true);
			}
			
		} else if (nullFirst) {
			if (reverse) {
				return new AdvancedSortField(fieldName, 
						sNullStringLastComparatorSource, true);
			} else { 
				return new AdvancedSortField(fieldName, 
						SortField.Type.STRING, false);
			}
			
		} else {
			return new AdvancedSortField(fieldName, 
					SortField.Type.STRING, reverse);
		}
	}
	
}
