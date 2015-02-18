package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.grouping.AbstractAllGroupHeadsCollector;
import org.javenstudio.hornet.grouping.GroupHead;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.util.MutableValue;

/**
 * An implementation of {@link AbstractAllGroupHeadsCollector} for retrieving 
 * the most relevant groups when grouping
 * by {@link ValueSource}.
 *
 */
public class FunctionAllGroupHeadsCollector 
		extends AbstractAllGroupHeadsCollector<GroupHead<?>> {

	private final ValueSource mGroupBy;
	private final ValueSourceContext mContext;
	private final Map<MutableValue, GroupHead<?>> mGroups;
	private final ISort mSortWithinGroup;

	private ValueFiller mFiller;
	private MutableValue mMutableValue;
	private IAtomicReaderRef mReaderContext;
	private IScorer mScorer;

	/**
	 * Constructs a {@link FunctionAllGroupHeadsCollector} instance.
	 *
	 * @param groupBy The {@link ValueSource} to group by
	 * @param vsContext The ValueSource context
	 * @param sortWithinGroup The sort within a group
	 */
	public FunctionAllGroupHeadsCollector(ValueSource groupBy, 
			ValueSourceContext vsContext, ISort sortWithinGroup) {
		super(sortWithinGroup.getSortFields().length);
		
		mGroups = new HashMap<MutableValue, GroupHead<?>>();
		mSortWithinGroup = sortWithinGroup;
		mGroupBy = groupBy;
		mContext = vsContext;

		final ISortField[] sortFields = sortWithinGroup.getSortFields();
		for (int i = 0; i < sortFields.length; i++) {
			mReversed[i] = sortFields[i].getReverse() ? -1 : 1;
		}
	}

	@Override
	protected void retrieveGroupHeadAndAddIfNotExist(int doc) throws IOException {
		mFiller.fillValue(doc);
		
		FunctionGroupHead groupHead = (FunctionGroupHead)mGroups.get(mMutableValue);
		if (groupHead == null) {
			MutableValue groupValue = mMutableValue.duplicate();
			groupHead = new FunctionGroupHead(mReaderContext, mScorer, 
					groupValue, mSortWithinGroup, doc);
			
			mGroups.put(groupValue, groupHead);
			mTemporalResult.setStop(true);
			
		} else {
			mTemporalResult.setStop(false);
		}
		
		mTemporalResult.setGroupHead(groupHead);
	}

	@Override
	protected Collection<GroupHead<?>> getCollectedGroupHeads() {
		return mGroups.values();
	}

	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mScorer = scorer;
		for (GroupHead<?> groupHead : mGroups.values()) {
			FunctionGroupHead funcHead = (FunctionGroupHead)groupHead;
			for (IFieldComparator<?> comparator : funcHead.getComparators()) {
				comparator.setScorer(scorer);
			}
		}
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mReaderContext = context;
		
		FunctionValues values = mGroupBy.getValues(mContext, context);
		mFiller = values.getValueFiller();
		mMutableValue = mFiller.getValue();

		for (GroupHead<?> groupHead : mGroups.values()) {
			FunctionGroupHead funcHead = (FunctionGroupHead)groupHead;
			for (int i = 0; i < funcHead.mComparators.length; i++) {
				funcHead.mComparators[i] = funcHead.mComparators[i].setNextReader(context);
			}
		}
	}

}
