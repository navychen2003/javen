package org.javenstudio.hornet.search;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.search.FieldComparatorSource;
import org.javenstudio.common.indexdb.search.Sort;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.hornet.search.comparator.ByteComparator;
import org.javenstudio.hornet.search.comparator.DocComparator;
import org.javenstudio.hornet.search.comparator.DoubleComparator;
import org.javenstudio.hornet.search.comparator.FloatComparator;
import org.javenstudio.hornet.search.comparator.IntComparator;
import org.javenstudio.hornet.search.comparator.LongComparator;
import org.javenstudio.hornet.search.comparator.RelevanceComparator;
import org.javenstudio.hornet.search.comparator.ShortComparator;
import org.javenstudio.hornet.search.comparator.TermOrdValComparator;
import org.javenstudio.hornet.search.comparator.TermValComparator;

public class AdvancedSortField extends SortField {

	/** Represents sorting by document score (relevance). */
	public static final SortField FIELD_SCORE = new AdvancedSortField(null, Type.SCORE);

	/** Represents sorting by document number (index order). */
	public static final SortField FIELD_DOC = new AdvancedSortField(null, Type.DOC);
	
	/** 
	 * Creates a sort by terms in the given field with the type of term
	 * values explicitly given.
	 * @param field  Name of field to sort by.  Can be <code>null</code> if
	 *               <code>type</code> is SCORE or DOC.
	 * @param type   Type of values in the terms.
	 */
	public AdvancedSortField(String field, Type type) {
		super(field, type);
	}
	
	/** 
	 * Creates a sort, possibly in reverse, by terms in the given field with the
	 * type of term values explicitly given.
	 * @param field  Name of field to sort by.  Can be <code>null</code> if
	 *               <code>type</code> is SCORE or DOC.
	 * @param type   Type of values in the terms.
	 * @param reverse True if natural order should be reversed.
	 */
	public AdvancedSortField(String field, Type type, boolean reverse) {
		super(field, type, reverse);
	}
	
	/** 
	 * Creates a sort by terms in the given field, parsed
	 * to numeric values using a custom {@link FieldCache.Parser}.
	 * @param field  Name of field to sort by.  Must not be null.
	 * @param parser Instance of a {@link FieldCache.Parser},
	 *  which must subclass one of the existing numeric
	 *  parsers from {@link FieldCache}. Sort type is inferred
	 *  by testing which numeric parser the parser subclasses.
	 * @throws IllegalArgumentException if the parser fails to
	 *  subclass an existing numeric parser, or field is null
	 */
	public AdvancedSortField(String field, ISortField.Parser parser) {
		super(field, parser);
	}
	
	/** 
	 * Creates a sort, possibly in reverse, by terms in the given field, parsed
	 * to numeric values using a custom {@link FieldCache.Parser}.
	 * @param field  Name of field to sort by.  Must not be null.
	 * @param parser Instance of a {@link FieldCache.Parser},
	 *  which must subclass one of the existing numeric
	 *  parsers from {@link FieldCache}. Sort type is inferred
	 *  by testing which numeric parser the parser subclasses.
	 * @param reverse True if natural order should be reversed.
	 * @throws IllegalArgumentException if the parser fails to
	 *  subclass an existing numeric parser, or field is null
	 */
	public AdvancedSortField(String field, ISortField.Parser parser, boolean reverse) {
		super(field, parser, reverse);
	}
	
	/** 
	 * Creates a sort with a custom comparison function.
	 * @param field Name of field to sort by; cannot be <code>null</code>.
	 * @param comparator Returns a comparator for sorting hits.
	 */
	public AdvancedSortField(String field, FieldComparatorSource comparator) {
		super(field, comparator);
	}
	
	/** 
	 * Creates a sort, possibly in reverse, with a custom comparison function.
	 * @param field Name of field to sort by; cannot be <code>null</code>.
	 * @param comparator Returns a comparator for sorting hits.
	 * @param reverse True if natural order should be reversed.
	 */
	public AdvancedSortField(String field, FieldComparatorSource comparator, boolean reverse) {
		super(field, comparator, reverse);
	}
	
	/** 
	 * Returns the {@link FieldComparator} to use for
	 * sorting.
	 *
	 * @param numHits number of top hits the queue will store
	 * @param sortPos position of this SortField within {@link
	 *   Sort}.  The comparator is primary if sortPos==0,
	 *   secondary if sortPos==1, etc.  Some comparators can
	 *   optimize themselves when they are the primary sort.
	 * @return {@link FieldComparator} to use when sorting
	 */
	@Override
	public IFieldComparator<?> getComparator(final int numHits, 
			final int sortPos) throws IOException {
		switch (mType) {
		case SCORE:
			return new RelevanceComparator(numHits);

		case DOC:
			return new DocComparator(numHits);

		case INT:
			if (mUseIndexValues) {
				return null; //new FieldComparator.IntDocValuesComparator(numHits, field);
			} else {
				return new IntComparator(numHits, 
						mField, mParser, (Integer) mMissingValue);
			}

		case FLOAT:
			if (mUseIndexValues) {
				return null; //new FieldComparator.FloatDocValuesComparator(numHits, field);
			} else {
				return new FloatComparator(numHits, 
						mField, mParser, (Float) mMissingValue);
			}

		case LONG:
			return new LongComparator(numHits, 
					mField, mParser, (Long) mMissingValue);

		case DOUBLE:
			return new DoubleComparator(numHits, 
					mField, mParser, (Double) mMissingValue);

		case BYTE:
			return new ByteComparator(numHits, 
					mField, mParser, (Byte) mMissingValue);

		case SHORT:
			return new ShortComparator(numHits, 
					mField, mParser, (Short) mMissingValue);

		case CUSTOM:
			assert mComparatorSource != null;
			return mComparatorSource.newComparator(
					mField, numHits, sortPos, mReverse);

		case STRING:
			if (mUseIndexValues) {
				return null; //new TermOrdValDocValuesComparator(numHits, mField);
			} else {
				return new TermOrdValComparator(numHits, mField);
			}

		case STRING_VAL:
			if (mUseIndexValues) {
				return null; //new FieldComparator.TermValDocValuesComparator(numHits, field);
			} else {
				return new TermValComparator(numHits, mField);
			}

		case REWRITEABLE:
			throw new IllegalStateException("SortField needs to be rewritten through " + 
					"Sort.rewrite(..) and SortField.rewrite(..)");
        
		default:
			throw new IllegalStateException("Illegal sort type: " + mType);
		}
	}
	
}
