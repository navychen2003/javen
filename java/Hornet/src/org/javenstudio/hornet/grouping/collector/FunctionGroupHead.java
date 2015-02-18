package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IFieldComparator;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.common.indexdb.ISortField;
import org.javenstudio.hornet.grouping.GroupHead;
import org.javenstudio.hornet.util.MutableValue;

/** 
 * Holds current head document for a single group.
 */
public class FunctionGroupHead extends GroupHead<MutableValue> {

	protected final IAtomicReaderRef mReaderContext;
    protected final IFieldComparator<?>[] mComparators;

    public FunctionGroupHead(IAtomicReaderRef readerContext, IScorer scorer, 
    		MutableValue groupValue, ISort sort, int doc) throws IOException {
    	super(groupValue, doc + readerContext.getDocBase());
    	
    	mReaderContext = readerContext;
    	
    	final ISortField[] sortFields = sort.getSortFields();
    	mComparators = new IFieldComparator[sortFields.length];
    	
    	for (int i = 0; i < sortFields.length; i++) {
    		mComparators[i] = sortFields[i].getComparator(1, i).setNextReader(readerContext);
    		mComparators[i].setScorer(scorer);
    		mComparators[i].copy(0, doc);
    		mComparators[i].setBottom(0);
    	}
    }

    public final IFieldComparator<?>[] getComparators() { 
    	return mComparators; 
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
    	
    	mDoc = doc + mReaderContext.getDocBase();
    }
	
}
