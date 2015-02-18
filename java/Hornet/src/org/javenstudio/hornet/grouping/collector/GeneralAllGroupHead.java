package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.GroupHead;

public class GeneralAllGroupHead extends GroupHead<BytesRef> {
	
	protected final GeneralAllGroupHeadsCollector mCollector;
    protected final IFieldComparator<?>[] mComparators;

    public GeneralAllGroupHead(GeneralAllGroupHeadsCollector collector, 
    		BytesRef groupValue, ISort sort, int doc) throws IOException {
    	super(groupValue, doc + collector.getReaderContext().getDocBase());
    	
    	mCollector = collector;
    	
    	final ISortField[] sortFields = sort.getSortFields();
    	mComparators = new IFieldComparator[sortFields.length];
    	
    	for (int i = 0; i < sortFields.length; i++) {
    		mComparators[i] = sortFields[i].getComparator(1, i)
    				.setNextReader(collector.getReaderContext());
    		mComparators[i].setScorer(collector.mScorer);
    		mComparators[i].copy(0, doc);
    		mComparators[i].setBottom(0);
    	}
    }

    @Override
    public int compare(int compIDX, int doc) throws IOException {
    	return mComparators[compIDX].compareBottom(doc);
    }

    @Override
    public void updateDocHead(int doc) throws IOException {
    	for (IFieldComparator<?> comparator : mComparators) {
    		comparator.copy(0, doc);
    		comparator.setBottom(0);
    	}
    	
    	mDoc = doc + mCollector.getReaderContext().getDocBase();
    }
    
}
