package org.javenstudio.falcon.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.index.IndexCommit;
import org.javenstudio.common.indexdb.index.IndexDeletionPolicy;

/**
 * A wrapper for an IndexDeletionPolicy instance.
 * <p/>
 * Provides features for looking up IndexCommit given a version. Allows reserving index
 * commit points for certain amounts of time to support features such as index replication
 * or snapshooting directly out of a live index directory.
 *
 * @see IndexDeletionPolicy
 */
public class DeletionPolicyWrapper extends IndexDeletionPolicy {
	
	private final IndexDeletionPolicy mDeletionPolicy;
	private final Map<Long, Long> mReserves = new ConcurrentHashMap<Long,Long>();
	
	private final ConcurrentHashMap<Long, AtomicInteger> mSavedCommits = 
			new ConcurrentHashMap<Long, AtomicInteger>();
	
	private volatile Map<Long, IndexCommit> mVersionVsCommits = 
			new ConcurrentHashMap<Long, IndexCommit>();
	
	private volatile IndexCommit mLatestCommit;
	
	public DeletionPolicyWrapper(IndexDeletionPolicy deletionPolicy) {
		mDeletionPolicy = deletionPolicy;
	}

	/**
	 * Gets the most recent commit point
	 * <p/>
	 * It is recommended to reserve a commit point for the duration of usage so that
	 * it is not deleted by the underlying deletion policy
	 *
	 * @return the most recent commit point
	 */
	public IndexCommit getLatestCommit() {
		return mLatestCommit;
	}

	public IndexDeletionPolicy getWrappedDeletionPolicy() {
		return mDeletionPolicy;
	}

	/**
	 * Set the duration for which commit point is to be reserved by the deletion policy.
	 *
	 * @param indexGen gen of the commit point to be reserved
	 * @param reserveTime  time in milliseconds for which the commit point is to be reserved
	 */
	public void setReserveDuration(Long indexGen, long reserveTime) {
		long timeToSet = System.currentTimeMillis() + reserveTime;
		for(;;) {
			Long previousTime = mReserves.put(indexGen, timeToSet);

			// this is the common success case: the older time didn't exist, or
			// came before the new time.
			if (previousTime == null || previousTime <= timeToSet) 
				break;

			// At this point, we overwrote a longer reservation, so we want to restore the older one.
			// the problem is that an even longer reservation may come in concurrently
			// and we don't want to overwrite that one too.  We simply keep retrying in a loop
			// with the maximum time value we have seen.
			timeToSet = previousTime;      
		}
	}

	private void cleanReserves() {
		long currentTime = System.currentTimeMillis();
		for (Map.Entry<Long, Long> entry : mReserves.entrySet()) {
			if (entry.getValue() < currentTime) 
				mReserves.remove(entry.getKey());
		}
	}

	private List<IndexCommitWrapper> wrap(List<? extends IndexCommit> list) {
		List<IndexCommitWrapper> result = new ArrayList<IndexCommitWrapper>();
		for (IndexCommit indexCommit : list) { 
			result.add(new IndexCommitWrapper(indexCommit)); 
		}
		return result;
	}

	/** 
	 * Permanently prevent this commit point from being deleted.
	 * A counter is used to allow a commit point to be correctly saved and released
	 * multiple times. 
	 */
	public synchronized void saveCommitPoint(Long indexCommitGen) {
		AtomicInteger reserveCount = mSavedCommits.get(indexCommitGen);
		if (reserveCount == null) 
			reserveCount = new AtomicInteger();
		
		reserveCount.incrementAndGet();
		mSavedCommits.put(indexCommitGen, reserveCount);
	}

	/** Release a previously saved commit point */
	public synchronized void releaseCommitPoint(Long indexCommitGen) {
		AtomicInteger reserveCount = mSavedCommits.get(indexCommitGen);
		if (reserveCount == null) 
			return;// this should not happen
		
		if (reserveCount.decrementAndGet() <= 0) 
			mSavedCommits.remove(indexCommitGen);
	}

	/**
	 * Internal use, do not explicitly call.
	 */
	public void onInit(List<? extends IndexCommit> list) throws IOException {
		List<IndexCommitWrapper> wrapperList = wrap(list);
		mDeletionPolicy.onInit(wrapperList);
		updateCommitPoints(wrapperList);
		cleanReserves();
	}

	/**
	 * Internal use, do not explicitly call.
	 */
	public void onCommit(List<? extends IndexCommit> list) throws IOException {
		List<IndexCommitWrapper> wrapperList = wrap(list);
		mDeletionPolicy.onCommit(wrapperList);
		updateCommitPoints(wrapperList);
		cleanReserves();
	}

	private class IndexCommitWrapper extends IndexCommit {
		private final IndexCommit mDelegate;

		IndexCommitWrapper(IndexCommit delegate) {
			mDelegate = delegate;
		}

		@Override
		public String getSegmentsFileName() {
			return mDelegate.getSegmentsFileName();
		}

		@Override
		public Collection<String> getFileNames() throws IOException {
			return mDelegate.getFileNames();
		}

		@Override
		public IDirectory getDirectory() {
			return mDelegate.getDirectory();
		}

		@Override
		public void delete() {
			Long gen = mDelegate.getGeneration();
			Long reserve = mReserves.get(gen);
			
			if (reserve != null && System.currentTimeMillis() < reserve) 
				return;
			
			if (mSavedCommits.containsKey(gen)) 
				return;
			
			mDelegate.delete();
		}

		@Override
		public int getSegmentCount() {
			return mDelegate.getSegmentCount();
		}

		@Override
		public boolean equals(Object o) {
			return mDelegate.equals(o);
		}

		@Override
		public int hashCode() {
			return mDelegate.hashCode();
		}

		@Override
		public long getGeneration() {
			return mDelegate.getGeneration();
		}

		@Override
		public boolean isDeleted() {
			return mDelegate.isDeleted();
		}

		@Override
		public Map<String,String> getUserData() throws IOException {
			return mDelegate.getUserData();
		}    
	}

	/**
	 * @param gen the gen of the commit point
	 * @return a commit point corresponding to the given version
	 */
	public IndexCommit getCommitPoint(Long gen) {
		return mVersionVsCommits.get(gen);
	}

	/**
	 * Gets the commit points for the index.
	 * This map instance may change between commits and commit points may be deleted.
	 * It is recommended to reserve a commit point for the duration of usage
	 *
	 * @return a Map of version to commit points
	 */
	public Map<Long, IndexCommit> getCommits() {
		return mVersionVsCommits;
	}

	private void updateCommitPoints(List<IndexCommitWrapper> list) {
		Map<Long, IndexCommit> map = new ConcurrentHashMap<Long, IndexCommit>();
		for (IndexCommitWrapper wrapper : list) {
			if (!wrapper.isDeleted())
				map.put(wrapper.mDelegate.getGeneration(), wrapper.mDelegate);
		}
		mVersionVsCommits = map;
		mLatestCommit = ((list.get(list.size() - 1)).mDelegate);
	}

}

