package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.grouping.AbstractAllGroupsCollector;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.util.MutableValue;

/**
 * A collector that collects all groups that match the
 * query. Only the group value is collected, and the order
 * is undefined.  This collector does not determine
 * the most relevant document of a group.
 *
 * <p/>
 * Implementation detail: Uses {@link ValueSource} and {@link FunctionValues} to retrieve the
 * field values to group by.
 *
 */
public class FunctionAllGroupsCollector extends AbstractAllGroupsCollector<MutableValue> {

	private final SortedSet<MutableValue> mGroups = new TreeSet<MutableValue>();
	private final ValueSourceContext mContext;
	private final ValueSource mGroupBy;
	
	private ValueFiller mFiller;
	private MutableValue mMutableValue;

	/**
	 * Constructs a {@link FunctionAllGroupsCollector} instance.
	 *
	 * @param groupBy The {@link ValueSource} to group by
	 * @param vsContext The ValueSource context
	 */
	public FunctionAllGroupsCollector(ValueSource groupBy, ValueSourceContext vsContext) {
		mContext = vsContext;
		mGroupBy = groupBy;
	}

	@Override
	public Collection<MutableValue> getGroups() {
		return mGroups;
	}

	@Override
	public void collect(int doc) throws IOException {
		mFiller.fillValue(doc);
		if (!mGroups.contains(mMutableValue)) 
			mGroups.add(mMutableValue.duplicate());
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		FunctionValues values = mGroupBy.getValues(mContext, context);
		mFiller = values.getValueFiller();
		mMutableValue = mFiller.getValue();
	}
	
}
