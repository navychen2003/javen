package org.javenstudio.common.indexdb.index;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IMergeOne;

/**
 * A MergeSpecification instance provides the information
 * necessary to perform multiple merges.  It simply
 * contains a list of {@link OneMerge} instances.
 */
public class MergeSpecification {
	
    /**
     * The subset of segments to be included in the primitive merge.
     */
    private final List<IMergeOne> mMerges = new ArrayList<IMergeOne>();

    public void add(IMergeOne merge) {
    	mMerges.add(merge);
    }
    
    public final int getMergeCount() { return mMerges.size(); }
    public final IMergeOne getMerge(int index) { return mMerges.get(index); }
    
}
