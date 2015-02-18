package org.javenstudio.hornet.index;

import java.io.IOException;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.indexdb.index.MergeOne;
import org.javenstudio.common.indexdb.index.MergePolicy;
import org.javenstudio.common.indexdb.index.MergeSpecification;
import org.javenstudio.common.indexdb.index.segment.SegmentCommitInfo;

/**
 *  Merges segments of approximately equal size, subject to
 *  an allowed number of segments per tier.  This is similar
 *  to {@link LogByteSizeMergePolicy}, except this merge
 *  policy is able to merge non-adjacent segment, and
 *  separates how many segments are merged at once ({@link
 *  #setMaxMergeAtOnce}) from how many segments are allowed
 *  per tier ({@link #setSegmentsPerTier}).  This merge
 *  policy also does not over-merge (i.e. cascade merges). 
 *
 *  <p>For normal merging, this policy first computes a
 *  "budget" of how many segments are allowed to be in the
 *  index.  If the index is over-budget, then the policy
 *  sorts segments by decreasing size (pro-rating by percent
 *  deletes), and then finds the least-cost merge.  Merge
 *  cost is measured by a combination of the "skew" of the
 *  merge (size of largest segment divided by smallest segment),
 *  total merge size and percent deletes reclaimed,
 *  so that merges with lower skew, smaller size
 *  and those reclaiming more deletes, are
 *  favored.
 *
 *  <p>If a merge will produce a segment that's larger than
 *  {@link #setMaxMergedSegmentMB}, then the policy will
 *  merge fewer segments (down to 1 at once, if that one has
 *  deletions) to keep the segment size under budget.
 *      
 *  <p><b>NOTE</b>: this policy freely merges non-adjacent
 *  segments; if this is a problem, use {@link
 *  LogMergePolicy}.
 *
 *  <p><b>NOTE</b>: This policy always merges by byte size
 *  of the segments, always pro-rates by percent deletes,
 *  and does not apply any maximum segment size during
 *  forceMerge (unlike {@link LogByteSizeMergePolicy}).
 *
 *  - we could try to take into account whether a large
 *     merge is already running (under CMS) and then bias
 *     ourselves towards picking smaller merges if so (or,
 *     maybe CMS should do so)
 */
final class TieredMergePolicy extends MergePolicy {

	private int mMaxMergeAtOnce = 10;
	private long mMaxMergedSegmentBytes = 5*1024*1024*1024L;
	private int mMaxMergeAtOnceExplicit = 30;

	private long mFloorSegmentBytes = 2*1024*1024L;
	private double mSegsPerTier = 10.0;
	private double mForceMergeDeletesPctAllowed = 10.0;
	private boolean mUseCompoundFile = true;
	private double mNoCFSRatio = 0.1;
	private double mReclaimDeletesWeight = 2.0;

	public TieredMergePolicy() {}
  
	/** 
	 * Maximum number of segments to be merged at a time
	 *  during "normal" merging.  For explicit merging (eg,
	 *  forceMerge or forceMergeDeletes was called), see {@link
	 *  #setMaxMergeAtOnceExplicit}.  Default is 10. 
	 */
	public TieredMergePolicy setMaxMergeAtOnce(int v) {
		if (v < 2) 
			throw new IllegalArgumentException("maxMergeAtOnce must be > 1 (got " + v + ")");
		
		mMaxMergeAtOnce = v;
		return this;
	}

	/** @see #setMaxMergeAtOnce */
	public int getMaxMergeAtOnce() {
		return mMaxMergeAtOnce;
	}

	// TODO: should addIndexes do explicit merging, too?  And,
	// if user calls IW.maybeMerge "explicitly"

	/** 
	 * Maximum number of segments to be merged at a time,
	 *  during forceMerge or forceMergeDeletes. Default is 30. 
	 */
	public TieredMergePolicy setMaxMergeAtOnceExplicit(int v) {
		if (v < 2) 
			throw new IllegalArgumentException("maxMergeAtOnceExplicit must be > 1 (got " + v + ")");
		
		mMaxMergeAtOnceExplicit = v;
		return this;
	}

	/** @see #setMaxMergeAtOnceExplicit */
	public int getMaxMergeAtOnceExplicit() {
		return mMaxMergeAtOnceExplicit;
	}

	/** 
	 * Maximum sized segment to produce during
	 *  normal merging.  This setting is approximate: the
	 *  estimate of the merged segment size is made by summing
	 *  sizes of to-be-merged segments (compensating for
	 *  percent deleted docs).  Default is 5 GB. 
	 */
	public TieredMergePolicy setMaxMergedSegmentMB(double v) {
		mMaxMergedSegmentBytes = (long) (v*1024*1024);
		return this;
	}

