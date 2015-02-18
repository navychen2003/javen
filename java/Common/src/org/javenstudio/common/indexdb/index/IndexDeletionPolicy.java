package org.javenstudio.common.indexdb.index;

import java.util.List;
import java.io.IOException;

/**
 * <p>Expert: policy for deletion of stale {@link IndexCommit index commits}. 
 * 
 * <p>Implement this interface, and pass it to one
 * of the {@link IndexWriter} or {@link IndexReader}
 * constructors, to customize when older
 * {@link IndexCommit point-in-time commits}
 * are deleted from the index directory.  The default deletion policy
 * is {@link KeepOnlyLastCommitDeletionPolicy}, which always
 * removes old commits as soon as a new commit is done (this
 * matches the behavior before 2.2).</p>
 *
 * <p>One expected use case for this (and the reason why it
 * was first created) is to work around problems with an
 * index directory accessed via filesystems like NFS because
 * NFS does not provide the "delete on last close" semantics
 * that Indexdb's "point in time" search normally relies on.
 * By implementing a custom deletion policy, such as "a
 * commit is only removed once it has been stale for more
 * than X minutes", you can give your readers time to
 * refresh to the new commit before {@link IndexWriter}
 * removes the old commits.  Note that doing so will
 * increase the storage requirements of the index.  See <a
 * target="top"
 * href="http://issues.apache.org/jira/browse/LUCENE-710">LUCENE-710</a>
 * for details.</p>
 */
public abstract class IndexDeletionPolicy {

	/**
	 * <p>This is called once when a writer is first
	 * instantiated to give the policy a chance to remove old
	 * commit points.</p>
	 * 
	 * <p>The writer locates all index commits present in the 
	 * index directory and calls this method.  The policy may 
	 * choose to delete some of the commit points, doing so by
	 * calling method {@link IndexCommit#delete delete()} 
	 * of {@link IndexCommit}.</p>
	 * 
	 * <p><u>Note:</u> the last CommitPoint is the most recent one,
	 * i.e. the "front index state". Be careful not to delete it,
	 * unless you know for sure what you are doing, and unless 
	 * you can afford to lose the index content while doing that. 
	 *
	 * @param commits List of current 
	 * {@link IndexCommit point-in-time commits},
	 *  sorted by age (the 0th one is the oldest commit).
	 */
	public abstract void onInit(List<? extends IndexCommit> commits) throws IOException;

	/**
	 * <p>This is called each time the writer completed a commit.
	 * This gives the policy a chance to remove old commit points
	 * with each commit.</p>
	 *
	 * <p>The policy may now choose to delete old commit points 
	 * by calling method {@link IndexCommit#delete delete()} 
	 * of {@link IndexCommit}.</p>
	 * 
	 * <p>This method is only called when {@link
	 * IndexWriter#commit} or {@link IndexWriter#close} is
	 * called, or possibly not at all if the {@link
	 * IndexWriter#rollback} is called.
	 *
	 * <p><u>Note:</u> the last CommitPoint is the most recent one,
	 * i.e. the "front index state". Be careful not to delete it,
	 * unless you know for sure what you are doing, and unless 
	 * you can afford to lose the index content while doing that.
	 *  
	 * @param commits List of {@link IndexCommit},
	 *  sorted by age (the 0th one is the oldest commit).
	 */
	public abstract void onCommit(List<? extends IndexCommit> commits) throws IOException;
	
	/**
	 * This {@link IndexDeletionPolicy} implementation that
	 * keeps only the most recent commit and immediately removes
	 * all prior commits after a new commit is done.  This is
	 * the default deletion policy.
	 */
	public static final class KeepOnlyLastCommitDeletionPolicy extends IndexDeletionPolicy {

		/**
		 * Deletes all commits except the most recent one.
		 */
		@Override
		public void onInit(List<? extends IndexCommit> commits) {
			// Note that commits.size() should normally be 1:
			onCommit(commits);
		}

		/**
		 * Deletes all commits except the most recent one.
		 */
		@Override
		public void onCommit(List<? extends IndexCommit> commits) {
			// Note that commits.size() should normally be 2 (if not
			// called by onInit above):
			int size = commits.size();
			for (int i=0; i < size-1; i++) {
				commits.get(i).delete();
			}
		}
	}
	
}
