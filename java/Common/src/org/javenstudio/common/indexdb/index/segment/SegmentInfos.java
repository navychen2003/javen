package org.javenstudio.common.indexdb.index.segment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.index.IndexFileNames;
import org.javenstudio.common.indexdb.index.IndexWriter;
import org.javenstudio.common.util.Logger;

/**
 * A collection of segmentInfo objects with methods for operating on
 * those segments in relation to the file system.
 * <p>
 * The active segments in the index are stored in the segment info file,
 * <tt>segments_N</tt>. There may be one or more <tt>segments_N</tt> files in the
 * index; however, the one with the largest generation is the active one (when
 * older segments_N files are present it's because they temporarily cannot be
 * deleted, or, a writer is in the process of committing, or a custom 
 * {@link IndexDeletionPolicy IndexDeletionPolicy}
 * is in use). This file lists each segment by name and has details about the
 * codec and generation of deletes.
 * </p>
 * <p>There is also a file <tt>segments.gen</tt>. This file contains
 * the current generation (the <tt>_N</tt> in <tt>segments_N</tt>) of the index.
 * This is used only as a fallback in case the current generation cannot be
 * accurately determined by directory listing alone (as is the case for some NFS
 * clients with time-based directory cache expiration). This file simply contains
 * an {@link DataOutput#writeInt Int32} version header 
 * ({@link #FORMAT_SEGMENTS_GEN_CURRENT}), followed by the
 * generation recorded as {@link DataOutput#writeLong Int64}, written twice.</p>
 * <p>
 * Files:
 * <ul>
 *   <li><tt>segments.gen</tt>: GenHeader, Generation, Generation
 *   <li><tt>segments_N</tt>: Header, Version, NameCounter, SegCount,
 *    &lt;SegName, SegCodec, DelGen, DeletionCount&gt;<sup>SegCount</sup>, 
 *    CommitUserData, Checksum
 * </ul>
 * </p>
 * Data types:
 * <p>
 * <ul>
 *   <li>Header --&gt; {@link CodecUtil#writeHeader CodecHeader}</li>
 *   <li>GenHeader, NameCounter, SegCount, DeletionCount 
 *   --&gt; {@link DataOutput#writeInt Int32}</li>
 *   <li>Generation, Version, DelGen, Checksum 
 *   --&gt; {@link DataOutput#writeLong Int64}</li>
 *   <li>SegName, SegCodec --&gt; {@link DataOutput#writeString String}</li>
 *   <li>CommitUserData --&gt; 
 *   {@link DataOutput#writeStringStringMap Map&lt;String,String&gt;}</li>
 * </ul>
 * </p>
 * Field Descriptions:
 * <p>
 * <ul>
 *   <li>Version counts how often the index has been changed by adding or deleting
 *       documents.</li>
 *   <li>NameCounter is used to generate names for new segment files.</li>
 *   <li>SegName is the name of the segment, and is used as the file name prefix for
 *       all of the files that compose the segment's index.</li>
 *   <li>DelGen is the generation count of the deletes file. If this is -1,
 *       there are no deletes. Anything above zero means there are deletes 
 *       stored by {@link LiveDocsFormat}.</li>
 *   <li>DeletionCount records the number of deleted documents in this segment.</li>
 *   <li>Checksum contains the CRC32 checksum of all bytes in the segments_N file up
 *       until the checksum. This is used to verify integrity of the file on opening the
 *       index.</li>
 *   <li>SegCodec is the {@link Codec#getName() name} of the Codec that encoded
 *       this segment.</li>
 *   <li>CommitUserData stores an optional user-supplied opaque
 *       Map&lt;String,String&gt; that was passed to 
 *       {@link IndexWriter#commit(java.util.Map)} 
 *       or {@link IndexWriter#prepareCommit(java.util.Map)}.</li>
 * </ul>
 * </p>
 */
public class SegmentInfos implements ISegmentInfos {
	static final Logger LOG = Logger.getLogger(SegmentInfos.class);

	// Opaque Map<String, String> that user can specify during IndexWriter.commit
	private Map<String,String> mUserData = 
			Collections.<String,String>emptyMap();
  
	private List<ISegmentCommitInfo> mSegments = 
			new ArrayList<ISegmentCommitInfo>();
	
	private final IDirectory mDirectory;
	
	// used to name new segments
	private int mCounter; 
	  
	// counts how often the index has been changed
	private long mVersion;

	// generation of the "segments_N" for the next commit
	private long mGeneration;
	
	// generation of the "segments_N" file we last successfully read
	// or wrote; this is normally the same as generation except if
	// there was an IOException that had interrupted a commit
	private long mLastGeneration;
	
