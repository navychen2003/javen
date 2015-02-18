package org.javenstudio.hornet.store.fst;

import java.io.IOException;

// Used to dedup states (lookup already-frozen states)
final class NodeHash<T> {

	private final FSTArc<T> mScratchArc = new FSTArc<T>();
	private final FST<T> mFst;
	
	private int[] mTable;
	private int mCount;
	private int mMask;
	
	public NodeHash(FST<T> fst) {
		mTable = new int[16];
		mMask = 15;
		mFst = fst;
	}

	private boolean nodesEqual(UnCompiledNode<T> node, int address, 
			BytesReader in) throws IOException {
		mFst.readFirstRealTargetArc(address, mScratchArc, in);
		if (mScratchArc.getBytesPerArc() != 0 && node.getNumArcs() != mScratchArc.getNumArcs()) 
			return false;
		
		for (int arcUpto=0; arcUpto < node.getNumArcs(); arcUpto++) {
			final BuilderArc<T> arc = node.getArcAt(arcUpto);
			
			if (arc.getLabel() != mScratchArc.getLabel() ||
				!arc.getOutput().equals(mScratchArc.getOutput()) ||
				((CompiledNode) arc.getTarget()).getNode() != mScratchArc.getTarget() ||
				!arc.getNextFinalOutput().equals(mScratchArc.getNextFinalOutput()) ||
				arc.isFinal() != mScratchArc.isFinal()) {
				return false;
			}

			if (mScratchArc.isLast()) {
				if (arcUpto == node.getNumArcs()-1) 
					return true;
				else 
					return false;
			}
			mFst.readNextRealArc(mScratchArc, in);
		}

		return false;
	}

	// hash code for an unfrozen node.  This must be identical
	// to the un-frozen case (below)!!
	private int hash(UnCompiledNode<T> node) {
		final int PRIME = 31;
		int h = 0;
		
		// TODO: maybe if number of arcs is high we can safely subsample?
		for (int arcIdx=0; arcIdx < node.getNumArcs(); arcIdx++) {
			final BuilderArc<T> arc = node.getArcAt(arcIdx);
			
			h = PRIME * h + arc.getLabel();
			h = PRIME * h + ((CompiledNode) arc.getTarget()).getNode();
			h = PRIME * h + arc.getOutput().hashCode();
			h = PRIME * h + arc.getNextFinalOutput().hashCode();
			
			if (arc.isFinal()) 
				h += 17;
		}
		
		return h & Integer.MAX_VALUE;
	}

	// hash code for a frozen node
	private int hash(int node) throws IOException {
		final int PRIME = 31;
		final BytesReader in = mFst.getBytesReader(0);
		
		int h = 0;
		mFst.readFirstRealTargetArc(node, mScratchArc, in);
		
		while (true) {
			h = PRIME * h + mScratchArc.getLabel();
			h = PRIME * h + mScratchArc.getTarget();
			h = PRIME * h + mScratchArc.getOutput().hashCode();
			h = PRIME * h + mScratchArc.getNextFinalOutput().hashCode();
			
			if (mScratchArc.isFinal()) 
				h += 17;
			
			if (mScratchArc.isLast()) 
				break;
			
			mFst.readNextRealArc(mScratchArc, in);
		}
		
		return h & Integer.MAX_VALUE;
	}

	public int add(UnCompiledNode<T> nodeIn) throws IOException {
		final BytesReader in = mFst.getBytesReader(0);
		final int h = hash(nodeIn);
		
		int pos = h & mMask;
		int c = 0;
		
		while (true) {
			final int v = mTable[pos];
			if (v == 0) {
				// freeze & add
				final int node = mFst.addNode(nodeIn);
				assert hash(node) == h : "frozenHash=" + hash(node) + " vs h=" + h;
				
				mCount ++;
				mTable[pos] = node;
				
				if (mTable.length < 2*mCount) 
					rehash();
				
				return node;
			} else if (nodesEqual(nodeIn, v, in)) {
				// same node is already here
				return v;
			}

			// quadratic probe
			pos = (pos + (++c)) & mMask;
		}
	}

	// called only by rehash
	private void addNew(int address) throws IOException {
		int pos = hash(address) & mMask;
		int c = 0;
		
		while (true) {
			if (mTable[pos] == 0) {
				mTable[pos] = address;
				break;
			}

			// quadratic probe
			pos = (pos + (++c)) & mMask;
		}
	}

	private void rehash() throws IOException {
		final int[] oldTable = mTable;
		mTable = new int[2*mTable.length];
		mMask = mTable.length-1;
		
		for (int idx=0; idx < oldTable.length; idx++) {
			final int address = oldTable[idx];
			if (address != 0) 
				addNew(address);
		}
	}

	public int count() {
		return mCount;
	}
	
}
