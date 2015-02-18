package org.javenstudio.common.indexdb.index;

public class DeleteSlice {
	
	// No need to be volatile, slices are thread captive (only accessed by one thread)!
    private DeleteNode<?> mSliceHead; // we don't apply this one
    private DeleteNode<?> mSliceTail;

    public DeleteSlice(DeleteNode<?> currentTail) {
    	assert currentTail != null;
    	/**
    	 * Initially this is a 0 length slice pointing to the 'current' tail of
    	 * the queue. Once we update the slice we only need to assign the tail and
    	 * have a new slice
    	 */
    	mSliceHead = mSliceTail = currentTail;
    }

    public final DeleteNode<?> getSliceHead() { 
    	return mSliceHead;
    }
    
    public void setSliceHead(DeleteNode<?> node) { 
    	mSliceHead = node;
    }
    
    public final DeleteNode<?> getSliceTail() { 
    	return mSliceTail;
    }
    
    public void setSliceTail(DeleteNode<?> node) { 
    	mSliceTail = node;
    }
    
    public void apply(BufferedDeletes del, int docIDUpto) {
    	if (mSliceHead == mSliceTail) {
    		// 0 length slice
    		return;
    	}
    	
    	/**
    	 * When we apply a slice we take the head and get its next as our first
    	 * item to apply and continue until we applied the tail. If the head and
    	 * tail in this slice are not equal then there will be at least one more
    	 * non-null node in the slice!
    	 */
    	DeleteNode<?> current = mSliceHead;
    	do {
    		current = current.next();
    		assert current != null : "slice property violated between the head on the tail must not be a null node";
    		current.apply(del, docIDUpto);
    	} while (current != mSliceTail);
    	
    	reset();
    }

    public void reset() {
    	// Reset to a 0 length slice
    	mSliceHead = mSliceTail;
    }

    /**
     * Returns <code>true</code> iff the given item is identical to the item
     * hold by the slices tail, otherwise <code>false</code>.
     */
    public boolean isTailItem(Object item) {
    	return mSliceTail.getItem() == item;
    }

    public boolean isEmpty() {
    	return mSliceHead == mSliceTail;
    }
    
}