	public SegmentInfos(IDirectory dir) { 
		if (dir == null) 
			throw new NullPointerException("Directory is null");
		
		mDirectory = dir;
		
		if (LOG.isDebugEnabled())
			LOG.debug("created with directory: " + dir);
	}
	
	@Override
	public final IDirectory getDirectory() { return mDirectory; }
	
	public final int getCounter() { return mCounter; }
	
	public final void setGeneration(long gen) { mGeneration = gen; }
	public final void setLastGeneration(long gen) { mLastGeneration = gen; }
	
	public final void setVersion(long ver) { mVersion = ver; }
	public final void setCounter(int count) { mCounter = count; }
	
	public final void increaseGeneration(long count) { 
		mGeneration += count;
	}
	
	public final void increaseCounter(int count) { 
		mCounter += count; 
	}
	
	@Override
	public ISegmentCommitInfo getCommitInfo(int i) {
		return mSegments.get(i);
	}
  
	/**
	 * Get the segments_N filename in use by this segment infos.
	 */
	@Override
	public String getSegmentsFileName() {
		return IndexFileNames.getFileNameFromGeneration(
				IndexFileNames.SEGMENTS, "", mLastGeneration);
	}
  
	/**
	 * Get the next segments_N filename that will be written.
	 */
	@Override
	public String getNextSegmentFileName() {
		long nextGeneration;

		if (mGeneration == -1) 
			nextGeneration = 1;
		else 
			nextGeneration = mGeneration+1;
		
		return IndexFileNames.getFileNameFromGeneration(
				IndexFileNames.SEGMENTS, "", nextGeneration);
	}
  
	/** 
	 * Returns all file names referenced by SegmentInfo
	 *  instances matching the provided Directory (ie files
	 *  associated with any "external" segments are skipped).
	 *  The returned collection is recomputed on each
	 *  invocation.
	 */
	@Override
	public Collection<String> getFileNames(boolean includeSegmentsFile) throws IOException {
		HashSet<String> files = new HashSet<String>();
		
		if (includeSegmentsFile) {
			final String segmentFileName = getSegmentsFileName();
			if (segmentFileName != null) {
				/**
				 * TODO: if lastGen == -1 we get might get null here it seems wrong to
				 * add null to the files set
				 */
				files.add(segmentFileName);
			}
		}
		
		final int size = size();
		for (int i=0; i < size; i++) {
			final ISegmentCommitInfo info = getCommitInfo(i);
			assert info.getSegmentInfo().getDirectory() == mDirectory;
			
			if (info.getSegmentInfo().getDirectory() == mDirectory) 
				files.addAll(info.getFileNames());
		}
		
		return files;
	}
	
	/**
	 * Returns a copy of this instance, also copying each
	 * SegmentInfo.
	 */
	@Override
	public SegmentInfos clone() {
		try {
			final SegmentInfos sis = (SegmentInfos) super.clone();
			sis.copyFrom(this);
			
			return sis;
			
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("should not happen", e);
		}
	}

	public void copyFrom(SegmentInfos infos) { 
		mCounter = infos.mCounter;
		mVersion = infos.mVersion;
		mGeneration = infos.mGeneration;
		mLastGeneration = infos.mLastGeneration;
		
		// deep clone, first recreate all collections:
		mSegments = new ArrayList<ISegmentCommitInfo>(infos.size());
		
		for (final ISegmentCommitInfo info : infos) {
			//assert info.info.getCodec() != null;
			// dont directly access segments, use add method!!!
			add(info.clone());
		}
		
		mUserData = new HashMap<String,String>(mUserData);
	}
	
	/**
	 * version number when this SegmentInfos was generated.
	 */
	@Override
	public long getVersion() {
		return mVersion;
	}
  
	@Override
	public long getGeneration() {
		return mGeneration;
	}
  
	public long getLastGeneration() {
		return mLastGeneration;
	}
  
	// Carry over generation numbers from another SegmentInfos
	public void updateGeneration(ISegmentInfos other) {
		mLastGeneration = ((SegmentInfos)other).getLastGeneration();
		mGeneration = other.getGeneration();
	}

	@Override
	public Map<String,String> getUserData() {
		return mUserData;
	}

	public final void setUserData(Map<String,String> data) {
		if (data == null) 
			mUserData = Collections.<String,String>emptyMap();
		else 
			mUserData = data;
	}

	/** 
	 * Replaces all segments in this instance, but keeps
	 *  generation, version, counter so that future commits
	 *  remain write once.
	 */
	public void replace(ISegmentInfos other) {
		rollbackSegmentInfos(other.asList());
		mLastGeneration = ((SegmentInfos)other).getLastGeneration();
	}

