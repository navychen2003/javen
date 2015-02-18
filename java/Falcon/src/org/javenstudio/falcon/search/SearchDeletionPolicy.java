package org.javenstudio.falcon.search;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.index.IndexCommit;
import org.javenstudio.common.indexdb.index.IndexDeletionPolicy;
import org.javenstudio.common.indexdb.store.local.FSDirectory;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.util.DateParser;
import org.javenstudio.falcon.util.NamedList;
import org.javenstudio.falcon.util.NamedListPlugin;

/**
 * Standard deletion policy that allows reserving index commit points
 * for certain amounts of time to support features such as index replication
 * or snapshooting directly out of a live index directory.
 *
 * @see IndexDeletionPolicy
 */
public class SearchDeletionPolicy extends IndexDeletionPolicy implements NamedListPlugin {
	public static Logger LOG = Logger.getLogger(SearchDeletionPolicy.class);

	private String mMaxCommitAge = null;
	private int mMaxCommitsToKeep = 1;
	private int mMaxOptimizedCommitsToKeep = 0;

	@Override
	public void init(NamedList<?> args) {
		String keepOptimizedOnlyString = (String) args.get("keepOptimizedOnly");
		String maxCommitsToKeepString = (String) args.get("maxCommitsToKeep");
		String maxOptimizedCommitsToKeepString = (String) args.get("maxOptimizedCommitsToKeep");
		String maxCommitAgeString = (String) args.get("maxCommitAge");

		if (maxCommitsToKeepString != null && maxCommitsToKeepString.trim().length() > 0)
			mMaxCommitsToKeep = Integer.parseInt(maxCommitsToKeepString);
		if (maxCommitAgeString != null && maxCommitAgeString.trim().length() > 0)
			mMaxCommitAge = "-" + maxCommitAgeString;
		if (maxOptimizedCommitsToKeepString != null && maxOptimizedCommitsToKeepString.trim().length() > 0) 
			mMaxOptimizedCommitsToKeep = Integer.parseInt(maxOptimizedCommitsToKeepString);
    
		// legacy support
		if (keepOptimizedOnlyString != null && keepOptimizedOnlyString.trim().length() > 0) {
			boolean keepOptimizedOnly = Boolean.parseBoolean(keepOptimizedOnlyString);
			if (keepOptimizedOnly) {
				mMaxOptimizedCommitsToKeep = Math.max(mMaxOptimizedCommitsToKeep, mMaxCommitsToKeep);
				mMaxCommitsToKeep = 0;
			}
		}
	}

	static String toString(IndexCommit commit) {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("commit{");

			IDirectory dir = commit.getDirectory();
			if (dir instanceof FSDirectory) {
				FSDirectory fsd = (FSDirectory) dir;
				sb.append("dir=").append(fsd.getDirectory());
			} else {
				sb.append("dir=").append(dir);
			}

			sb.append(",segFN=").append(commit.getSegmentsFileName());
			sb.append(",generation=").append(commit.getGeneration());
			sb.append(",filenames=").append(commit.getFileNames());
			
		} catch (Exception e) {
			sb.append(e);
		}
		
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	static String toString(List<? extends IndexCommit> commits) {
		StringBuilder sb = new StringBuilder();
		sb.append("num=").append(commits.size());

		for (IndexCommit commit : (List<IndexCommit>) commits) {
			sb.append("\n\t");
			sb.append(toString(commit));
		}
		
		return sb.toString();
	}

	/**
	 * Internal use for indexdb... do not explicitly call.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onInit(List<? extends IndexCommit> commits) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("onInit: commits:" + toString(commits));
		
		updateCommits((List<IndexCommit>) commits);
	}

	/**
	 * Internal use for indexdb... do not explicitly call.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void onCommit(List<? extends IndexCommit> commits) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("onCommit: commits:" + toString(commits));
		
		updateCommits((List<IndexCommit>) commits);
	}

	private void updateCommits(List<IndexCommit> commits) {
		// to be safe, we should only call delete on a commit point passed to us
		// in this specific call (may be across diff IndexWriter instances).
		// this will happen rarely, so just synchronize everything
		// for safety and to avoid race conditions
		synchronized (this) {
			long maxCommitAgeTimeStamp = -1L;
			IndexCommit newest = commits.get(commits.size() - 1);
			
			if (LOG.isDebugEnabled())
				LOG.debug("newest commit = " + newest.getGeneration());

			int singleSegKept = (newest.getSegmentCount() == 1) ? 1 : 0;
			int totalKept = 1;

			// work our way from newest to oldest, skipping the first since we always want to keep it.
			for (int i=commits.size()-2; i >= 0; i--) {
				IndexCommit commit = commits.get(i);

				// delete anything too old, regardless of other policies
				try {
					if (mMaxCommitAge != null) {
						if (maxCommitAgeTimeStamp == -1) {
							DateParser dmp = new DateParser(DateParser.UTC, Locale.ROOT);
							maxCommitAgeTimeStamp = dmp.parseMath(mMaxCommitAge).getTime();
						}
						
						if (SearchWriter.getCommitTimestamp(commit) < maxCommitAgeTimeStamp) {
							commit.delete();
							continue;
						}
					}
				} catch (Exception e) {
					if (LOG.isWarnEnabled())
						LOG.warn("Exception while checking commit point's age for deletion", e);
				}

				if (singleSegKept < mMaxOptimizedCommitsToKeep && commit.getSegmentCount() == 1) {
					totalKept ++;
					singleSegKept ++;
					continue;
				}

				if (totalKept < mMaxCommitsToKeep) {
					totalKept ++;
					continue;
				}
                                                  
				commit.delete();
			}

		} // end synchronized
	}

	@SuppressWarnings("unused")
	private String getId(IndexCommit commit) {
		StringBuilder sb = new StringBuilder();
		
		// For anything persistent, make something that will
		// be the same, regardless of the Directory instance.
		IDirectory dir = commit.getDirectory();
		if (dir instanceof FSDirectory) {
			FSDirectory fsd = (FSDirectory) dir;
			File fdir = fsd.getDirectory();
			sb.append(fdir.getPath());
		} else {
			sb.append(dir);
		}

		sb.append('/');
		sb.append(commit.getGeneration());
		
		return sb.toString();
	}

	public String getMaxCommitAge() { return mMaxCommitAge; }

	public int getMaxCommitsToKeep() { return mMaxCommitsToKeep; }
	public int getMaxOptimizedCommitsToKeep() { return mMaxOptimizedCommitsToKeep; }

	public void setMaxCommitsToKeep(int maxCommitsToKeep) {
		synchronized (this) {
			mMaxCommitsToKeep = maxCommitsToKeep;
		}
	}

	public void setMaxOptimizedCommitsToKeep(int maxOptimizedCommitsToKeep) {
		synchronized (this) {
			mMaxOptimizedCommitsToKeep = maxOptimizedCommitsToKeep;
		}    
	}

}
