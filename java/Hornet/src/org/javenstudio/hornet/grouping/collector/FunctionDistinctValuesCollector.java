package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.hornet.grouping.AbstractDistinctValuesCollector;
import org.javenstudio.hornet.grouping.GroupCount;
import org.javenstudio.hornet.grouping.SearchGroup;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueFiller;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.hornet.util.MutableValue;

/**
 * Function based implementation of {@link AbstractDistinctValuesCollector}.
 *
 */
public class FunctionDistinctValuesCollector 
		extends AbstractDistinctValuesCollector<GroupCount<?>> {

	private final Map<MutableValue, GroupCount<?>> mGroupMap;
	private final ValueSourceContext mContext;
	private final ValueSource mGroupSource;
	private final ValueSource mCountSource;
	
	private ValueFiller mGroupFiller;
	private ValueFiller mCountFiller;
	private MutableValue mGroupMval;
	private MutableValue mCountMval;

	public FunctionDistinctValuesCollector(ValueSourceContext vsContext, 
			ValueSource groupSource, ValueSource countSource, 
			Collection<SearchGroup<MutableValue>> groups) {
		mContext = vsContext;
		mGroupSource = groupSource;
		mCountSource = countSource;
		mGroupMap = new LinkedHashMap<MutableValue, GroupCount<?>>();
		
		for (SearchGroup<MutableValue> group : groups) {
			mGroupMap.put(group.getGroupValue(), 
					new FunctionGroupCount(group.getGroupValue()));
		}
	}

	@Override
	public List<GroupCount<?>> getGroups() {
		return new ArrayList<GroupCount<?>>(mGroupMap.values());
	}

	public void collect(int doc) throws IOException {
		mGroupFiller.fillValue(doc);
		
		FunctionGroupCount groupCount = (FunctionGroupCount)mGroupMap.get(mGroupMval);
		if (groupCount != null) {
			mCountFiller.fillValue(doc);
			groupCount.getUniqueValues().add(mCountMval.duplicate());
		}
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		FunctionValues values = mGroupSource.getValues(mContext, context);
		mGroupFiller = values.getValueFiller();
		mGroupMval = mGroupFiller.getValue();
		
		values = mCountSource.getValues(mContext, context);
		mCountFiller = values.getValueFiller();
		mCountMval = mCountFiller.getValue();
	}

}
