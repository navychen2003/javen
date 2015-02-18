package org.javenstudio.common.indexdb;

import java.io.IOException;
import java.util.Map;

public interface IIndexCommit extends Comparable<IIndexCommit> {

	/**
	 * Returns the {@link Directory} for the index.
	 */
	public IDirectory getDirectory();
	
	/** 
	 * Returns the generation (the _N in segments_N) for this
	 *  IndexCommit 
	 */
	public long getGeneration();
	
	/**
	 * Get the segments file (<code>segments_N</code>) associated 
	 * with this commit point.
	 */
	public String getSegmentsFileName();
	
	/** 
	 * Returns userData, previously passed to {@link
	 *  IndexWriter#commit(Map)} for this commit.  Map is
	 *  String -> String. 
	 */
	public Map<String,String> getUserData() throws IOException;
	
}
