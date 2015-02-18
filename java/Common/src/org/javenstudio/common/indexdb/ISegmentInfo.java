package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface ISegmentInfo {

	public String getName();
	public IDirectory getDirectory();
	
	public Map<String, String> getDiagnostics();
	public void setDiagnostics(Map<String, String> diagnostics);
	
	/**
	 * Returns total size in bytes of all of files used by
	 * this segment.  Note that this will not include any live
	 * docs for the segment; to include that use {@link
	 * SegmentCommitInfo#sizeInBytes()} instead.
	 */
	public long getSizeInBytes() throws IOException;
	
	/**
	 * Returns true if this segment is stored as a compound
	 * file; else, false.
	 */
	public boolean getUseCompoundFile();
	
	/**
	 * Mark whether this segment is stored as a compound file.
	 *
	 * @param isCompoundFile true if this is a compound file;
	 * else, false
	 */
	public void setUseCompoundFile(boolean isCompoundFile);
	
	public int getDocCount();
	
	public void setDocCount(int docCount);
	
	/**
	 * Return all files referenced by this SegmentInfo.  The
	 * returns List is a locally cached List so you should not
	 * modify it.
	 */
	public Set<String> getFileNames() throws IOException;
	
	public void setFileNames(Set<String> files);
	public void addFileName(String file);
	
	/** Returns the version of the code which wrote the segment. */
	public String getVersion();
	
	/**
	 * Get a codec attribute value, or null if it does not exist
	 */
	public String getAttribute(String key);
	
	/**
	 * @return internal codec attributes map. May be null if no mappings exist.
	 */
	public Map<String,String> getAttributes();
	
	/** 
	 * Used for debugging.  Format may suddenly change.
	 *
	 *  <p>Current format looks like
	 *  <code>_a(3.1):c45/4</code>, which means the segment's
	 *  name is <code>_a</code>; it was created with Indexdb 3.1 (or
	 *  '?' if it's unknown); it's using compound file
	 *  format (would be <code>C</code> if not compound); it
	 *  has 45 documents; it has 4 deletions (this part is
	 *  left off when there are no deletions).</p>
	 */
	public String toString(IDirectory dir, int delCount);
	
}
