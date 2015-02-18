package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.hornet.grouping.AbstractFirstPassGroupingCollector;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.util.MutableValue;

/**
 * Concrete implementation of {@link AbstractFirstPassGroupingCollector} that groups based on
 * {@link ValueSource} instances.
 *
 */
public class FunctionFirstPassGroupingCollector 
		extends AbstractFirstPassGroupingCollector<MutableValue> {

	private final ValueSource mGroupByVS;
	private final ValueSourceContext mContext;

	private ValueFiller mFiller;
	private MutableValue mMutableValue;

	/**
	 * Creates a first pass collector.
	 *
	 * @param groupByVS  The {@link ValueSource} instance to group by
	 * @param vsContext  The ValueSource context
	 * @param groupSort  The {@link Sort} used to sort the
	 *                   groups.  The top sorted document within each group
	 *                   according to groupSort, determines how that group
	 *                   sorts against other groups.  This must be non-null,
	 *                   ie, if you want to groupSort by relevance use
	 *                   Sort.RELEVANCE.
	 * @param topNGroups How many top groups to keep.
	 * @throws IOException When I/O related errors occur
	 */
	public FunctionFirstPassGroupingCollector(ValueSource groupByVS, 
			ValueSourceContext vsContext, ISort groupSort, int topNGroups) throws IOException {
		super(groupSort, topNGroups);
		
		mGroupByVS = groupByVS;
		mContext = vsContext;
	}

	@Override
	protected MutableValue getDocGroupValue(int doc) {
		mFiller.fillValue(doc);
		return mMutableValue;
	}

	@Override
	protected MutableValue copyDocGroupValue(MutableValue groupValue, MutableValue reuse) {
		if (reuse != null) {
			reuse.copy(groupValue);
			return reuse;
		}
		
		return groupValue.duplicate();
	}

	@Override
	public void setNextReader(IAtomicReaderRef readerContext) throws IOException {
		super.setNextReader(readerContext);
		
		FunctionValues values = mGroupByVS.getValues(mContext, readerContext);
		mFiller = values.getValueFiller();
		mMutableValue = mFiller.getValue();
	}

}
