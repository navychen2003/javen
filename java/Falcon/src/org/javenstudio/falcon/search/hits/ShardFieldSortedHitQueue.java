package org.javenstudio.falcon.search.hits;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.javenstudio.common.indexdb.IFieldComparatorSource;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.PriorityQueue;
import org.javenstudio.hornet.search.AdvancedSortField;
import org.javenstudio.falcon.search.comparator.MissingStringLastSource;

//used by distributed search to merge results.
public class ShardFieldSortedHitQueue extends PriorityQueue<ShardDoc> {
	
	/** 
	 * The order of these fieldNames should correspond to the order of 
	 * sort field values retrieved from the shard 
	 */
	protected List<String> mFieldNames = new ArrayList<String>();
	
	/** Stores a comparator corresponding to each field being sorted by */
	protected Comparator<Object>[] mComparators;

	/** Stores the sort criteria being used. */
	protected ISortField[] mFields;

	@SuppressWarnings("unchecked")
	public ShardFieldSortedHitQueue(ISortField[] fields, int size) {
		super(size);
		
		final int n = fields.length;
		mComparators = new Comparator[n];
		mFields = new ISortField[n];
		
		for (int i = 0; i < n; ++i) {
			// keep track of the named fields
			ISortField.Type type = fields[i].getType();
			if (type != ISortField.Type.SCORE && type != ISortField.Type.DOC) 
				mFieldNames.add(fields[i].getField());
			
			String fieldname = fields[i].getField();
			mComparators[i] = getCachedComparator(fieldname, 
					fields[i].getType(), fields[i].getComparatorSource());

			if (fields[i].getType() == ISortField.Type.STRING) {
				mFields[i] = new AdvancedSortField(fieldname, ISortField.Type.STRING,
						fields[i].getReverse());
			} else {
				mFields[i] = new AdvancedSortField(fieldname, fields[i].getType(),
						fields[i].getReverse());
			}
		}
	}

	@Override
	protected boolean lessThan(ShardDoc docA, ShardDoc docB) {
		// If these docs are from the same shard, then the relative order
		// is how they appeared in the response from that shard.    
		if (docA.getShard() == docB.getShard()) {
			// if docA has a smaller position, it should be "larger" so it
			// comes before docB.
			// This will handle sorting by docid within the same shard

			// comment this out to test comparators.
			return !(docA.getOrderInShard() < docB.getOrderInShard());
		}

		// run comparators
		final int n = mComparators.length;
		int c = 0;
		
		for (int i = 0; i < n && c == 0; i++) {
			c = (mFields[i].getReverse()) ? mComparators[i].compare(docB, docA)
					: mComparators[i].compare(docA, docB);
		}

		// solve tiebreaks by comparing shards (similar to using docid)
		// smaller docid's beat larger ids, so reverse the natural ordering
		if (c == 0) 
			c = -docA.getShard().compareTo(docB.getShard());
		
		return c < 0;
	}

	protected Comparator<Object> getCachedComparator(String fieldname, ISortField.Type type, 
			IFieldComparatorSource factory) {
		Comparator<Object> comparator = null;
		switch (type) {
		case SCORE:
			comparator = comparatorScore(fieldname);
			break;
		case STRING:
			comparator = comparatorNatural(fieldname);
			break;
		case CUSTOM:
			if (factory instanceof MissingStringLastSource){
				comparator = comparatorMissingStringLast(fieldname);
			} else {
				// TODO: support other types such as random... is there a way to
				// support generically?  Perhaps just comparing Object
				comparator = comparatorNatural(fieldname);
				// throw new RuntimeException("Custom sort not supported factory is "+factory.getClass());
			}
			break;
		case DOC:
			// TODO: we can support this!
			throw new RuntimeException("Doc sort not supported");
		default:
			comparator = comparatorNatural(fieldname);
			break;
		}
		
		return comparator;
	}

	class ShardComparator implements Comparator<Object> {
		private String mFieldName;
		private int mFieldNum;
		
		public ShardComparator(String fieldName) {
			mFieldName = fieldName;
			mFieldNum = 0;
			
			for (int i=0; i < mFieldNames.size(); i++) {
				if (mFieldNames.get(i).equals(fieldName)) {
					mFieldNum = i;
					break;
				}
			}
		}

		Object sortVal(ShardDoc shardDoc) {
			assert(shardDoc.getSortFieldValues().getName(mFieldNum).equals(mFieldName));
			List<?> lst = (List<?>)shardDoc.getSortFieldValues().getVal(mFieldNum);
			return lst.get(shardDoc.getOrderInShard());
		}

		@Override
		public int compare(Object o1, Object o2) {
			return 0;
		}
	}

	static Comparator<Object> comparatorScore(final String fieldName) {
		return new Comparator<Object>() {
			public final int compare(final Object o1, final Object o2) {
				ShardDoc e1 = (ShardDoc) o1;
				ShardDoc e2 = (ShardDoc) o2;

				final float f1 = e1.getScore();
				final float f2 = e2.getScore();
				
				if (f1 < f2)
					return -1;
				if (f1 > f2)
					return 1;
				
				return 0;
			}
		};
	}

	// The natural sort ordering corresponds to numeric
	// and string natural sort orderings (ascending).  Since
	// the PriorityQueue keeps the biggest elements by default,
	// we need to reverse the natural compare ordering so that the
	// smallest elements are kept instead of the largest... hence
	// the negative sign on the final compareTo().
	protected Comparator<Object> comparatorNatural(String fieldName) {
		return new ShardComparator(fieldName) {
			@SuppressWarnings("unchecked")
			@Override
			public final int compare(final Object o1, final Object o2) {
				ShardDoc sd1 = (ShardDoc) o1;
				ShardDoc sd2 = (ShardDoc) o2;
				
				Comparable<Object> v1 = (Comparable<Object>)sortVal(sd1);
				Comparable<Object> v2 = (Comparable<Object>)sortVal(sd2);
				
				if (v1 == v2)
					return 0;
				if (v1 == null)
					return 1;
				if (v2 == null)
					return -1;
				
				return -v1.compareTo(v2);
			}
		};
	}

	protected Comparator<Object> comparatorStringLocale(final String fieldName, Locale locale) {
		final Collator collator = Collator.getInstance(locale);
		return new ShardComparator(fieldName) {
			@SuppressWarnings("unchecked")
			@Override
			public final int compare(final Object o1, final Object o2) {
				ShardDoc sd1 = (ShardDoc) o1;
				ShardDoc sd2 = (ShardDoc) o2;
				
				Comparable<Object> v1 = (Comparable<Object>)sortVal(sd1);
				Comparable<Object> v2 = (Comparable<Object>)sortVal(sd2);
				
				if (v1 == v2)
					return 0;
				if (v1 == null)
					return 1;
				if (v2 == null)
					return -1;
				
				return -collator.compare(v1,v2);
			}
		};
	}

	protected Comparator<Object> comparatorMissingStringLast(final String fieldName) {
		return new ShardComparator(fieldName) {
			@SuppressWarnings("unchecked")
			@Override
			public final int compare(final Object o1, final Object o2) {
				ShardDoc sd1 = (ShardDoc) o1;
				ShardDoc sd2 = (ShardDoc) o2;
				
				Comparable<Object> v1 = (Comparable<Object>)sortVal(sd1);
				Comparable<Object> v2 = (Comparable<Object>)sortVal(sd2);
				
				if (v1 == v2)
					return 0;
				if (v1 == null)
					return -1;
				if (v2 == null)
					return 1;
				
				return -v1.compareTo(v2);
			}
		};
	}

}
