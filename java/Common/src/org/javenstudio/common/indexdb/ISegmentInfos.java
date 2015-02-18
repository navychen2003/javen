package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ISegmentInfos extends Cloneable, Iterable<ISegmentCommitInfo> {

	public IDirectory getDirectory();
	public long getGeneration();
	
	/** 
	 * Returns sum of all segment's docCounts.  Note that
	 *  this does not include deletions 
	 */
	public int getTotalDocCount();
	
	/**
	 * version number when this SegmentInfos was generated.
	 */
	public long getVersion();
	
	/**
	 * Get the segments_N filename in use by this segment infos.
	 */
	public String getSegmentsFileName();
	
	/**
	 * Get the next segments_N filename that will be written.
	 */
	public String getNextSegmentFileName();
	
	/** 
	 * Returns all file names referenced by SegmentInfo
	 *  instances matching the provided Directory (ie files
	 *  associated with any "external" segments are skipped).
	 *  The returned collection is recomputed on each
	 *  invocation.
	 */
	public Collection<String> getFileNames(boolean includeSegmentsFile) 
			throws IOException;
	
	/** Returns all contained segments as an <b>unmodifiable</b> {@link List} view. */
	public List<ISegmentCommitInfo> asList();
	
	public Map<String,String> getUserData();
	
	public ISegmentCommitInfo getCommitInfo(int i);
	public int indexOf(ISegmentCommitInfo si);
	public int size();
	
	public ISegmentInfos clone();
	
}
