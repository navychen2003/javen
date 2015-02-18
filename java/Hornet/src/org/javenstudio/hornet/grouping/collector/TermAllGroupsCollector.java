package org.javenstudio.hornet.grouping.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.IDocTermsIndex;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.hornet.grouping.AbstractAllGroupsCollector;
import org.javenstudio.hornet.search.cache.FieldCache;
import org.javenstudio.hornet.util.SentinelIntSet;

/**
 * A collector that collects all groups that match the
 * query. Only the group value is collected, and the order
 * is undefined.  This collector does not determine
 * the most relevant document of a group.
 *
 * <p/>
 * Implementation detail: an int hash set (SentinelIntSet)
 * is used to detect if a group is already added to the
 * total count.  For each segment the int set is cleared and filled
 * with previous counted groups that occur in the new
 * segment.
 *
 */
public class TermAllGroupsCollector extends AbstractAllGroupsCollector<BytesRef> {

	public static final int DEFAULT_INITIAL_SIZE = 128;

	private final BytesRef mSpareBytesRef = new BytesRef();
	private final String mGroupField;
	private final SentinelIntSet mOrdSet;
	private final List<BytesRef> mGroups;

	private IDocTermsIndex mIndex;
	
	/**
	 * Expert: Constructs a {@link AbstractAllGroupsCollector}
	 *
	 * @param groupField  The field to group by
	 * @param initialSize The initial allocation size of the
	 *                    internal int set and group list
	 *                    which should roughly match the total
	 *                    number of expected unique groups. Be aware that the
	 *                    heap usage is 4 bytes * initialSize.
	 */
	public TermAllGroupsCollector(String groupField, int initialSize) {
		mOrdSet = new SentinelIntSet(initialSize, -1);
		mGroups = new ArrayList<BytesRef>(initialSize);
		mGroupField = groupField;
	}

	/**
	 * Constructs a {@link AbstractAllGroupsCollector}. This sets the
	 * initial allocation size for the internal int set and group
	 * list to 128.
	 *
	 * @param groupField The field to group by
	 */
	public TermAllGroupsCollector(String groupField) {
		this(groupField, DEFAULT_INITIAL_SIZE);
	}

	@Override
	public void collect(int doc) throws IOException {
		int key = mIndex.getOrd(doc);
		
		if (!mOrdSet.exists(key)) {
			mOrdSet.put(key);
			
			BytesRef term = key == 0 ? null : mIndex.lookup(key, new BytesRef());
			mGroups.add(term);
		}
	}

	@Override
	public Collection<BytesRef> getGroups() {
		return mGroups;
	}

	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mIndex = FieldCache.DEFAULT.getTermsIndex(context.getReader(), mGroupField);

		// Clear ordSet and fill it with previous encountered groups 
		// that can occur in the current segment.
		mOrdSet.clear();
		
		for (BytesRef countedGroup : mGroups) {
			int ord = mIndex.binarySearch(countedGroup, mSpareBytesRef);
			if (ord >= 0) 
				mOrdSet.put(ord);
		}
	}

}
