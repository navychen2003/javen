package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.Constants;
import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IMergeOne;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.MergeAbortedException;
import org.javenstudio.common.indexdb.MergeException;
import org.javenstudio.common.indexdb.codec.ISegmentInfoFormat;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfo;
import org.javenstudio.common.indexdb.index.segment.SegmentInfos;
import org.javenstudio.common.indexdb.store.TrackingDirectoryWrapper;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.util.Logger;

public class MergeControl {
	private static final Logger LOG = Logger.getLogger(MergeControl.class);

	private final IndexWriter mIndexWriter;
	
	private final MergePolicy mMergePolicy;
	private final MergeScheduler mMergeScheduler;
	
	// used by forceMerge to note those needing merging
	private Map<ISegmentCommitInfo,Boolean> mSegmentsToMerge = 
			new HashMap<ISegmentCommitInfo,Boolean>();
	private int mMergeMaxNumSegments = 0;
	
	// Holds all SegmentInfo instances currently involved in merges
	private HashSet<ISegmentCommitInfo> mMergingSegments = 
			new HashSet<ISegmentCommitInfo>();
	
	private LinkedList<MergeOne> mPendingMerges = new LinkedList<MergeOne>();
	private Set<MergeOne> mRunningMerges = new HashSet<MergeOne>();
	private List<MergeOne> mMergeExceptions = new ArrayList<MergeOne>();
	
	private long mMergeGen = 0;
	private boolean mStopMerges = false;
	
	public MergeControl(IndexWriter indexWriter, IndexParams params) { 
		mIndexWriter = indexWriter;
		mMergePolicy = params.getMergePolicy();
		mMergeScheduler = params.getMergeScheduler();
	}
	
	public IndexWriter getIndexWriter() { return mIndexWriter; }
	public IndexContext getContext() { return mIndexWriter.getContext(); }
	public IDirectory getDirectory() { return mIndexWriter.getDirectory(); }
	public SegmentInfos getSegmentInfos() { return mIndexWriter.getSegmentInfos(); }
	
	public boolean isStopMerges() { return mStopMerges; }
	public void setStopMerges(boolean stop) { mStopMerges = stop; }
	
	public final MergePolicy getMergePolicy() { 
		return mMergePolicy;
	}
	
	public final MergeScheduler getMergeScheduler() { 
		return mMergeScheduler;
	}
	
	public synchronized boolean containsMergingSegment(ISegmentCommitInfo info) { 
		return mMergingSegments.contains(info);
	}
	
	/**
	 * Expert: the {@link MergeScheduler} calls this method to retrieve the next
	 * merge requested by the MergePolicy
	 */
	public final synchronized IMergeOne getNextMerge() {
		if (mPendingMerges.size() == 0) {
			return null;
		} else {
			// Advance the merge from pending to running
			MergeOne merge = mPendingMerges.removeFirst();
			mRunningMerges.add(merge);
			return merge;
		}
	}

	private synchronized void resetMergeExceptions() {
	    mMergeExceptions = new ArrayList<MergeOne>();
	    mMergeGen ++;
	}
	
	public synchronized boolean useCompoundFile(ISegmentCommitInfo segmentInfo) 
			throws IOException {
	    return mMergePolicy.useCompoundFile(getSegmentInfos(), segmentInfo);
	}
	
	/** Hook that's called when the specified merge is complete. */
	public void mergeSuccess(MergeOne merge) {}
	
	/**
	 * Expert: asks the mergePolicy whether any merges are
	 * necessary now and if so, runs the requested merges and
	 * then iterate (test again if merges are needed) until no
	 * more merges are returned by the mergePolicy.
	 *
	 * Explicit calls to maybeMerge() are usually not
	 * necessary. The most common case is when merge policy
	 * parameters have changed.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 */
	public final void maybeMerge() throws CorruptIndexException, IOException {
		maybeMerge(-1);
	}
	
	public void maybeMerge(int maxNumSegments) throws CorruptIndexException, IOException {
		getIndexWriter().ensureOpen(false);
		updatePendingMerges(maxNumSegments);
		mMergeScheduler.merge(getIndexWriter());
	}
	
	/**
	 * Forces merge policy to merge segments until there are <=
	 * maxNumSegments.  The actual merges to be
	 * executed are determined by the {@link MergePolicy}.
	 *
	 * <p>This is a horribly costly operation, especially when
	 * you pass a small {@code maxNumSegments}; usually you
	 * should only call this if the index is static (will no
	 * longer be changed).</p>
	 *
	 * <p>Note that this requires up to 2X the index size free
	 * space in your Directory (3X if you're using compound
	 * file format).  For example, if your index size is 10 MB
	 * then you need up to 20 MB free for this to complete (30
	 * MB if you're using compound file format).  Also,
	 * it's best to call {@link #commit()} afterwards,
	 * to allow IndexWriter to free up disk space.</p>
	 *
	 * <p>If some but not all readers re-open while merging
	 * is underway, this will cause > 2X temporary
	 * space to be consumed as those new readers will then
	 * hold open the temporary segments at that time.  It is
	 * best not to re-open readers while merging is running.</p>
	 *
	 * <p>The actual temporary usage could be much less than
	 * these figures (it depends on many factors).</p>
	 *
	 * <p>In general, once this completes, the total size of the
	 * index will be less than the size of the starting index.
	 * It could be quite a bit smaller (if there were many
	 * pending deletes) or just slightly smaller.</p>
	 *
	 * <p>If an Exception is hit, for example
	 * due to disk full, the index will not be corrupted and no
	 * documents will be lost.  However, it may have
	 * been partially merged (some segments were merged but
	 * not all), and it's possible that one of the segments in
	 * the index will be in non-compound format even when
	 * using compound file format.  This will occur when the
	 * Exception is hit during conversion of the segment into
	 * compound format.</p>
	 *
	 * <p>This call will merge those segments present in
	 * the index when the call started.  If other threads are
	 * still adding documents and flushing segments, those
	 * newly created segments will not be merged unless you
	 * call forceMerge again.</p>
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * <p><b>NOTE</b>: if you call {@link #close(boolean)}
	 * with <tt>false</tt>, which aborts all running merges,
	 * then any thread still running this method might hit a
	 * {@link MergePolicy.MergeAbortedException}.
	 *
	 * @param maxNumSegments maximum number of segments left
	 * in the index after merging finishes
	 * 
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 * @see MergePolicy#findMerges
	 */
	public void forceMerge(int maxNumSegments) throws IOException {
		forceMerge(maxNumSegments, true);
	}
	
