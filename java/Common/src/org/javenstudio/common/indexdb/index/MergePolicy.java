package org.javenstudio.common.indexdb.index;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.util.SetOnce;

/**
 * <p>Expert: a MergePolicy determines the sequence of
 * primitive merge operations.</p>
 * 
 * <p>Whenever the segments in an index have been altered by
 * {@link IndexWriter}, either the addition of a newly
 * flushed segment, addition of many segments from
 * addIndexes* calls, or a previous merge that may now need
 * to cascade, {@link IndexWriter} invokes {@link
 * #findMerges} to give the MergePolicy a chance to pick
 * merges that are now required.  This method returns a
 * {@link MergeSpecification} instance describing the set of
 * merges that should be done, or null if no merges are
 * necessary.  When IndexWriter.forceMerge is called, it calls
 * {@link #findForcedMerges(SegmentInfos,int,Map)} and the MergePolicy should
 * then return the necessary merges.</p>
 *
 * <p>Note that the policy can return more than one merge at
 * a time.  In this case, if the writer is using {@link
 * SerialMergeScheduler}, the merges will be run
 * sequentially but if it is using {@link
 * ConcurrentMergeScheduler} they will be run concurrently.</p>
 * 
 * <p>The default MergePolicy is {@link
 * TieredMergePolicy}.</p>
 *
 */
public abstract class MergePolicy implements Closeable, Cloneable {

	protected SetOnce<IndexWriter> mWriter;
	
	/**
	 * Creates a new merge policy instance. Note that if you intend to use it
	 * without passing it to {@link IndexWriter}, you should call
	 * {@link #setIndexWriter(IndexWriter)}.
	 */
	protected MergePolicy() {
		mWriter = new SetOnce<IndexWriter>();
	}

	@Override
	public MergePolicy clone() {
		MergePolicy clone;
		try {
			clone = (MergePolicy) super.clone();
		} catch (CloneNotSupportedException e) {
			// should not happen
			throw new RuntimeException(e);
		}
		clone.mWriter = new SetOnce<IndexWriter>();
		return clone;
	}
	
	/**
	 * Sets the {@link IndexWriter} to use by this merge policy. This method is
	 * allowed to be called only once, and is usually set by IndexWriter. If it is
	 * called more than once, {@link AlreadySetException} is thrown.
	 * 
	 * @see SetOnce
	 */
	public void setIndexWriter(IndexWriter writer) {
		mWriter.set(writer);
	}
	
	protected IndexWriter getIndexWriter() { 
		return mWriter.get();
	}
	
	/**
	 * Determine what set of merge operations are now necessary on the index.
	 * {@link IndexWriter} calls this whenever there is a change to the segments.
	 * This call is always synchronized on the {@link IndexWriter} instance so
	 * only one thread at a time will call this method.
	 * 
	 * @param segmentInfos
	 *          the total set of segments in the index
	 */
	public abstract MergeSpecification findMerges(ISegmentInfos segmentInfos)
			throws CorruptIndexException, IOException;

	/**
	 * Determine what set of merge operations is necessary in
	 * order to merge to <= the specified segment count. {@link IndexWriter} calls this when its
	 * {@link IndexWriter#forceMerge} method is called. This call is always
	 * synchronized on the {@link IndexWriter} instance so only one thread at a
	 * time will call this method.
	 * 
	 * @param segmentInfos
	 *          the total set of segments in the index
	 * @param maxSegmentCount
	 *          requested maximum number of segments in the index (currently this
	 *          is always 1)
	 * @param segmentsToMerge
	 *          contains the specific SegmentInfo instances that must be merged
	 *          away. This may be a subset of all
	 *          SegmentInfos.  If the value is True for a
	 *          given SegmentInfo, that means this segment was
	 *          an original segment present in the
	 *          to-be-merged index; else, it was a segment
	 *          produced by a cascaded merge.
	 */
	public abstract MergeSpecification findForcedMerges(
			ISegmentInfos segmentInfos, int maxSegmentCount, Map<ISegmentCommitInfo,Boolean> segmentsToMerge)
			throws CorruptIndexException, IOException;

	/**
	 * Determine what set of merge operations is necessary in order to expunge all
	 * deletes from the index.
	 * 
	 * @param segmentInfos
	 *          the total set of segments in the index
	 */
	public abstract MergeSpecification findForcedDeletesMerges(ISegmentInfos segmentInfos) 
			throws CorruptIndexException, IOException;

	/**
	 * Returns true if a new segment (regardless of its origin) should use the compound file format.
	 */
	public abstract boolean useCompoundFile(ISegmentInfos segments, ISegmentCommitInfo newSegment) 
			throws IOException;
	
	/**
	 * Release all resources for the policy.
	 */
	public abstract void close();
	
}
