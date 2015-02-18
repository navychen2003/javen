package org.javenstudio.hornet.grouping.collector;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.index.term.DocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.GroupHead;

/**
 * A base implementation of {@link AbstractAllGroupHeadsCollector} 
 * for retrieving the most relevant groups when grouping
 * on a string based group field. 
 * More specifically this all concrete implementations of this base implementation
 * use {@link DocTermsIndex}.
 *
 */
public abstract class TermAllGroupHeadsCollector<GH extends GroupHead<?>> 
		extends AbstractAllGroupHeadsCollector<GH> {

	protected static final int DEFAULT_INITIAL_SIZE = 128;

	protected final BytesRef mScratchBytesRef = new BytesRef();
	protected final String mGroupField;
	
	protected IDocTermsIndex mGroupIndex;
	protected IAtomicReaderRef mReaderContext;

	protected TermAllGroupHeadsCollector(String groupField, int numberOfSorts) {
		super(numberOfSorts);
		mGroupField = groupField;
	}

	public final IAtomicReaderRef getReaderContext() { 
		return mReaderContext;
	}
	
	/**
	 * Creates an <code>AbstractAllGroupHeadsCollector</code> instance 
	 * based on the supplied arguments.
	 * This factory method decides with implementation is best suited.
	 *
	 * Delegates to {@link #create(String, Sort, int)} with an initialSize of 128.
	 *
	 * @param groupField      The field to group by
	 * @param sortWithinGroup The sort within each group
	 * @return an <code>AbstractAllGroupHeadsCollector</code> instance 
	 * based on the supplied arguments
	 */
	public static AbstractAllGroupHeadsCollector<?> create(String groupField, 
			ISort sortWithinGroup) {
		return create(groupField, sortWithinGroup, DEFAULT_INITIAL_SIZE);
	}

	/**
	 * Creates an <code>AbstractAllGroupHeadsCollector</code> instance 
	 * based on the supplied arguments.
	 * This factory method decides with implementation is best suited.
	 *
	 * @param groupField      The field to group by
	 * @param sortWithinGroup The sort within each group
	 * @param initialSize The initial allocation size of the internal int set 
	 * and group list which should roughly match
	 * the total number of expected unique groups. Be aware that the heap usage is
	 * 4 bytes * initialSize.
	 * @return an <code>AbstractAllGroupHeadsCollector</code> instance 
	 * based on the supplied arguments
	 */
	public static AbstractAllGroupHeadsCollector<?> create(String groupField, 
			ISort sortWithinGroup, int initialSize) {
		boolean sortAllScore = true;
		boolean sortAllFieldValue = true;

		for (ISortField sortField : sortWithinGroup.getSortFields()) {
			if (sortField.getType() == ISortField.Type.SCORE) {
				sortAllFieldValue = false;
				
			} else if (needGeneralImpl(sortField)) {
				return new GeneralAllGroupHeadsCollector(groupField, sortWithinGroup);
				
			} else {
				sortAllScore = false;
			}
		}

		if (sortAllScore) {
			return new ScoreAllGroupHeadsCollector(
					groupField, sortWithinGroup, initialSize);
			
		} else if (sortAllFieldValue) {
			return new OrdAllGroupHeadsCollector(
					groupField, sortWithinGroup, initialSize);
			
		} else {
			return new OrdScoreAllGroupHeadsCollector(
					groupField, sortWithinGroup, initialSize);
		}
	}

	// Returns when a sort field needs the general impl.
	private static boolean needGeneralImpl(ISortField sortField) {
		ISortField.Type sortType = sortField.getType();
		// Note (MvG): We can also make an optimized impl when sorting is SortField.DOC
		return  sortType != ISortField.Type.STRING_VAL && 
				sortType != ISortField.Type.STRING && 
				sortType != ISortField.Type.SCORE;
	}

}
