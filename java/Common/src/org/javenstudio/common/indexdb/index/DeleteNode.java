package org.javenstudio.common.indexdb.index;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ITerm;

public class DeleteNode<T> {
	
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<DeleteNode,DeleteNode> sNextUpdater = 
    	AtomicReferenceFieldUpdater.newUpdater(DeleteNode.class, DeleteNode.class, "mNext");
    
    private volatile DeleteNode<?> mNext;
    private final T mItem;

    public DeleteNode(T item) {
    	mItem = item;
    }

    public void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
    	assert false : "sentinel item must never be applied";
    }

    public boolean casNext(DeleteNode<?> cmp, DeleteNode<?> val) {
    	return sNextUpdater.compareAndSet(this, cmp, val);
    }
    
    public final DeleteNode<?> next() { 
    	return mNext; 
    }
    
    public final T getItem() { 
    	return mItem;
    }
    
    public static class QueryArrayDeleteNode extends DeleteNode<IQuery[]> {
    	public QueryArrayDeleteNode(IQuery[] query) {
    		super(query);
    	}

    	@Override
    	public void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
    		for (IQuery query : getItem()) {
    			bufferedDeletes.addQuery(query, docIDUpto);  
    		}
    	}
    }
    
    public static class TermArrayDeleteNode extends DeleteNode<ITerm[]> {
    	public TermArrayDeleteNode(ITerm[] term) {
    		super(term);
    	}

    	@Override
    	public void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
    		for (ITerm term : getItem()) {
    			bufferedDeletes.addTerm(term, docIDUpto);  
    		}
    	}

    	@Override
    	public String toString() {
    		return "dels=" + Arrays.toString(getItem());
    	}
    }
    
    public static class TermDeleteNode extends DeleteNode<ITerm> {
    	public TermDeleteNode(ITerm term) {
    		super(term);
    	}

    	@Override
    	public void apply(BufferedDeletes bufferedDeletes, int docIDUpto) {
    		bufferedDeletes.addTerm(getItem(), docIDUpto);
    	}

    	@Override
    	public String toString() {
    		return "del=" + getItem();
    	}
    }
    
}
