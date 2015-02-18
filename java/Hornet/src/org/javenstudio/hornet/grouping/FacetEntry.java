package org.javenstudio.hornet.grouping;

import org.javenstudio.common.indexdb.util.BytesRef;

/**
 * Represents a facet entry with a value and a count.
 */
public class FacetEntry {
	
    private final BytesRef mValue;
    private final int mCount;

    public FacetEntry(BytesRef value, int count) {
    	mValue = value;
    	mCount = count;
    }

    @Override
    public boolean equals(Object o) {
    	if (this == o) return true;
    	if (o == null || getClass() != o.getClass()) 
    		return false;

    	FacetEntry that = (FacetEntry) o;

    	if (this.mCount != that.mCount) 
    		return false;
    	
    	if (!mValue.equals(that.mValue)) 
    		return false;

    	return true;
    }

    @Override
    public int hashCode() {
    	int result = mValue.hashCode();
    	result = 31 * result + mCount;
    	return result;
    }

    /**
     * @return The value of this facet entry
     */
    public BytesRef getValue() {
    	return mValue;
    }

    /**
     * @return The count (number of groups) of this facet entry.
     */
    public int getCount() {
    	return mCount;
    }
    
    @Override
    public String toString() {
    	return "FacetEntry{" +
    			"value=" + mValue.utf8ToString() +
    			",count=" + mCount +
    			'}';
    }
    
}
