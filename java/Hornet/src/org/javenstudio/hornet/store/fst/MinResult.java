package org.javenstudio.hornet.store.fst;

import java.util.Comparator;

import org.javenstudio.common.indexdb.util.IntsRef;

/** 
 * Holds a single input (IntsRef) + output, returned by
 *  {@link #shortestPaths shortestPaths()}. 
 */
public final class MinResult<T> implements Comparable<MinResult<T>> {
	
    private final IntsRef mInput;
    private final T mOutput;
    private final Comparator<T> mComparator;
    
    public MinResult(IntsRef input, T output, Comparator<T> comparator) {
    	mInput = input;
    	mOutput = output;
    	mComparator = comparator;
    }

    @Override
    public int compareTo(MinResult<T> other) {
    	int cmp = mComparator.compare(mOutput, other.mOutput);
    	if (cmp == 0) 
    		return mInput.compareTo(other.mInput);
    	else 
    		return cmp;
    }
    
}
