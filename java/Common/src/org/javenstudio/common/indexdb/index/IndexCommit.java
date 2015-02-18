package org.javenstudio.common.indexdb.index;

import java.util.Collection;
import java.util.Map;
import java.io.IOException;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IIndexCommit;

/**
 * <p>Expert: represents a single commit into an index as seen by the
 * {@link IndexDeletionPolicy} or {@link IndexReader}.</p>
 *
 * <p> Changes to the content of an index are made visible
 * only after the writer who made that change commits by
 * writing a new segments file
 * (<code>segments_N</code>). This point in time, when the
 * action of writing of a new segments file to the directory
 * is completed, is an index commit.</p>
 *
 * <p>Each index commit point has a unique segments file
 * associated with it. The segments file associated with a
 * later index commit point would have a larger N.</p>
 *
 */
public abstract class IndexCommit implements IIndexCommit {

	/**
	 * Get the segments file (<code>segments_N</code>) associated 
	 * with this commit point.
	 */
	public abstract String getSegmentsFileName();

	/**
	 * Returns all index files referenced by this commit point.
	 */
	public abstract Collection<String> getFileNames() throws IOException;

	/**
	 * Returns the {@link Directory} for the index.
	 */
	public abstract IDirectory getDirectory();
  
	/**
	 * Delete this commit point.  This only applies when using
	 * the commit point in the context of IndexWriter's
	 * IndexDeletionPolicy.
	 * <p>
	 * Upon calling this, the writer is notified that this commit 
	 * point should be deleted. 
	 * <p>
	 * Decision that a commit-point should be deleted is taken by 
	 * the {@link IndexDeletionPolicy} in effect
	 * and therefore this should only be called by its 
	 * {@link IndexDeletionPolicy#onInit onInit()} or 
	 * {@link IndexDeletionPolicy#onCommit onCommit()} methods.
	 */
	public abstract void delete();

	public abstract boolean isDeleted();

	/** Returns number of segments referenced by this commit. */
	public abstract int getSegmentCount();

	/** Two IndexCommits are equal if both their Directory and versions are equal. */
	@Override
	public boolean equals(Object other) {
		if (other instanceof IndexCommit) {
			IndexCommit otherCommit = (IndexCommit) other;
			return otherCommit.getDirectory() == getDirectory() && 
					otherCommit.getGeneration() == getGeneration();
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getDirectory().hashCode() + Long.valueOf(getGeneration()).hashCode();
	}

	/** 
	 * Returns the generation (the _N in segments_N) for this
	 *  IndexCommit 
	 */
	public abstract long getGeneration();

	/** 
	 * Returns userData, previously passed to {@link
	 *  IndexWriter#commit(Map)} for this commit.  Map is
	 *  String -> String. 
	 */
	public abstract Map<String,String> getUserData() throws IOException;
  
	@Override
	public int compareTo(IIndexCommit commit) {
		if (getDirectory() != commit.getDirectory()) {
			throw new UnsupportedOperationException(
					"cannot compare IndexCommits from different Directory instances");
		}

		long gen = getGeneration();
		long comgen = commit.getGeneration();
		if (gen < comgen) 
			return -1;
		else if (gen > comgen) 
			return 1;
		
		return 0;
	}
  
}
