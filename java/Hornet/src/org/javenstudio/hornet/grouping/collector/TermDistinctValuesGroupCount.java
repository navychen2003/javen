package org.javenstudio.hornet.grouping.collector;

import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.GroupCount;

/** 
 * Holds distinct values for a single group.
 */
public class TermDistinctValuesGroupCount extends GroupCount<BytesRef> {
	
    protected int[] mOrds;

    public TermDistinctValuesGroupCount(BytesRef groupValue) {
    	super(groupValue);
    }
    
    public final int[] getOrds() { return mOrds; }
    
}
