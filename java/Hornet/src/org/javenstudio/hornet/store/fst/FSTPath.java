package org.javenstudio.hornet.store.fst;

import java.util.Comparator;

import org.javenstudio.common.indexdb.util.IntsRef;

final class FSTPath<T> implements Comparable<FSTPath<T>> {
	
	private final IntsRef mInput = new IntsRef();
    private final Comparator<T> mComparator;
    private FSTArc<T> mArc;
	private T mCost;

    public FSTPath(T cost, FSTArc<T> arc, Comparator<T> comparator) {
    	mArc = new FSTArc<T>().copyFrom(arc);
    	mCost = cost;
    	mComparator = comparator;
    }

    @Override
    public String toString() {
    	return "FSTPath: input=" + mInput + " cost=" + mCost;
    }

    @Override
    public int compareTo(FSTPath<T> other) {
    	int cmp = mComparator.compare(mCost, other.mCost);
    	if (cmp == 0) 
    		return mInput.compareTo(other.mInput);
    	else 
    		return cmp;
    }
    
    public final IntsRef getInput() { return mInput; }
    public final FSTArc<T> getArc() { return mArc; }
    public final T getCost() { return mCost; }
    
    final void setCost(T cost) { mCost = cost; }
    
}