	/** @see #getMaxMergedSegmentMB */
	public double getMaxMergedSegmentMB() {
		return mMaxMergedSegmentBytes/1024/1024.;
	}

	/** 
	 * Controls how aggressively merges that reclaim more
	 *  deletions are favored.  Higher values favor selecting
	 *  merges that reclaim deletions.  A value of 0.0 means
	 *  deletions don't impact merge selection. 
	 */
	public TieredMergePolicy setReclaimDeletesWeight(double v) {
		if (v < 0.0) 
			throw new IllegalArgumentException("reclaimDeletesWeight must be >= 0.0 (got " + v + ")");
		
		mReclaimDeletesWeight = v;
		return this;
	}

	/** See {@link #setReclaimDeletesWeight}. */
	public double getReclaimDeletesWeight() {
		return mReclaimDeletesWeight;
	}

	/** 
	 * Segments smaller than this are "rounded up" to this
	 *  size, ie treated as equal (floor) size for merge
	 *  selection.  This is to prevent frequent flushing of
	 *  tiny segments from allowing a long tail in the index.
	 *  Default is 2 MB. 
	 */
	public TieredMergePolicy setFloorSegmentMB(double v) {
		if (v <= 0.0) 
			throw new IllegalArgumentException("floorSegmentMB must be >= 0.0 (got " + v + ")");
		
		mFloorSegmentBytes = (long) (v*1024*1024);
		return this;
	}

	/** @see #setFloorSegmentMB */
	public double getFloorSegmentMB() {
		return mFloorSegmentBytes/1024*1024.;
	}

	/** 
	 * When forceMergeDeletes is called, we only merge away a
	 *  segment if its delete percentage is over this
	 *  threshold.  Default is 10%. 
	 */ 
	public TieredMergePolicy setForceMergeDeletesPctAllowed(double v) {
		if (v < 0.0 || v > 100.0) {
			throw new IllegalArgumentException("forceMergeDeletesPctAllowed must be " 
					+ "between 0.0 and 100.0 inclusive (got " + v + ")");
		}
		
		mForceMergeDeletesPctAllowed = v;
		return this;
	}

	/** @see #setForceMergeDeletesPctAllowed */
	public double getForceMergeDeletesPctAllowed() {
		return mForceMergeDeletesPctAllowed;
	}

	/** 
	 * Sets the allowed number of segments per tier.  Smaller
	 *  values mean more merging but fewer segments.
	 *
	 *  <p><b>NOTE</b>: this value should be >= the {@link
	 *  #setMaxMergeAtOnce} otherwise you'll force too much
	 *  merging to occur.</p>
	 *
	 *  <p>Default is 10.0.</p> 
	 */
	public TieredMergePolicy setSegmentsPerTier(double v) {
		if (v < 2.0) 
			throw new IllegalArgumentException("segmentsPerTier must be >= 2.0 (got " + v + ")");
		
		mSegsPerTier = v;
		return this;
	}

	/** @see #setSegmentsPerTier */
	public double getSegmentsPerTier() {
		return mSegsPerTier;
	}

	/** 
	 * Sets whether compound file format should be used for
	 *  newly flushed and newly merged segments.  Default
	 *  true. 
	 */
	public TieredMergePolicy setUseCompoundFile(boolean useCompoundFile) {
		mUseCompoundFile = useCompoundFile;
		return this;
	}

	/** @see  #setUseCompoundFile */
	public boolean getUseCompoundFile() {
		return mUseCompoundFile;
	}

	/** 
	 * If a merged segment will be more than this percentage
	 *  of the total size of the index, leave the segment as
	 *  non-compound file even if compound file is enabled.
	 *  Set to 1.0 to always use CFS regardless of merge
	 *  size.  Default is 0.1. 
	 */
	public TieredMergePolicy setNoCFSRatio(double noCFSRatio) {
		if (noCFSRatio < 0.0 || noCFSRatio > 1.0) 
			throw new IllegalArgumentException("noCFSRatio must be 0.0 to 1.0 inclusive; got " + noCFSRatio);
		
		mNoCFSRatio = noCFSRatio;
		return this;
	}
  
	/** @see #setNoCFSRatio */
	public double getNoCFSRatio() {
		return mNoCFSRatio;
	}

