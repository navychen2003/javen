package org.javenstudio.hornet.grouping;

import java.io.IOException;
import java.util.Collection;

import org.javenstudio.common.indexdb.IFixedBitSet;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.hornet.search.OpenFixedBitSet;

/**
 * This collector specializes in collecting the most relevant document (group head) 
 * for each group that match the query.
 *
 */
public abstract class AbstractAllGroupHeadsCollector<GH extends GroupHead<?>> 
		extends Collector {

	protected final int[] mReversed;
	protected final int mCompIDXEnd;
	protected final TemporalResult<GH> mTemporalResult;

	protected AbstractAllGroupHeadsCollector(int numberOfSorts) {
		mReversed = new int[numberOfSorts];
		mCompIDXEnd = numberOfSorts - 1;
		mTemporalResult = new TemporalResult<GH>();
	}

	/**
	 * @param maxDoc The maxDoc of the top level {@link IndexReader}.
	 * @return an {@link FixedBitSet} containing all group heads.
	 */
	public IFixedBitSet retrieveGroupHeads(int maxDoc) {
		IFixedBitSet bitSet = new OpenFixedBitSet(maxDoc);

		Collection<GH> groupHeads = getCollectedGroupHeads();
		for (GroupHead<?> groupHead : groupHeads) {
			bitSet.set(groupHead.getDoc());
		}

		return bitSet;
	}

	/**
	 * @return an int array containing all group heads. 
	 * The size of the array is equal to number of collected unique groups.
	 */
	public int[] retrieveGroupHeads() {
		Collection<GH> groupHeads = getCollectedGroupHeads();
		
		int[] docHeads = new int[groupHeads.size()];
		int i = 0;
		for (GroupHead<?> groupHead : groupHeads) {
			docHeads[i++] = groupHead.getDoc();
		}

		return docHeads;
	}

	/**
	 * @return the number of group heads found for a query.
	 */
	public int groupHeadsSize() {
		return getCollectedGroupHeads().size();
	}

	/**
	 * Returns the group head and puts it into {@link #temporalResult}.
	 * If the group head wasn't encountered before then it will be added to the collected group heads.
	 * <p/>
	 * The {@link TemporalResult#stop} property will be <code>true</code> 
	 * if the group head wasn't encountered before
	 * otherwise <code>false</code>.
	 *
	 * @param doc The document to retrieve the group head for.
	 * @throws IOException If I/O related errors occur
	 */
	protected abstract void retrieveGroupHeadAndAddIfNotExist(int doc) 
			throws IOException;

	/**
	 * Returns the collected group heads.
	 * Subsequent calls should return the same group heads.
	 *
	 * @return the collected group heads
	 */
	protected abstract Collection<GH> getCollectedGroupHeads();

	public void collect(int doc) throws IOException {
		retrieveGroupHeadAndAddIfNotExist(doc);
		if (mTemporalResult.isStop()) 
			return;
		
		GH groupHead = mTemporalResult.getGroupHead();

		// Ok now we need to check if the current doc is more relevant then current doc for this group
		for (int compIDX = 0; ; compIDX++) {
			final int c = mReversed[compIDX] * groupHead.compare(compIDX, doc);
			if (c < 0) {
				// Definitely not competitive. So don't even bother to continue
				return;
				
			} else if (c > 0) {
				// Definitely competitive.
				break;
				
			} else if (compIDX == mCompIDXEnd) {
				// Here c=0. If we're at the last comparator, this doc is not
				// competitive, since docs are visited in doc Id order, which means
				// this doc cannot compete with any other document in the queue.
				return;
			}
		}
		
		groupHead.updateDocHead(doc);
	}

	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

}
