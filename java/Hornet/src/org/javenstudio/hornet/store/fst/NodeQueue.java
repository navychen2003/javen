package org.javenstudio.hornet.store.fst;

import org.javenstudio.common.indexdb.util.PriorityQueue;

final class NodeQueue extends PriorityQueue<NodeAndInCount> {
	
	public NodeQueue(int topN) {
		super(topN, false);
	}

	@Override
	public boolean lessThan(NodeAndInCount a, NodeAndInCount b) {
		final int cmp = a.compareTo(b);
		assert cmp != 0;
		return cmp < 0;
	}
	
}
