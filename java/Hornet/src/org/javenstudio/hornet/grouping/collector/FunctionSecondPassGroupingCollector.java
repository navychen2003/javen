package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.Collection;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.hornet.grouping.AbstractSecondPassGroupingCollector;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.util.MutableValue;

/**
 * Concrete implementation of {@link AbstractSecondPassGroupingCollector} that groups based on
 * {@link ValueSource} instances.
 *
 */
public class FunctionSecondPassGroupingCollector 
		extends AbstractSecondPassGroupingCollector<MutableValue> {

	private final ValueSource mGroupByVS;
	private final ValueSourceContext mContext;

	private ValueFiller mFiller;
	private MutableValue mMutableValue;

	/**
	 * Constructs a {@link FunctionSecondPassGroupingCollector} instance.
	 *
	 * @param searchGroups The {@link SearchGroup} instances collected during the first phase.
	 * @param groupSort The group sort
	 * @param withinGroupSort The sort inside a group
	 * @param maxDocsPerGroup The maximum number of documents to collect inside a group
	 * @param getScores Whether to include the scores
	 * @param getMaxScores Whether to include the maximum score
	 * @param fillSortFields Whether to fill the sort values in {@link TopGroups#withinGroupSort}
	 * @param groupByVS The {@link ValueSource} to group by
	 * @param vsContext The value source context
	 * @throws IOException IOException When I/O related errors occur
	 */
	public FunctionSecondPassGroupingCollector(Collection<SearchGroup<MutableValue>> searchGroups, 
			ISort groupSort, ISort withinGroupSort, int maxDocsPerGroup, boolean getScores, 
			boolean getMaxScores, boolean fillSortFields, ValueSource groupByVS, 
			ValueSourceContext vsContext) throws IOException {
		super(searchGroups, groupSort, withinGroupSort, maxDocsPerGroup, 
				getScores, getMaxScores, fillSortFields);
		
		mGroupByVS = groupByVS;
		mContext = vsContext;
	}

	@Override
	protected SearchGroupDocs<MutableValue> retrieveGroup(int doc) throws IOException {
		mFiller.fillValue(doc);
		return mGroupMap.get(mMutableValue);
	}

	@Override
	public void setNextReader(IAtomicReaderRef readerContext) throws IOException {
		super.setNextReader(readerContext);
		
		FunctionValues values = mGroupByVS.getValues(mContext, readerContext);
		mFiller = values.getValueFiller();
		mMutableValue = mFiller.getValue();
	}

}
