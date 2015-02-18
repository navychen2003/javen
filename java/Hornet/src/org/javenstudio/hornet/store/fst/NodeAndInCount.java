package org.javenstudio.hornet.store.fst;

final class NodeAndInCount implements Comparable<NodeAndInCount> {
	
    private final int mNode;
    private final int mCount;

    public NodeAndInCount(int node, int count) {
    	mNode = node;
    	mCount = count;
    }
    
    @Override
    public int compareTo(NodeAndInCount other) {
    	if (mCount > other.mCount) {
    		return 1;
    	} else if (mCount < other.mCount) {
    		return -1;
    	} else {
    		// Tie-break: smaller node compares as greater than
    		return other.mNode - mNode;
    	}
    }
    
    public final int getNode() { return mNode; }
    public final int getCount() { return mCount; }
    
}