	public List<ISegmentCommitInfo> createBackupSegmentInfos() {
		final List<ISegmentCommitInfo> list = new ArrayList<ISegmentCommitInfo>(size());
		for (final ISegmentCommitInfo info : this) {
			list.add(info.clone());
		}
		return list;
	}
	
	public void rollbackSegmentInfos(List<ISegmentCommitInfo> infos) {
		this.clear();
		this.addAll(infos);
	}
  
	/** applies all changes caused by committing a merge to this SegmentInfos */
	public void applyMergeChanges(ISegmentCommitInfo mergeInfo, 
			Collection<ISegmentCommitInfo> mergeSegments, boolean dropSegment) {
		final Set<ISegmentCommitInfo> mergedAway = 
				new HashSet<ISegmentCommitInfo>(mergeSegments);
		
		boolean inserted = false;
		int newSegIdx = 0;
		
		for (int segIdx = 0, cnt = mSegments.size(); segIdx < cnt; segIdx++) {
			assert segIdx >= newSegIdx;
			final ISegmentCommitInfo info = mSegments.get(segIdx);
			
			if (mergedAway.contains(info)) {
				if (!inserted && !dropSegment) {
					mSegments.set(segIdx, mergeInfo);
					inserted = true;
					newSegIdx ++;
				}
			} else {
				mSegments.set(newSegIdx, info);
				newSegIdx ++;
			}
		}

		// the rest of the segments in list are duplicates, 
		// so don't remove from map, only list!
		mSegments.subList(newSegIdx, mSegments.size()).clear();
    
		// Either we found place to insert segment, or, we did
		// not, but only because all segments we merged becamee
		// deleted while we are merging, in which case it should
		// be the case that the new segment is also all deleted,
		// we insert it at the beginning if it should not be dropped:
		if (!inserted && !dropSegment) 
			mSegments.add(0, mergeInfo);
	}
  
	/** 
	 * Returns sum of all segment's docCounts.  Note that
	 *  this does not include deletions 
	 */
	@Override
	public int getTotalDocCount() {
		int count = 0;
		for (ISegmentCommitInfo info : this) {
			count += info.getSegmentInfo().getDocCount();
		}
		return count;
	}

	/** 
	 * Call this before committing if changes have been made to the
	 *  segments. 
	 */
	public void increaseVersion() {
		mVersion ++;
		
		if (LOG.isDebugEnabled())
			LOG.debug("increaseVersion: version=" + mVersion);
	}
  
	/** Returns an <b>unmodifiable</b> {@link Iterator} of contained segments in order. */
	// @Override (comment out until Java 6)
	@Override
	public Iterator<ISegmentCommitInfo> iterator() {
		return asList().iterator();
	}
  
	/** Returns all contained segments as an <b>unmodifiable</b> {@link List} view. */
	@Override
	public List<ISegmentCommitInfo> asList() {
		return Collections.unmodifiableList(mSegments);
	}
  
	@Override
	public int size() {
		return mSegments.size();
	}

	public void add(ISegmentCommitInfo si) {
		mSegments.add(si);
	}
  
	public void addAll(Iterable<ISegmentCommitInfo> sis) {
		for (final ISegmentCommitInfo si : sis) {
			this.add(si);
		}
	}
  
	public void clear() {
		mSegments.clear();
	}

	/** WARNING: O(N) cost */
	public void remove(ISegmentCommitInfo si) {
		mSegments.remove(si);
	}
  
	/** WARNING: O(N) cost */
	public void remove(int index) {
		mSegments.remove(index);
	}

	/** WARNING: O(N) cost */
	public boolean contains(ISegmentCommitInfo si) {
		return mSegments.contains(si);
	}

	/** WARNING: O(N) cost */
	@Override
	public int indexOf(ISegmentCommitInfo si) {
		return mSegments.indexOf(si);
	}
  
	@Override
	public String toString() { 
		return toString(mDirectory);
	}
	
	public String toString(IDirectory directory) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(getClass().getSimpleName()).append("{");
		buffer.append("counter=").append(mCounter);
		buffer.append(", version=").append(mVersion);
		buffer.append(", gen=").append(mGeneration);
		buffer.append(", lastGen=").append(mLastGeneration);
		buffer.append(", ");
		
		buffer.append(getSegmentsFileName()).append("={");
		
		final int count = size();
		for (int i = 0; i < count; i++) {
			if (i > 0) buffer.append(' ');
			final ISegmentCommitInfo info = getCommitInfo(i);
			buffer.append(info.toString(directory, 0));
		}
		
		buffer.append(")}");
		return buffer.toString();
	}
	
}
