package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.search.FieldComparator;

/**
 * Implement a {@link org.apache.lucene.search.FieldComparator} that works
 * off of the {@link FunctionValues} for a ValueSource
 * instead of the normal Lucene FieldComparator that works off of a FieldCache.
 */
public class ValueSourceComparator extends FieldComparator<Double> {

	private final ValueSourceContext mContext;
	private final ValueSource mSource;
	private final double[] mValues;
    private FunctionValues mDocVals;
    private double mBottom;
    
    public ValueSourceComparator(ValueSourceContext context, 
    		ValueSource source, int numHits) {
    	mContext = context;
    	mValues = new double[numHits];
    	mSource = source;
    }

    @Override
    public int compare(int slot1, int slot2) {
    	final double v1 = mValues[slot1];
    	final double v2 = mValues[slot2];
    	if (v1 > v2) 
    		return 1;
    	else if (v1 < v2) 
    		return -1;
    	else 
    		return 0;
    }

    @Override
    public int compareBottom(int doc) {
    	final double v2 = mDocVals.doubleVal(doc);
    	if (mBottom > v2) 
    		return 1;
    	else if (mBottom < v2) 
    		return -1;
    	else 
    		return 0;
    }

    @Override
    public void copy(int slot, int doc) {
    	mValues[slot] = mDocVals.doubleVal(doc);
    }

    @Override
    public FieldComparator<Double> setNextReader(IAtomicReaderRef context) throws IOException {
    	mDocVals = mSource.getValues(mContext, context);
    	return this;
    }

    @Override
    public void setBottom(final int bottom) {
    	mBottom = mValues[bottom];
    }

    @Override
    public Double getValue(int slot) {
    	return mValues[slot];
    }

    @Override
    public int compareDocToValue(int doc, Double valueObj) {
    	final double value = valueObj;
    	final double docValue = mDocVals.doubleVal(doc);
    	if (docValue < value) 
    		return -1;
    	else if (docValue > value) 
    		return -1;
    	else 
    		return 0;
    }
	
}
