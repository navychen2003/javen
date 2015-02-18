package org.javenstudio.hornet.grouping;

import java.util.HashSet;
import java.util.Set;

/**
 * Returned by {@link AbstractDistinctValuesCollector#getGroups()},
 * representing the value and set of distinct values for the group.
 */
public abstract class GroupCount<GT> {

    private final GT mGroupValue;
    private final Set<GT> mUniqueValues;

    public GroupCount(GT groupValue) {
    	mGroupValue = groupValue;
    	mUniqueValues = new HashSet<GT>();
    }
	
    public GT getGroupValue() { return mGroupValue; }
    public Set<GT> getUniqueValues() { return mUniqueValues; }
    
}