	/** 
	 * Just like {@link #forceMerge(int)}, except you can
	 *  specify whether the call should block until
	 *  all merging completes.  This is only meaningful with a
	 *  {@link MergeScheduler} that is able to run merges in
	 *  background threads.
	 *
	 *  <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 *  you should immediately close the writer.  See <a
	 *  href="#OOME">above</a> for details.</p>
	 */
	public void forceMerge(int maxNumSegments, boolean doWait) throws IOException {
		getIndexWriter().ensureOpen();

		if (maxNumSegments < 1)
			throw new IllegalArgumentException("maxNumSegments must be >= 1; got " + maxNumSegments);

		if (LOG.isDebugEnabled()) { 
			LOG.debug("forceMerge: index now " + getIndexWriter().toSegmentString());
			LOG.debug("now flush at forceMerge");
		}

		getIndexWriter().flush(true, true);

		synchronized(this) {
			resetMergeExceptions();
			mSegmentsToMerge.clear();
			
			for (ISegmentCommitInfo info : getSegmentInfos()) {
				mSegmentsToMerge.put(info, Boolean.TRUE);
			}
			mMergeMaxNumSegments = maxNumSegments;

			// Now mark all pending & running merges for forced
			// merge:
			for (final MergeOne merge : mPendingMerges) {
				merge.setMaxNumSegments(maxNumSegments);
				mSegmentsToMerge.put(merge.getCommitInfo(), Boolean.TRUE);
			}

			for (final MergeOne merge: mRunningMerges) {
				merge.setMaxNumSegments(maxNumSegments);
				mSegmentsToMerge.put(merge.getCommitInfo(), Boolean.TRUE);
			}
		}

		maybeMerge(maxNumSegments);

		if (doWait) {
			synchronized(this) {
				while (true) {
					if (getIndexWriter().isHitOOM()) {
						throw new IllegalStateException("this writer hit an OutOfMemoryError; " + 
								"cannot complete forceMerge");
					}

					if (mMergeExceptions.size() > 0) {
						// Forward any exceptions in background merge
						// threads to the current thread:
						final int size = mMergeExceptions.size();
						
						for (int i=0; i < size; i++) {
							final MergeOne merge = mMergeExceptions.get(i);
							if (merge.getMaxNumSegments() != -1) {
								IOException err = new IOException("background merge hit exception: " 
										+ merge.toSegmentString(getDirectory()));
								
								final Throwable t = merge.getException();
								if (t != null)
									err.initCause(t);
								
								throw err;
							}
						}
					}

					if (hasMaxNumSegmentsMergesPending())
						getIndexWriter().doWait();
					else
						break;
				}
			}

			// If close is called while we are still
			// running, throw an exception so the calling
			// thread will know merging did not
			// complete
			getIndexWriter().ensureOpen();
		}

		// NOTE: in the ConcurrentMergeScheduler case, when
		// doWait is false, we can return immediately while
		// background threads accomplish the merging
	}
	
	/** 
	 * Returns true if any merges in pendingMerges or
	 *  runningMerges are maxNumSegments merges. 
	 */
	private synchronized boolean hasMaxNumSegmentsMergesPending() {
		for (final MergeOne merge : mPendingMerges) {
			if (merge.getMaxNumSegments() != -1)
				return true;
		}

		for (final MergeOne merge : mRunningMerges) {
			if (merge.getMaxNumSegments() != -1)
				return true;
		}

		return false;
	}
	
	/**
	 *  Forces merging of all segments that have deleted
	 *  documents.  The actual merges to be executed are
	 *  determined by the {@link MergePolicy}.  For example,
	 *  the default {@link TieredMergePolicy} will only
	 *  pick a segment if the percentage of
	 *  deleted docs is over 10%.
	 *
	 *  <p>This is often a horribly costly operation; rarely
	 *  is it warranted.</p>
	 *
	 *  <p>To see how
	 *  many deletions you have pending in your index, call
	 *  {@link IndexReader#numDeletedDocs}.</p>
	 *
	 *  <p><b>NOTE</b>: this method first flushes a new
	 *  segment (if there are indexed documents), and applies
	 *  all buffered deletes.
	 *
	 *  <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 *  you should immediately close the writer.  See <a
	 *  href="#OOME">above</a> for details.</p>
	 */
	public void forceMergeDeletes() throws IOException {
		forceMergeDeletes(true);
	}
	
