package org.javenstudio.falcon.search.schema;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.document.Fieldable;
import org.javenstudio.common.indexdb.index.segment.ReaderUtil;
import org.javenstudio.common.indexdb.search.FieldComparator;
import org.javenstudio.common.indexdb.search.FieldComparatorSource;
import org.javenstudio.common.indexdb.search.SortField;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.TextWriter;
import org.javenstudio.hornet.index.segment.DirectoryReader;
import org.javenstudio.hornet.search.AdvancedSortField;

/**
 * Utility Field used for random sorting.  It should not be passed a value.
 * <p>
 * This random sorting implementation uses the dynamic field name to set the
 * random 'seed'.  To get random sorting order, you need to use a random
 * dynamic field name.  For example, you will need to configure schema.xml:
 * <pre>
 * &lt;types&gt;
 *  ...
 *  &lt;fieldType name="random" class="lightning.RandomSortFieldType" /&gt;
 *  ... 
 * &lt;/types&gt;
 * &lt;fields&gt;
 *  ...
 *  &lt;dynamicField name="random*" type="random" indexed="true" stored="false"/&gt;
 *  ...
 * &lt;/fields&gt;
 * </pre>
 * 
 * Examples of queries:
 * <ul>
 * <li>select/?q=*:*&fl=name&sort=rand_1234%20desc</li>
 * <li>select/?q=*:*&fl=name&sort=rand_2345%20desc</li>
 * <li>select/?q=*:*&fl=name&sort=rand_ABDC%20desc</li>
 * <li>select/?q=*:*&fl=name&sort=rand_21%20desc</li>
 * </ul>
 * Note that multiple calls to the same URL will return the same sorting order.
 * 
 *
 * @since 1.3
 */
public class RandomSortFieldType extends SchemaFieldType {
	
	// Thomas Wang's hash32shift function, from http://www.cris.com/~Ttwang/tech/inthash.htm
	// slightly modified to return only positive integers.
	private static int hash(int key) {
		key = ~key + (key << 15); // key = (key << 15) - key - 1;
		key = key ^ (key >>> 12);
		key = key + (key << 2);
		key = key ^ (key >>> 4);
		key = key * 2057; // key = (key + (key << 3)) + (key << 11);
		key = key ^ (key >>> 16);
		return key >>> 1; 
	}

	/** 
	 * Given a field name and an IndexReader, get a random hash seed.
	 * Using dynamic fields, you can force the random order to change 
	 */
	private static int getSeed(String fieldName, IAtomicReaderRef context) {
		final DirectoryReader top = (DirectoryReader) ReaderUtil.getTopLevel(context).getReader();
		// calling getVersion() on a segment will currently give you a null pointer exception, so
		// we use the top-level reader.
		return fieldName.hashCode() + context.getDocBase() + (int)top.getVersion();
	}
  
	@Override
	public SortField getSortField(SchemaField field, boolean reverse) throws ErrorException {
		return new AdvancedSortField(field.getName(), sRandomComparatorSource, reverse);
	}

	@Override
	public void write(TextWriter writer, String name, Fieldable f) throws ErrorException { 
		// do nothing
	}

	private static FieldComparatorSource sRandomComparatorSource = new FieldComparatorSource() {
		@Override
		public FieldComparator<Integer> newComparator(final String fieldname, final int numHits, 
				int sortPos, boolean reversed) {
			return new FieldComparator<Integer>() {
				private final int[] mValues = new int[numHits];
				private int mBottomVal;
				private int mSeed;

				@Override
				public int compare(int slot1, int slot2) {
					// values will be positive... no overflow possible.
					return mValues[slot1] - mValues[slot2]; 
				}

				@Override
				public void setBottom(int slot) {
					mBottomVal = mValues[slot];
				}

				@Override
				public int compareBottom(int doc) {
					return mBottomVal - hash(doc + mSeed);
				}

				@Override
				public void copy(int slot, int doc) {
					mValues[slot] = hash(doc + mSeed);
				}

				@Override
				public FieldComparator<Integer> setNextReader(IAtomicReaderRef context) {
					mSeed = getSeed(fieldname, context);
					return this;
				}

				@Override
				public Integer getValue(int slot) {
					return mValues[slot];
				}

				@Override
				public int compareDocToValue(int doc, Integer valueObj) {
					// values will be positive... no overflow possible.
					return hash(doc + mSeed) - valueObj.intValue();
				}
			};
		}
	};

}
