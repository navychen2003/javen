package org.javenstudio.falcon.search.update;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.javenstudio.common.indexdb.util.BitUtil;
import org.javenstudio.common.indexdb.util.BytesRef;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.hornet.query.FunctionValues;
import org.javenstudio.hornet.query.ValueSource;
import org.javenstudio.hornet.query.ValueSourceContext;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.SearcherRef;
import org.javenstudio.falcon.search.ISearchCore;
import org.javenstudio.falcon.search.schema.IndexSchema;
import org.javenstudio.falcon.search.schema.SchemaField;

public class VersionInfo {
	
	public static final String VERSION_FIELD = "_version_";

	private final ReadWriteLock mLock = new ReentrantReadWriteLock(true);
	
	private final UpdateLog mUpdateLog;
	private final VersionBucket[] mBuckets;
	
	private SchemaField mVersionField;
	private SchemaField mIdField;
  
	/** 
	 * We are currently using this time-based clock to avoid going back in time on a
	 * server restart (i.e. we don't want version numbers to start at 1 again).
	 */

	// Time-based lamport clock.  Good for introducing some reality into clocks (to the degree
	// that times are somewhat synchronized in the cluster).
	// Good if we want to relax some constraints to scale down to where only one node may be
	// up at a time.  Possibly harder to detect missing messages (because versions are not contiguous.
	
	private final Object mClockSync = new Object();
	private long mVclock;
	private long mTime;

	public VersionInfo(UpdateLog ulog, int nBuckets) throws ErrorException {
		mUpdateLog = ulog;
		
		ISearchCore core = ulog.getUpdateIndexer().getSearchCore();
		
		mVersionField = getAndCheckVersionField(core.getSchema());
		mIdField = core.getSchema().getUniqueKeyField();
		mBuckets = new VersionBucket[ BitUtil.nextHighestPowerOfTwo(nBuckets) ];
		
		for (int i=0; i < mBuckets.length; i++) {
			mBuckets[i] = new VersionBucket();
		}
	}

	public void reload() {
		// do nothing
	}

	public SchemaField getVersionField() { return mVersionField; }
	public SchemaField getIdField() { return mIdField; }
	
	public void lockForUpdate() { mLock.readLock().lock(); }
	public void unlockForUpdate() { mLock.readLock().unlock(); }

	public void blockUpdates() { mLock.writeLock().lock(); }
	public void unblockUpdates() { mLock.writeLock().unlock(); }

	public long getNewClock() {
		synchronized (mClockSync) {
			mTime = System.currentTimeMillis();
			
			long result = mTime << 20;
			if (result <= mVclock) 
				result = mVclock + 1;
      
			mVclock = result;
			return mVclock;
		}
	}

	public long getOldClock() {
		synchronized (mClockSync) {
			return mVclock;
		}
	}

	public void updateClock(long clock) {
		synchronized (mClockSync) {
			mVclock = Math.max(mVclock, clock);
		}
	}

	public VersionBucket getBucket(int hash) {
		// If this is a user provided hash, it may be poor in the right-hand bits.
		// Make sure high bits are moved down, since only the low bits will matter.
		// int h = hash + (hash >>> 8) + (hash >>> 16) + (hash >>> 24);
		// Assume good hash codes for now.
		int slot = hash & (mBuckets.length-1);
		
		return mBuckets[slot];
	}

	public Long lookupVersion(BytesRef idBytes) throws ErrorException {
		return mUpdateLog.lookupVersion(idBytes);
	}

	public Long getVersionFromIndex(BytesRef idBytes) throws ErrorException {
		// TODO: we could cache much of this and invalidate during a commit.
		// TODO: most DocValues classes are threadsafe - expose which.
		SearcherRef newestSearcher = mUpdateLog.getUpdateIndexer().
				getSearchCore().getSearchControl().getRealtimeSearcher();
		
		try {
			Searcher searcher = newestSearcher.get();
			long lookup = searcher.lookupId(idBytes);
			if (lookup < 0) 
				return null;

			ValueSource vs = mVersionField.getType().getValueSource(mVersionField, null);
			ValueSourceContext context = searcher.createValueSourceContext();
			searcher.createValueSourceWeight(context, vs);
			
			FunctionValues fv = vs.getValues(context, 
					searcher.getTopReaderContext().getLeaves().get((int)(lookup>>32)));
			long ver = fv.longVal((int)lookup);
			
			return ver;

		} catch (IOException e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Error reading version from index", e);
			
		} finally {
			if (newestSearcher != null) 
				newestSearcher.decreaseRef();
		}
	}

	/**
	 * Gets and returns the {@link #VERSION_FIELD} from the specified 
	 * schema, after verifying that it is indexed, stored, and single-valued.  
	 * If any of these pre-conditions are not met, it throws a ErrorException 
	 * with a user suitable message indicating the problem.
	 */
	public static SchemaField getAndCheckVersionField(IndexSchema schema) 
			throws ErrorException {
		final String errPrefix = VERSION_FIELD + "field must exist in schema, " 
				+ "using indexed=\"true\" stored=\"true\" and multiValued=\"false\"";
		
		SchemaField sf = schema.getFieldOrNull(VERSION_FIELD);

		if (sf == null) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					errPrefix + " (" + VERSION_FIELD + " does not exist)");
		}
		
		if (!sf.isIndexed()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					errPrefix + " (" + VERSION_FIELD + " is not indexed");
		}
		
		if (!sf.isStored()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					errPrefix + " (" + VERSION_FIELD + " is not stored");
		}
		
		if (sf.isMultiValued()) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					errPrefix + " (" + VERSION_FIELD + " is not multiValued");
		}
    
		return sf;
	}
	
}