	/** 
	 * Just like {@link #forceMergeDeletes()}, except you can
	 *  specify whether the call should block until the
	 *  operation completes.  This is only meaningful with a
	 *  {@link MergeScheduler} that is able to run merges in
	 *  background threads.
	 *
	 * <p><b>NOTE</b>: if this method hits an OutOfMemoryError
	 * you should immediately close the writer.  See <a
	 * href="#OOME">above</a> for details.</p>
	 *
	 * <p><b>NOTE</b>: if you call {@link #close(boolean)}
	 * with <tt>false</tt>, which aborts all running merges,
	 * then any thread still running this method might hit a
	 * {@link MergePolicy.MergeAbortedException}.
	 */
	public void forceMergeDeletes(boolean doWait) throws IOException {
		getIndexWriter().ensureOpen();
		getIndexWriter().flush(true, true);

		if (LOG.isDebugEnabled())
			LOG.debug("forceMergeDeletes: index now " + getIndexWriter().toSegmentString());

		MergeSpecification spec;

		synchronized(this) {
			spec = mMergePolicy.findForcedDeletesMerges(getSegmentInfos());
			if (spec != null) {
				final int numMerges = spec.getMergeCount();
				for (int i=0; i < numMerges; i++) {
					registerMerge((MergeOne)spec.getMerge(i));
				}
			}
		}

		mMergeScheduler.merge(getIndexWriter());

		if (spec != null && doWait) {
			final int numMerges = spec.getMergeCount();
			
			synchronized (this) {
				boolean running = true;
				
				while (running) {
					if (getIndexWriter().isHitOOM()) {
						throw new IllegalStateException("this writer hit an OutOfMemoryError; " + 
								"cannot complete forceMergeDeletes");
					}

					// Check each merge that MergePolicy asked us to
					// do, to see if any of them are still running and
					// if any of them have hit an exception.
					running = false;
					
					for (int i=0; i < numMerges; i++) {
						final MergeOne merge = (MergeOne)spec.getMerge(i);
						if (mPendingMerges.contains(merge) || mRunningMerges.contains(merge)) 
							running = true;
						
						Throwable t = merge.getException();
						if (t != null) {
							IOException ioe = new IOException("background merge hit exception: " 
									+ merge.toSegmentString(getDirectory()));
							ioe.initCause(t);
							throw ioe;
						}
					}

					// If any of our merges are still running, wait:
					if (running)
						getIndexWriter().doWait();
				}
			}
		}

		// NOTE: in the ConcurrentMergeScheduler case, when
		// doWait is false, we can return immediately while
		// background threads accomplish the merging
	}
	
	private synchronized void updatePendingMerges(int maxNumSegments) 
			throws CorruptIndexException, IOException {
		assert maxNumSegments == -1 || maxNumSegments > 0;

		// Do not start new merges if we've hit OOME
		if (mStopMerges || getIndexWriter().isHitOOM()) 
			return;
		
	    final MergeSpecification spec;
	    if (maxNumSegments != -1) {
	    	spec = mMergePolicy.findForcedMerges(getSegmentInfos(), maxNumSegments, 
	    			Collections.unmodifiableMap(mSegmentsToMerge));
	    	
	    	if (spec != null) {
	    		final int numMerges = spec.getMergeCount();
	    		for (int i=0; i < numMerges; i++) {
	    			final MergeOne merge = (MergeOne)spec.getMerge(i);
	    			merge.setMaxNumSegments(maxNumSegments);
	    		}
	    	}
	    } else {
	    	spec = mMergePolicy.findMerges(getSegmentInfos());
	    }

	    if (spec != null) {
	    	final int numMerges = spec.getMergeCount();
	    	for (int i=0; i < numMerges; i++) {
	    		registerMerge((MergeOne)spec.getMerge(i));
	    	}
	    }
	}
	
	/** 
	 * Checks whether this merge involves any segments
	 *  already participating in a merge.  If not, this merge
	 *  is "registered", meaning we record that its segments
	 *  are now participating in a merge, and true is
	 *  returned.  Else (the merge conflicts) false is
	 *  returned. 
	 */
	private synchronized boolean registerMerge(MergeOne merge) 
			throws MergeAbortedException, IOException {
		if (merge.isRegisterDone()) 
			return true;
      
		assert merge.getSegmentSize() > 0;

		if (mStopMerges) {
			merge.abort();
			throw new MergeAbortedException("merge is aborted: " + 
					getIndexWriter().toSegmentString(merge.getSegments()));
		}

		boolean isExternal = false;
		for (int i=0; i < merge.getSegmentSize(); i++) {
			ISegmentCommitInfo info = merge.getSegmentAt(i);
			if (mMergingSegments.contains(info)) 
				return false;
			
			if (!getSegmentInfos().contains(info)) 
				return false;
			
			if (info.getSegmentInfo().getDirectory() != getDirectory()) 
				isExternal = true;
			
			if (mSegmentsToMerge.containsKey(info)) 
				merge.setMaxNumSegments(mMergeMaxNumSegments);
		}

		ensureValidMerge(merge);
		mPendingMerges.add(merge);
		
		if (LOG.isDebugEnabled()) { 
			LOG.debug("add merge to pendingMerges: " 
					+ getIndexWriter().toSegmentString(merge.getSegments()) 
					+ " [total " + mPendingMerges.size() + " pending]");
		}
		
	    merge.setMergeGen(mMergeGen);
	    merge.setExternal(isExternal);

	    // OK it does not conflict; now record that this merge
	    // is running (while synchronized) to avoid race
	    // condition where two conflicting merges from different
	    // threads, start
	    if (LOG.isDebugEnabled()) { 
	        StringBuilder builder = new StringBuilder("registerMerge merging= [");
	        for (ISegmentCommitInfo info : mMergingSegments) {
	        	builder.append(info.getSegmentInfo().getName()).append(", ");  
	        }
	        builder.append("]");
	        // don't call mergingSegments.toString() could lead to ConcurrentModException
	        // since merge updates the segments FieldInfos
	        LOG.debug(builder.toString());
	    }
	    
	    for (int i=0; i < merge.getSegmentSize(); i++) {
	    	ISegmentCommitInfo info = merge.getSegmentAt(i);
	    	if (LOG.isDebugEnabled())
	    		LOG.debug("registerMerge info=" + getIndexWriter().toSegmentString(info));
	    	
	    	mMergingSegments.add(info);
	    }

	    // Merge is now registered
	    merge.setRegisterDone(true);
		
		return true;
	}
	
	private synchronized void ensureValidMerge(MergeOne merge) throws IOException {
		for (int i=0; i < merge.getSegmentSize(); i++) {
			ISegmentCommitInfo info = merge.getSegmentAt(i);
			
			if (!getSegmentInfos().contains(info)) {
				throw new MergeException("MergePolicy selected a segment (" 
						+ info.getSegmentInfo().getName() + ") that is not in the current index " 
						+ getIndexWriter().toSegmentString(), getDirectory());
			}
		}
	}
	
	public synchronized void finishMerges(boolean waitForMerges) throws IOException {
		if (!waitForMerges) {
			mStopMerges = true;

			// Abort all pending & running merges:
			for (final MergeOne merge : mPendingMerges) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("now abort pending merge " 
							+ getIndexWriter().toSegmentString(merge.getSegments()));
				}
				
				merge.abort();
				mergeFinish(merge);
			}
			mPendingMerges.clear();