	private class SegmentByteSizeDescending implements Comparator<ISegmentCommitInfo> {
		@Override
		public int compare(ISegmentCommitInfo o1, ISegmentCommitInfo o2) {
			try {
				final long sz1 = size(o1);
				final long sz2 = size(o2);
				if (sz1 > sz2) {
					return -1;
				} else if (sz2 > sz1) {
					return 1;
				} else {
					return o1.getSegmentInfo().getName().compareTo(o2.getSegmentInfo().getName());
				}
			} catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	/** Holds score and explanation for a single candidate merge. */
	protected static abstract class MergeScore {
		public abstract double getScore();
		public abstract String getExplanation();
	}

	@Override
	public MergeSpecification findMerges(ISegmentInfos infos) throws IOException {
		if (infos.size() == 0) 
			return null;
		
		//final Collection<ISegmentCommitInfo> merging = getIndexWriter().getMergingSegments();
		final Collection<ISegmentCommitInfo> toBeMerged = new HashSet<ISegmentCommitInfo>();

		final List<ISegmentCommitInfo> infosSorted = new ArrayList<ISegmentCommitInfo>(infos.asList());
		Collections.sort(infosSorted, new SegmentByteSizeDescending());

		// Compute total index bytes & print details about the index
		long totIndexBytes = 0;
		long minSegmentBytes = Long.MAX_VALUE;
		
		for (ISegmentCommitInfo info : infosSorted) {
			final long segBytes = size(info);
			minSegmentBytes = Math.min(segBytes, minSegmentBytes);
			// Accum total byte size
			totIndexBytes += segBytes;
		}

		// If we have too-large segments, grace them out
		// of the maxSegmentCount:
		int tooBigCount = 0;
		while (tooBigCount < infosSorted.size() && size(infosSorted.get(tooBigCount)) 
				>= mMaxMergedSegmentBytes/2.0) {
			totIndexBytes -= size(infosSorted.get(tooBigCount));
			tooBigCount ++;
		}

		minSegmentBytes = floorSize(minSegmentBytes);

		// Compute max allowed segs in the index
		long levelSize = minSegmentBytes;
		long bytesLeft = totIndexBytes;
		double allowedSegCount = 0;
		
		while (true) {
			final double segCountLevel = bytesLeft / (double) levelSize;
			if (segCountLevel < mSegsPerTier) {
				allowedSegCount += Math.ceil(segCountLevel);
				break;
			}
			allowedSegCount += mSegsPerTier;
			bytesLeft -= mSegsPerTier * levelSize;
			levelSize *= mMaxMergeAtOnce;
		}
		
		int allowedSegCountInt = (int) allowedSegCount;
		MergeSpecification spec = null;

		// Cycle to possibly select more than one merge:
		while (true) {
			long mergingBytes = 0;

			// Gather eligible segments for merging, ie segments
			// not already being merged and not already picked (by
			// prior iteration of this loop) for merging:
			final List<ISegmentCommitInfo> eligible = new ArrayList<ISegmentCommitInfo>();
			
			for (int idx = tooBigCount; idx<infosSorted.size(); idx++) {
				final ISegmentCommitInfo info = infosSorted.get(idx);
				if (getIndexWriter().getMergeControl().containsMergingSegment(info)) {
					mergingBytes += info.getSegmentInfo().getSizeInBytes();
				} else if (!toBeMerged.contains(info)) {
					eligible.add(info);
				}
			}

			final boolean maxMergeIsRunning = mergingBytes >= mMaxMergedSegmentBytes;
			if (eligible.size() == 0) 
				return spec;

			if (eligible.size() >= allowedSegCountInt) {
				// OK we are over budget -- find best merge!
				MergeScore bestScore = null;
				List<ISegmentCommitInfo> best = null;
				
				//boolean bestTooLarge = false;
				//long bestMergeBytes = 0;

				// Consider all merge starts:
				for (int startIdx = 0; startIdx <= eligible.size()-mMaxMergeAtOnce; startIdx++) {
					final List<ISegmentCommitInfo> candidate = new ArrayList<ISegmentCommitInfo>();
					long totAfterMergeBytes = 0;
					boolean hitTooLarge = false;
					
					for (int idx = startIdx; idx < eligible.size() && candidate.size() < mMaxMergeAtOnce; idx++) {
						final ISegmentCommitInfo info = eligible.get(idx);
						final long segBytes = size(info);

						if (totAfterMergeBytes + segBytes > mMaxMergedSegmentBytes) {
							hitTooLarge = true;
							// NOTE: we continue, so that we can try
							// "packing" smaller segments into this merge
							// to see if we can get closer to the max
							// size; this in general is not perfect since
							// this is really "bin packing" and we'd have
							// to try different permutations.
							continue;
						}
						
						candidate.add(info);
						totAfterMergeBytes += segBytes;
					}

					final MergeScore score = score(candidate, hitTooLarge, mergingBytes);

					// If we are already running a max sized merge
					// (maxMergeIsRunning), don't allow another max
					// sized merge to kick off:
					if ((bestScore == null || score.getScore() < bestScore.getScore()) && 
						(!hitTooLarge || !maxMergeIsRunning)) {
						best = candidate;
						bestScore = score;
						//bestTooLarge = hitTooLarge;
						//bestMergeBytes = totAfterMergeBytes;
					}
				}
        
				if (best != null) {
					if (spec == null) 
						spec = new MergeSpecification();
					
					final MergeOne merge = new MergeOne(best);
					spec.add(merge);
					
					for (int i=0; i < merge.getSegmentSize(); i++) {
						ISegmentCommitInfo info = merge.getSegmentAt(i);
						toBeMerged.add(info);
					}
				} else 
					return spec;
				
			} else 
				return spec;
		}
	}

	/** Expert: scores one merge; subclasses can override. */
	protected MergeScore score(List<ISegmentCommitInfo> candidate, 
			boolean hitTooLarge, long mergingBytes) throws IOException {
		long totBeforeMergeBytes = 0;
		long totAfterMergeBytes = 0;
		long totAfterMergeBytesFloored = 0;
		
		for (ISegmentCommitInfo info : candidate) {
			final long segBytes = size(info);
			totAfterMergeBytes += segBytes;
			totAfterMergeBytesFloored += floorSize(segBytes);
			totBeforeMergeBytes += info.getSegmentInfo().getSizeInBytes();
		}

		// Measure "skew" of the merge, which can range
		// from 1.0/numSegsBeingMerged (good) to 1.0 (poor):
		final double skew;
		
		if (hitTooLarge) {
			// Pretend the merge has perfect skew; skew doesn't
			// matter in this case because this merge will not
			// "cascade" and so it cannot lead to N^2 merge cost
			// over time:
			skew = 1.0/mMaxMergeAtOnce;
		} else {
			skew = ((double) floorSize(size(candidate.get(0))))/totAfterMergeBytesFloored;
		}

		// Strongly favor merges with less skew (smaller
		// mergeScore is better):
		double mergeScore = skew;

		// Gently favor smaller merges over bigger ones.  We
		// don't want to make this exponent too large else we
		// can end up doing poor merges of small segments in
		// order to avoid the large merges:
		mergeScore *= Math.pow(totAfterMergeBytes, 0.05);

		// Strongly favor merges that reclaim deletes:
		final double nonDelRatio = ((double) totAfterMergeBytes)/totBeforeMergeBytes;
		mergeScore *= Math.pow(nonDelRatio, mReclaimDeletesWeight);

		final double finalMergeScore = mergeScore;

		return new MergeScore() {
				@Override
				public double getScore() {
					return finalMergeScore;
				}
				@Override
				public String getExplanation() {
					return "skew=" + String.format("%.3f", skew) + " nonDelRatio=" + String.format("%.3f", nonDelRatio);
				}
			};
	}

	@Override
	public MergeSpecification findForcedMerges(ISegmentInfos infos, int maxSegmentCount, 
			Map<ISegmentCommitInfo,Boolean> segmentsToMerge) throws IOException {
		final List<ISegmentCommitInfo> eligible = new ArrayList<ISegmentCommitInfo>();
		//final Collection<ISegmentCommitInfo> merging = getIndexWriter().getMergingSegments();
		
		boolean segmentIsOriginal = false;
		boolean forceMergeRunning = false;
		
		for (ISegmentCommitInfo info : infos) {
			final Boolean isOriginal = segmentsToMerge.get(info);
			if (isOriginal != null) {
				segmentIsOriginal = isOriginal;
				if (!getIndexWriter().getMergeControl().containsMergingSegment(info)) 
					eligible.add(info);
				else 
					forceMergeRunning = true;
			}
		}

		if (eligible.size() == 0) 
			return null;

		if ((maxSegmentCount > 1 && eligible.size() <= maxSegmentCount) ||
			(maxSegmentCount == 1 && eligible.size() == 1 && (!segmentIsOriginal || isMerged(eligible.get(0))))) {
			return null;
		}

		Collections.sort(eligible, new SegmentByteSizeDescending());
		int end = eligible.size();
    
		MergeSpecification spec = null;

		// Do full merges, first, backwards:
		while (end >= mMaxMergeAtOnceExplicit + maxSegmentCount - 1) {
			if (spec == null) 
				spec = new MergeSpecification();
			
			final MergeOne merge = new MergeOne(eligible.subList(end-mMaxMergeAtOnceExplicit, end));
			spec.add(merge);
			
			end -= mMaxMergeAtOnceExplicit;
		}

		if (spec == null && !forceMergeRunning) {
			// Do final merge
			final int numToMerge = end - maxSegmentCount + 1;
			final MergeOne merge = new MergeOne(eligible.subList(end-numToMerge, end));
			spec = new MergeSpecification();
			spec.add(merge);
		}

		return spec;
	}

	@Override
	public MergeSpecification findForcedDeletesMerges(ISegmentInfos infos)
			throws CorruptIndexException, IOException {
		final List<ISegmentCommitInfo> eligible = new ArrayList<ISegmentCommitInfo>();
		//final Collection<ISegmentCommitInfo> merging = getIndexWriter().getMergingSegments();
		
		for (ISegmentCommitInfo info : infos) {
			double pctDeletes = 100*((double) getIndexWriter().getNumDeletedDocs(info))/info.getSegmentInfo().getDocCount();
			if (pctDeletes > mForceMergeDeletesPctAllowed && 
				!getIndexWriter().getMergeControl().containsMergingSegment(info)) 
				eligible.add((SegmentCommitInfo)info);
		}

		if (eligible.size() == 0) 
			return null;

		Collections.sort(eligible, new SegmentByteSizeDescending());

		MergeSpecification spec = null;
		int start = 0;

		while (start < eligible.size()) {
			// Don't enforce max merged size here: app is explicitly
			// calling forceMergeDeletes, and knows this may take a
			// long time / produce big segments (like forceMerge):
			final int end = Math.min(start + mMaxMergeAtOnceExplicit, eligible.size());
			
			if (spec == null) 
				spec = new MergeSpecification();

			final MergeOne merge = new MergeOne(eligible.subList(start, end));
			spec.add(merge);
			
			start = end;
		}	

		return spec;
	}

	@Override
	public boolean useCompoundFile(ISegmentInfos infos, ISegmentCommitInfo mergedInfo) 
			throws IOException {
		final boolean doCFS;

		if (!mUseCompoundFile) {
			doCFS = false;
			
		} else if (mNoCFSRatio == 1.0) {
			doCFS = true;
			
		} else {
			long totalSize = 0;
			for (ISegmentCommitInfo info : infos) {
				totalSize += size(info);
			}

			doCFS = (size(mergedInfo) <= mNoCFSRatio * totalSize);
		}
		
		return doCFS;
	}

	@Override
	public void close() {
		//do nothing
	}

	private boolean isMerged(ISegmentCommitInfo info) throws IOException {
		IndexWriter w = getIndexWriter();
		assert w != null;
		
		boolean hasDeletions = w.getNumDeletedDocs(info) > 0;
		return !hasDeletions &&
				//!info.getSegmentInfo().hasSeparateNorms() &&
				info.getSegmentInfo().getDirectory() == w.getDirectory() &&
				(info.getSegmentInfo().getUseCompoundFile() == mUseCompoundFile || mNoCFSRatio < 1.0);
	}

	// Segment size in bytes, pro-rated by % deleted
	private long size(ISegmentCommitInfo info) throws IOException {
		final long byteSize = info.getSegmentInfo().getSizeInBytes();    
		final int delCount = getIndexWriter().getNumDeletedDocs(info);
		final double delRatio = (info.getSegmentInfo().getDocCount() <= 0 ? 0.0f : 
			((double)delCount / (double)info.getSegmentInfo().getDocCount()));
		
		assert delRatio <= 1.0;
		return (long) (byteSize * (1.0-delRatio));
	}

	private long floorSize(long bytes) {
		return Math.max(mFloorSegmentBytes, bytes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append("{maxMergeAtOnce=").append(mMaxMergeAtOnce).append(", ");
		sb.append("maxMergeAtOnceExplicit=").append(mMaxMergeAtOnceExplicit).append(", ");
		sb.append("maxMergedSegmentMB=").append(mMaxMergedSegmentBytes/1024/1024.).append(", ");
		sb.append("floorSegmentMB=").append(mFloorSegmentBytes/1024/1024.).append(", ");
		sb.append("forceMergeDeletesPctAllowed=").append(mForceMergeDeletesPctAllowed).append(", ");
		sb.append("segmentsPerTier=").append(mSegsPerTier).append(", ");
		sb.append("useCompoundFile=").append(mUseCompoundFile).append(", ");
		sb.append("noCFSRatio=").append(mNoCFSRatio).append("}");
		return sb.toString();
	}
	
}