			for (final MergeOne merge : mRunningMerges) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("now abort running merge " 
							+ getIndexWriter().toSegmentString(merge.getSegments()));
				}
				
				merge.abort();
			}

			// These merges periodically check whether they have
			// been aborted, and stop if so.  We wait here to make
			// sure they all stop.  It should not take very long
			// because the merge threads periodically check if
			// they are aborted.
			while (mRunningMerges.size() > 0) {
				if (LOG.isDebugEnabled())
					LOG.debug("now wait for " + mRunningMerges.size() + " running merge to abort");
				
				getIndexWriter().doWait();
			}

			mStopMerges = false;
			notifyAll();

			assert 0 == mMergingSegments.size();

			if (LOG.isDebugEnabled())
				LOG.debug("all running merges have aborted");
			
		} else {
			// waitForMerges() will ensure any running addIndexes finishes.
			// It's fine if a new one attempts to start because from our
			// caller above the call will see that we are in the
			// process of closing, and will throw an
			// AlreadyClosedException.
			waitForMerges();
		}
	}
	
	/** 
	 * Does finishing for a merge, which is fast but holds
	 *  the synchronized lock on IndexWriter instance. 
	 */
	private final synchronized void mergeFinish(MergeOne merge) throws IOException {
		// forceMerge, addIndexes or finishMerges may be waiting
		// on merges to finish.
		notifyAll();

		// It's possible we are called twice, eg if there was an
		// exception inside mergeInit
		if (merge.isRegisterDone()) {
			for (int i=0; i < merge.getSegmentSize(); i++) {
				ISegmentCommitInfo info = merge.getSegmentAt(i);
				mMergingSegments.remove(info);
			}
			merge.setRegisterDone(false);
		}

		mRunningMerges.remove(merge);
	}
	
	/**
	 * Wait for any currently outstanding merges to finish.
	 *
	 * <p>It is guaranteed that any merges started prior to calling this method
	 *    will have completed once this method completes.</p>
	 */
	public synchronized void waitForMerges() {
		getIndexWriter().ensureOpen(false);

		if (LOG.isDebugEnabled())
			LOG.debug("wait for merges ...");
		
		while (mPendingMerges.size() > 0 || mRunningMerges.size() > 0) {
			getIndexWriter().doWait();
		}

		// sanity check
		assert 0 == mMergingSegments.size();
		
		if (LOG.isDebugEnabled())
			LOG.debug("wait for merges done.");
	}
	
	/**
	 * Merges the indicated segments, replacing them in the stack with a
	 * single segment.
	 */
	public void merge(IMergeOne mergeOne) throws CorruptIndexException, IOException {
	    final long t0 = System.currentTimeMillis();
	    final MergeOne merge = (MergeOne)mergeOne;

	    boolean success = false;
	    try {
	    	try {
	    		try {
	    			mergeInit(merge);
	    			
	    			if (LOG.isDebugEnabled()) {
	    				LOG.debug("now merge");
	    				LOG.debug(" merge=" + getIndexWriter().toSegmentString(merge.getSegments()));
	    				LOG.debug(" index=" + getIndexWriter().toSegmentString());
	    			}
	    			
	    			mergeMiddle(merge);
	    			mergeSuccess(merge);
	    			
	    			success = true;
	    		} catch (Throwable t) {
	    			handleMergeException(t, merge);
	    		}
	    		
	    	} finally {
	    		synchronized(this) {
	    			mergeFinish(merge);

	    			if (!success) {
	    				if (LOG.isDebugEnabled())
	    					LOG.debug("hit exception during merge");
	    				
	    				if (merge.getCommitInfo() != null && 
	    					!getSegmentInfos().contains(merge.getCommitInfo())) {
	    					getIndexWriter().getDeleter().refresh(
	    							merge.getCommitInfo().getSegmentInfo().getName());
	    				}
	    			}

	    			// This merge (and, generally, any change to the
	    			// segments) may now enable new merges, so we call
	    			// merge policy & update pending merges.
	    			if (success && !merge.isAborted() && (merge.getMaxNumSegments() != -1 || 
	    				(!getIndexWriter().isClosed() && !getIndexWriter().isClosing()))) {
	    				updatePendingMerges(merge.getMaxNumSegments());
	    			}
	    		}
	    	}
	    	
    	} catch (OutOfMemoryError oom) {
    		getIndexWriter().handleOOM(oom, "merge");
    	}
	    
	    if (merge.getCommitInfo() != null && !merge.isAborted()) { 
	    	if (LOG.isDebugEnabled()) { 
	    		LOG.debug("merge time " + (System.currentTimeMillis()-t0) + " msec for " + 
	    				merge.getCommitInfo().getSegmentInfo().getDocCount() + " docs");
	    	}
	    }
	}
	
	/** 
	 * Does initial setup for a merge, which is fast but holds
	 *  the synchronized lock on IndexWriter instance. 
	 */
	private final synchronized void mergeInit(MergeOne merge) throws IOException {
		boolean success = false;
		try {
			doMergeInit(merge);
			success = true;
		} finally {
			if (!success) {
				if (LOG.isDebugEnabled())
					LOG.debug("hit exception in mergeInit");
				
				mergeFinish(merge);
			}
		}
	}
	
	private final void handleMergeException(Throwable t, MergeOne merge) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("handleMergeException: merge=" 
					+ getIndexWriter().toSegmentString(merge.getSegments()) 
					+ " exc=" + t);
		}
		
		// Set the exception on the merge, so if
		// forceMerge is waiting on us it sees the root
		// cause exception:
		merge.setException(t);
		addMergeException(merge);

		if (t instanceof MergeAbortedException) {
			// We can ignore this exception (it happens when
			// close(false) or rollback is called), unless the
			// merge involves segments from external directories,
			// in which case we must throw it so, for example, the
			// rollbackTransaction code in addIndexes* is
			// executed.
			if (merge.isExternal())
				throw (MergeAbortedException) t;
			
		} else if (t instanceof IOException) {
			throw (IOException) t;
			
		} else if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
			
		} else if (t instanceof Error) {
			throw (Error) t;
			
		} else {
			// Should not get here
			throw new RuntimeException(t);
		}
	}
	
	private synchronized void addMergeException(MergeOne merge) {
	    assert merge.getException() != null;
	    if (!mMergeExceptions.contains(merge) && mMergeGen == merge.getMergeGen()) {
	    	mMergeExceptions.add(merge);
	    }
	}
	
	private synchronized void doMergeInit(MergeOne merge) throws IOException {
		assert merge.isRegisterDone();
		assert merge.getMaxNumSegments() == -1 || merge.getMaxNumSegments() > 0;

		if (getIndexWriter().isHitOOM()) {
			throw new IllegalStateException(
					"this writer hit an OutOfMemoryError; cannot merge");
		}

		if (merge.getCommitInfo() != null) {
			// mergeInit already done
			return;
		}

		if (merge.isAborted()) 
			return;

		// TODO: in the non-pool'd case this is somewhat
		// wasteful, because we open these readers, close them,
		// and then open them again for merging.  Maybe  we
		// could pre-pool them somehow in that case...

	    // Lock order: IW -> BD
	    final DeletesStream.ApplyResult result = 
	    		getIndexWriter().getDeletesStream().applyDeletes(
	    				getIndexWriter().getReaderPool(), merge.getSegments());

	    if (result.anyDeletes()) 
	    	getIndexWriter().checkpoint();

	    if (result.getAllDeleted() != null) {
	    	if (LOG.isDebugEnabled())
	    		LOG.debug("drop 100% deleted segments: " + result.getAllDeleted());
	    	
	    	for (ISegmentCommitInfo info : result.getAllDeleted()) {
	    		getSegmentInfos().remove(info);
	    		if (merge.containsSegment(info)) {
	    			mMergingSegments.remove(info);
	    			merge.removeSegment(info);
	    		}
	    		getIndexWriter().getReaderPool().drop(info);
	    	}
	    	getIndexWriter().checkpoint();
	    }

	    // Bind a new segment name here so even with
	    // ConcurrentMergePolicy we keep deterministic segment
	    // names.
	    final String mergeSegmentName = getIndexWriter().newSegmentName();
	    SegmentInfo si = new SegmentInfo(getDirectory(), Constants.INDEXDB_MAIN_VERSION, 
	    		mergeSegmentName, -1, false, null, null);
	    merge.setCommitInfo(new SegmentCommitInfo(getIndexWriter().getIndexFormat(), si, 0, -1L));

	    // Lock order: IW -> BD
	    getIndexWriter().getDeletesStream().prune(getSegmentInfos());

	    Map<String,String> details = new HashMap<String,String>();
	    details.put("mergeMaxNumSegments", ""+merge.getMaxNumSegments());
	    details.put("mergeFactor", Integer.toString(merge.getSegmentSize()));
	    IndexWriter.setDiagnostics(si, "merge", details);

	    if (LOG.isDebugEnabled())
	    	LOG.debug("merge seg=" + merge.getCommitInfo().getSegmentInfo().getName());
	    assert merge.getEstimatedMergeBytes() == 0;
	    
	    for (int i=0; i < merge.getSegmentSize(); i++) {
	    	ISegmentCommitInfo info = merge.getSegmentAt(i);
	    	
	    	if (info.getSegmentInfo().getDocCount() > 0) {
	    		final int delCount = getIndexWriter().getNumDeletedDocs(info);
	    		assert delCount <= info.getSegmentInfo().getDocCount();
	    		
	    		final double delRatio = ((double) delCount)/info.getSegmentInfo().getDocCount();
	    		merge.increaseEstimatedMergeBytes(
	    				(long)(info.getSegmentInfo().getSizeInBytes() * (1.0 - delRatio)));
	    	}
	    }
	}
	
	/** 
	 * Does the actual (time-consuming) work of the merge,
	 *  but without holding synchronized lock on IndexWriter
	 *  instance 
	 */
	private int mergeMiddle(MergeOne merge) throws CorruptIndexException, IOException {
	    merge.checkAborted(getDirectory());

	    final String mergedName = merge.getCommitInfo().getSegmentInfo().getName();
	    
	    getContext().setMergeInfo(merge.getMergeInfo());
	    
	    final CheckAbort checkAbort = new CheckAbort(merge, getDirectory());
	    final TrackingDirectoryWrapper dirWrapper = new TrackingDirectoryWrapper(getDirectory());

	    SegmentMerger merger = getIndexWriter().getIndexParams().newSegmentMerger(
	    		getIndexWriter(), merge.getCommitInfo().getSegmentInfo(), dirWrapper, checkAbort, 
	    		getIndexWriter().getGlobalFieldNumbers());

	    if (LOG.isDebugEnabled())
	    	LOG.debug("merging " + getIndexWriter().toSegmentString(merge.getSegments()));
	    
	    // This is try/finally to make sure merger's readers are
	    // closed:
	    boolean success = false;
	    try {
	    	int segUpto = 0;
	    	while (segUpto < merge.getSegmentSize()) {
	    		final ISegmentCommitInfo info = merge.getSegmentAt(segUpto);

	    		// Hold onto the "live" reader; we will use this to
	    		// commit merged deletes
	    		final ReadersAndLiveDocs rld = getIndexWriter().getReaderPool().get(info, true);
	    		ISegmentReader reader = rld.getMergeReader();
	    		assert reader != null;

	    		// Carefully pull the most recent live docs:
	    		final Bits liveDocs;
	    		final int delCount;

	    		synchronized(this) {
	    			// Must sync to ensure BufferedDeletesStream
	    			// cannot change liveDocs/pendingDeleteCount while
	    			// we pull a copy:
	    			liveDocs = rld.getReadOnlyLiveDocs();
	    			delCount = rld.getPendingDeleteCount() + info.getDelCount();

	    			assert rld.verifyDocCounts();

	    			if (LOG.isDebugEnabled()) { 
	    				if (rld.getPendingDeleteCount() != 0) {
	    					LOG.debug(" seg=" + getIndexWriter().toSegmentString(info) 
	    							+ " delCount=" + info.getDelCount() + " pendingDelCount=" 
	    							+ rld.getPendingDeleteCount());
	    				} else if (info.getDelCount() != 0) {
	    					LOG.debug(" seg=" + getIndexWriter().toSegmentString(info) 
	    							+ " delCount=" + info.getDelCount());
	    				} else {
	    					LOG.debug(" seg=" + getIndexWriter().toSegmentString(info) 
	    							+ " no deletes");
	    				}
	    			}
	    		}

	    		// Deletes might have happened after we pulled the merge reader and
	    		// before we got a read-only copy of the segment's actual live docs
	    		// (taking pending deletes into account). In that case we need to
	    		// make a new reader with updated live docs and del count.
	    		if (reader.getNumDeletedDocs() != delCount) {
	    			// fix the reader's live docs and del count
	    			assert delCount > reader.getNumDeletedDocs(); // beware of zombies

	    			ISegmentReader newReader = getIndexWriter().getIndexParams().newSegmentReader(
	    					reader, info, liveDocs, (info.getSegmentInfo().getDocCount() - delCount));
	    			
	    			boolean released = false;
	    			try {
	    				rld.release(reader);
	    				released = true;
	    			} finally {
	    				if (!released) 
	    					newReader.decreaseRef();
	    			}

	    			reader = newReader;
	    		}

	    		merge.addReader(reader);
	    		assert delCount <= info.getSegmentInfo().getDocCount(): "delCount=" + delCount + 
	    				" info.docCount=" + info.getSegmentInfo().getDocCount() + 
	    				" rld.pendingDeleteCount=" + rld.getPendingDeleteCount() + 
	    				" info.getDelCount()=" + info.getDelCount();
	    		
	    		if (delCount < info.getSegmentInfo().getDocCount()) 
	    			merger.add(reader);
	    		
	    		segUpto ++;
	    	}

	    	merge.checkAborted(getDirectory());

	    	// This is where all the work happens:
	    	MergeState mergeState = merger.merge();
	    	assert mergeState.getSegmentInfo() == merge.getCommitInfo().getSegmentInfo();
	      
	    	merge.getCommitInfo().getSegmentInfo().setFileNames(
	    			new HashSet<String>(dirWrapper.getCreatedFiles()));

	    	// Record which codec was used to write the segment
	    	if (LOG.isDebugEnabled()) { 
	    		LOG.debug("merge docCount=" + merge.getCommitInfo().getSegmentInfo().getDocCount() + "; merged segment has " +
                        (mergeState.getFieldInfos().hasVectors() ? "vectors" : "no vectors") + "; " +
                        (mergeState.getFieldInfos().hasNorms() ? "norms" : "no norms") + "; " + 
                        (mergeState.getFieldInfos().hasProx() ? "prox" : "no prox") + "; " + 
                        (mergeState.getFieldInfos().hasProx() ? "freqs" : "no freqs"));
	    	}

	    	// Very important to do this before opening the reader
	    	// because codec must know if prox was written for
	    	// this segment:
	    	final boolean useCompoundFile = useCompoundFile(merge.getCommitInfo());
	    	if (useCompoundFile) {
	    		success = false;
	    		Collection<String> filesToRemove = merge.getCommitInfo().getFileNames();

	    		try {
	    			filesToRemove = IndexWriter.createCompoundFile(getContext(), getDirectory(), 
	    					checkAbort, merge.getCommitInfo().getSegmentInfo());
	    			success = true;
	    			
	    		} catch (IOException ioe) {
	    			synchronized(this) {
	    				if (merge.isAborted()) {
	    					// This can happen if rollback or close(false)
	    					// is called -- fall through to logic below to
	    					// remove the partially created CFS:
	    				} else {
	    					handleMergeException(ioe, merge);
	    				}
	    			}
	    			
	    		} catch (Throwable t) {
	    			handleMergeException(t, merge);
	    			
	    		} finally {
	    			if (!success) {
	    				if (LOG.isDebugEnabled())
	    					LOG.debug("hit exception creating compound file during merge");

	    				synchronized(this) {
	    					getIndexWriter().getDeleter().deleteFile(
	    							getContext().getCompoundFileName(mergedName));
	    					getIndexWriter().getDeleter().deleteFile(
	    							getContext().getCompoundEntriesFileName(mergedName));
	    					getIndexWriter().getDeleter().deleteNewFiles(
	    							merge.getCommitInfo().getFileNames());
	    				}
	    			}
	    		}

	    		// So that, if we hit exc in deleteNewFiles (next)
	    		// or in commitMerge (later), we close the
	    		// per-segment readers in the finally clause below:
	    		success = false;

	    		synchronized(this) {
	    			// delete new non cfs files directly: they were never
	    			// registered with IFD
	    			getIndexWriter().getDeleter().deleteNewFiles(filesToRemove);

	    			if (merge.isAborted()) {
	    				if (LOG.isDebugEnabled())
	    					LOG.debug("abort merge after building CFS");
	    				
	    				getIndexWriter().getDeleter().deleteFile(
	    						getContext().getCompoundFileName(mergedName));
	    				getIndexWriter().getDeleter().deleteFile(
	    						getContext().getCompoundEntriesFileName(mergedName)); 
	    				
	    				return 0;
	    			}
	    		}

	    		merge.getCommitInfo().getSegmentInfo().setUseCompoundFile(true);
	    		
	    	} else {
	    		// So that, if we hit exc in commitMerge (later),
	    		// we close the per-segment readers in the finally
	    		// clause below:
	    		success = false;
	    	}

	    	// Have codec write SegmentInfo.  Must do this after
	    	// creating CFS so that 1) .si isn't slurped into CFS,
	    	// and 2) .si reflects useCompoundFile=true change
	    	// above:
	    	boolean success2 = false;
	    	try {
	    		ISegmentInfoFormat.Writer writer = (ISegmentInfoFormat.Writer)
	    				getIndexWriter().getIndexFormat().getSegmentInfoFormat().createWriter(getDirectory()); 
	    		writer.writeSegmentInfo(merge.getCommitInfo().getSegmentInfo(), 
	    				mergeState.getFieldInfos());
	    		
	    		success2 = true;
	    	} finally {
	    		if (!success2) {
	    			synchronized(this) {
	    				getIndexWriter().getDeleter().deleteNewFiles(
	    						merge.getCommitInfo().getFileNames());
	    			}
	    		}
	    	}

	    	// TODO: ideally we would freeze merge.info here!!
	    	// because any changes after writing the .si will be
	    	// lost... 
	    	if (LOG.isDebugEnabled()) { 
	    		LOG.debug(String.format(Locale.ROOT, "merged segment size=%.3f MB vs estimate=%.3f MB", 
	    				merge.getCommitInfo().getSegmentInfo().getSizeInBytes()/1024./1024., 
	    				merge.getEstimatedMergeBytes()/1024/1024.));
	    	}

	    	final IndexReaderWarmer mergedSegmentWarmer = 
	    			getIndexWriter().getIndexParams().getMergedSegmentWarmer();

	    	if (getIndexWriter().isPoolReaders() && mergedSegmentWarmer != null) {
	    		final ReadersAndLiveDocs rld = getIndexWriter().getReaderPool().get(merge.getCommitInfo(), true);
	    		final ISegmentReader sr = rld.getReader();
	    		try {
	    			mergedSegmentWarmer.warm(sr);
	    		} finally {
	    			synchronized(this) {
	    				rld.release(sr);
	    				getIndexWriter().getReaderPool().release(rld);
	    			}
	    		}
	    	}

	    	// Force READ context because we merge deletes onto
	    	// this reader:
	    	if (!commitMerge(merge)) {
	    		// commitMerge will return false if this merge was aborted
	    		return 0;
	    	}

	    	success = true;
	    } finally {
	    	// Readers are already closed in commitMerge if we didn't hit
	    	// an exc:
	    	if (!success) 
	    		closeMergeReaders(merge, true);
	    	
	    	getContext().setMergeInfo(null);
	    }

	    return merge.getCommitInfo().getSegmentInfo().getDocCount();
	}
	
	private synchronized boolean commitMerge(MergeOne merge) throws IOException {
	    if (getIndexWriter().isHitOOM()) {
	    	throw new IllegalStateException(
	    			"this writer hit an OutOfMemoryError; cannot complete merge");
	    }

	    if (LOG.isDebugEnabled()) {
	    	LOG.debug("commitMerge: " + getIndexWriter().toSegmentString(merge.getSegments()) + 
	    			  " index=" + getIndexWriter().toSegmentString());
	    }
	    
	    assert merge.isRegisterDone();

	    // If merge was explicitly aborted, or, if rollback() or
	    // rollbackTransaction() had been called since our merge
	    // started (which results in an unqualified
	    // deleter.refresh() call that will remove any index
	    // file that current segments does not reference), we
	    // abort this merge
	    if (merge.isAborted()) {
	    	if (LOG.isDebugEnabled())
	    		LOG.debug("commitMerge: skip: it was aborted");
	    	
	    	return false;
	    }

	    final ReadersAndLiveDocs mergedDeletes = 
	    		merge.getCommitInfo().getSegmentInfo().getDocCount() == 0 ? null : 
	    		commitMergedDeletes(merge);

	    assert mergedDeletes == null || mergedDeletes.getPendingDeleteCount() != 0;

	    // If the doc store we are using has been closed and
	    // is in now compound format (but wasn't when we
	    // started), then we will switch to the compound
	    // format as well:
	    assert !getSegmentInfos().contains(merge.getCommitInfo());

	    final boolean allDeleted = merge.getSegmentSize() == 0 ||
	    		merge.getCommitInfo().getSegmentInfo().getDocCount() == 0 ||
	    		(mergedDeletes != null && mergedDeletes.getPendingDeleteCount() == 
	    		merge.getCommitInfo().getSegmentInfo().getDocCount());

	    if (LOG.isDebugEnabled() && allDeleted) 
	    	LOG.debug("merged segment " + merge.getCommitInfo() + " is 100% deleted; skipping insert");

	    final boolean dropSegment = allDeleted;

	    // If we merged no segments then we better be dropping
	    // the new segment:
	    assert merge.getSegmentSize() > 0 || dropSegment;
	    assert merge.getCommitInfo().getSegmentInfo().getDocCount() != 0 || dropSegment;

	    getSegmentInfos().applyMergeChanges(merge.getCommitInfo(), merge.getSegments(), dropSegment);

	    if (mergedDeletes != null) {
	    	if (dropSegment) 
	    		mergedDeletes.dropChanges();
	    	
	    	getIndexWriter().getReaderPool().release(mergedDeletes);
	    	if (dropSegment) 
	    		getIndexWriter().getReaderPool().drop(mergedDeletes.getCommitInfo());
	    }

	    // Must close before checkpoint, otherwise IFD won't be
	    // able to delete the held-open files from the merge
	    // readers:
	    closeMergeReaders(merge, false);

	    // Must note the change to segmentInfos so any commits
	    // in-flight don't lose it:
	    getIndexWriter().checkpoint();

	    if (LOG.isDebugEnabled())
	    	LOG.debug("after commitMerge: " + getIndexWriter().toSegmentString());

	    if (merge.getMaxNumSegments() != -1 && !dropSegment) {
	    	// cascade the forceMerge:
	    	if (!mSegmentsToMerge.containsKey(merge.getCommitInfo())) 
	    		mSegmentsToMerge.put(merge.getCommitInfo(), Boolean.FALSE);
	    }

	    return true;
	}
	
	/** 
	 * Carefully merges deletes for the segments we just
	 *  merged.  This is tricky because, although merging will
	 *  clear all deletes (compacts the documents), new
	 *  deletes may have been flushed to the segments since
	 *  the merge was started.  This method "carries over"
	 *  such new deletes onto the newly merged segment, and
	 *  saves the resulting deletes file (incrementing the
	 *  delete generation for merge.info).  If no deletes were
	 *  flushed, no new deletes file is saved. 
	 */
	private synchronized ReadersAndLiveDocs commitMergedDeletes(MergeOne merge) 
			throws IOException {
	    if (LOG.isDebugEnabled())
	    	LOG.debug("commitMergeDeletes " + getIndexWriter().toSegmentString(merge.getSegments()));

	    // Carefully merge deletes that occurred after we
	    // started merging:
	    int docUpto = 0;
	    long minGen = Long.MAX_VALUE;

	    // Lazy init (only when we find a delete to carry over):
	    ReadersAndLiveDocs mergedDeletes = null;

	    for (int i=0; i < merge.getSegmentSize(); i++) {
	    	ISegmentCommitInfo info = merge.getSegmentAt(i);
	    	minGen = Math.min(info.getBufferedDeletesGen(), minGen);
	    	
	    	final int docCount = info.getSegmentInfo().getDocCount();
	    	final Bits prevLiveDocs = merge.getReaderAt(i).getLiveDocs();
	    	final Bits currentLiveDocs;
	    	final ReadersAndLiveDocs rld = getIndexWriter().getReaderPool().get(info, false);
	    	
	    	// We hold a ref so it should still be in the pool:
	    	assert rld != null: "seg=" + info.getSegmentInfo().getName();
	    	currentLiveDocs = rld.getLiveDocs();

	    	if (prevLiveDocs != null) {
	    		// If we had deletions on starting the merge we must
	    		// still have deletions now:
	    		assert currentLiveDocs != null;
	    		assert prevLiveDocs.length() == docCount;
	    		assert currentLiveDocs.length() == docCount;

	    		// There were deletes on this segment when the merge
	    		// started.  The merge has collapsed away those
	    		// deletes, but, if new deletes were flushed since
	    		// the merge started, we must now carefully keep any
	    		// newly flushed deletes but mapping them to the new
	    		// docIDs.

	    		// Since we copy-on-write, if any new deletes were
	    		// applied after merging has started, we can just
	    		// check if the before/after liveDocs have changed.
	    		// If so, we must carefully merge the liveDocs one
	    		// doc at a time:
	    		if (currentLiveDocs != prevLiveDocs) {

	    			// This means this segment received new deletes
	    			// since we started the merge, so we
	    			// must merge them:
	    			for (int j=0; j < docCount; j++) {
	    				if (!prevLiveDocs.get(j)) {
	    					assert !currentLiveDocs.get(j);
	    				} else {
	    					if (!currentLiveDocs.get(j)) {
	    						if (mergedDeletes == null) {
	    							mergedDeletes = getIndexWriter().getReaderPool().get(merge.getCommitInfo(), true);
	    							mergedDeletes.initWritableLiveDocs();
	    						}
	    						mergedDeletes.delete(docUpto);
	    					}
	    					docUpto ++;
	    				}
	    			}
	    		} else {
	    			docUpto += info.getSegmentInfo().getDocCount() - info.getDelCount() 
	    					- rld.getPendingDeleteCount();
	    		}
	    		
	      } else if (currentLiveDocs != null) {
	    	  assert currentLiveDocs.length() == docCount;
	    	  // This segment had no deletes before but now it
	    	  // does:
	    	  
	    	  for (int j=0; j < docCount; j++) {
	    		  if (!currentLiveDocs.get(j)) {
	    			  if (mergedDeletes == null) {
	    				  mergedDeletes = getIndexWriter().getReaderPool().get(merge.getCommitInfo(), true);
	    				  mergedDeletes.initWritableLiveDocs();
	    			  }
	    			  mergedDeletes.delete(docUpto);
	    		  }
	    		  docUpto ++;
	    	  }
	      	} else {
	      		// No deletes before or after
	      		docUpto += info.getSegmentInfo().getDocCount();
	      	}
	    }

	    assert docUpto == merge.getCommitInfo().getSegmentInfo().getDocCount();
	    if (LOG.isDebugEnabled()) { 
	    	if (mergedDeletes != null) 
	    		LOG.debug(mergedDeletes.getPendingDeleteCount() + " new deletes since merge started");
	    	else 
	    		LOG.debug("no new deletes since merge started");
	    }

	    // If new deletes were applied while we were merging
	    // (which happens if eg commit() or getReader() is
	    // called during our merge), then it better be the case
	    // that the delGen has increased for all our merged
	    // segments:
	    assert mergedDeletes == null || minGen > merge.getCommitInfo().getBufferedDeletesGen();

	    merge.getCommitInfo().setBufferedDeletesGen(minGen);

	    return mergedDeletes;
	}
	
	private final synchronized void closeMergeReaders(MergeOne merge, 
  			boolean suppressExceptions) throws IOException {
  		final int numSegments = merge.getReaderSize();
  		
  		Throwable th = null;
  		boolean drop = !suppressExceptions;
    
  		for (int i = 0; i < numSegments; i++) {
  			final ISegmentReader sr = merge.getReaderAt(i);
  			if (sr != null) {
  				try {
  					final ReadersAndLiveDocs rld = getIndexWriter().getReaderPool().get(sr.getCommitInfo(), false);
  					// We still hold a ref so it should not have been removed:
  					assert rld != null;
  					if (drop) 
  						rld.dropChanges();
  					
  					rld.release(sr);
  					getIndexWriter().getReaderPool().release(rld);
  					if (drop) 
  						getIndexWriter().getReaderPool().drop(rld.getCommitInfo());
  					
  				} catch (Throwable t) {
  					if (th == null) 
  						th = t;
  				}
  				
  				merge.setReaderAt(i, null);
  			}
  		}
    
  		// If any error occured, throw it.
  		if (!suppressExceptions && th != null) {
  			if (th instanceof IOException) throw (IOException) th;
  			if (th instanceof RuntimeException) throw (RuntimeException) th;
  			if (th instanceof Error) throw (Error) th;
  			throw new RuntimeException(th);
  		}
  	}
	
}
