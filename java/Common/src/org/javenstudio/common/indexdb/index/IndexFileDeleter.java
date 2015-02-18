package org.javenstudio.common.indexdb.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javenstudio.common.indexdb.CorruptIndexException;
import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.ISegmentInfos;
import org.javenstudio.common.indexdb.NoSuchDirectoryException;
import org.javenstudio.common.indexdb.codec.IIndexFormat;
import org.javenstudio.common.indexdb.util.CollectionUtil;

/**
 * This class keeps track of each SegmentInfos instance that
 * is still "live", either because it corresponds to a
 * segments_N file in the Directory (a "commit", i.e. a
 * committed SegmentInfos) or because it's an in-memory
 * SegmentInfos that a writer is actively updating but has
 * not yet committed.  This class uses simple reference
 * counting to map the live SegmentInfos instances to
 * individual files in the Directory.
 *
 * The same directory file may be referenced by more than
 * one IndexCommit, i.e. more than one SegmentInfos.
 * Therefore we count how many commits reference each file.
 * When all the commits referencing a certain file have been
 * deleted, the refcount for that file becomes zero, and the
 * file is deleted.
 *
 * A separate deletion policy interface
 * (IndexDeletionPolicy) is consulted on creation (onInit)
 * and once per commit (onCommit), to decide when a commit
 * should be removed.
 *
 * It is the business of the IndexDeletionPolicy to choose
 * when to delete commit points.  The actual mechanics of
 * file deletion, retrying, etc, derived from the deletion
 * of commit points is the business of the IndexFileDeleter.
 *
 * The current default deletion policy is {@link
 * KeepOnlyLastCommitDeletionPolicy}, which removes all
 * prior commits when a new commit has completed.  This
 * matches the behavior before 2.2.
 *
 * Note that you must hold the write.lock before
 * instantiating this class.  It opens segments_N file(s)
 * directly with no retry logic.
 */
public final class IndexFileDeleter {

	/** 
	 * Files that we tried to delete but failed (likely
	 * because they are open and we are running on Windows),
	 * so we will retry them again later: 
	 */
	private List<String> mDeletable;

	/** 
	 * Reference count for all files in the index.
	 * Counts how many existing commits reference a file.
	 */
	private Map<String, RefCount> mRefCounts = new HashMap<String, RefCount>();

	/** 
	 * Holds all commits (segments_N) currently in the index.
	 * This will have just 1 commit if you are using the
	 * default delete policy (KeepOnlyLastCommitDeletionPolicy).
	 * Other policies may leave commit points live for longer
	 * in which case this list would be longer than 1: 
	 */
	private List<CommitPoint> mCommits = new ArrayList<CommitPoint>();

	/** 
	 * Holds files we had incref'd from the previous
	 * non-commit checkpoint: 
	 */
	private List<Collection<String>> mLastFiles = new ArrayList<Collection<String>>();

	/** Commits that the IndexDeletionPolicy have decided to delete: */
	private List<CommitPoint> mCommitsToDelete = new ArrayList<CommitPoint>();

	private final IDirectory mDirectory;
	private final IndexDeletionPolicy mPolicy;

	private final boolean mStartingCommitDeleted;
	private ISegmentInfos mLastSegmentInfos;

	/**
	 * Initialize the deleter: find all previous commits in
	 * the Directory, incref the files they reference, call
	 * the policy to let it delete commits.  This will remove
	 * any files not referenced by any of the commits.
	 * @throws CorruptIndexException if the index is corrupt
	 * @throws IOException if there is a low-level IO error
	 */
	public IndexFileDeleter(IndexDeletionPolicy policy, 
			ISegmentInfos segmentInfos, IIndexFormat format) 
			throws CorruptIndexException, IOException {
		final String currentSegmentsFile = segmentInfos.getSegmentsFileName();

		mPolicy = policy;
		mDirectory = segmentInfos.getDirectory();

		// First pass: walk the files and initialize our ref
		// counts:
		long currentGen = segmentInfos.getGeneration();

		CommitPoint currentCommitPoint = null;
		String[] files = null;
		try {
			files = mDirectory.listAll();
		} catch (NoSuchDirectoryException e) {
			// it means the directory is empty, so ignore it.
			files = new String[0];
		}

		for (String fileName : files) {
			if (!fileName.endsWith(IndexFileNames.WRITE_LOCK_NAME) && 
				!fileName.equals(IndexFileNames.SEGMENTS_GEN)) {

				// Add this file to refCounts with initial count 0:
				getRefCount(fileName);

				if (fileName.startsWith(IndexFileNames.SEGMENTS)) {
					// This is a commit (segments or segments_N), and
					// it's valid (<= the max gen).  Load it, then
					// incref all files it refers to:
					ISegmentInfos sis = null;
					try {
						sis = format.getSegmentInfosFormat().readSegmentInfos(mDirectory, fileName);
						
					} catch (FileNotFoundException e) {
						// LUCENE-948: on NFS (and maybe others), if
						// you have writers switching back and forth
						// between machines, it's very likely that the
						// dir listing will be stale and will claim a
						// file segments_X exists when in fact it
						// doesn't.  So, we catch this and handle it
						// as if the file does not exist

						sis = null;
					} catch (IOException e) {
						if (IndexFileNames.generationFromSegmentsFileName(fileName) <= currentGen && 
							mDirectory.getFileLength(fileName) > 0) {
							throw e;
						} else {
							// Most likely we are opening an index that
							// has an aborted "future" commit, so suppress
							// exc in this case
							sis = null;
						}
					}
					if (sis != null) {
						final CommitPoint commitPoint = new CommitPoint(mCommitsToDelete, sis);
						if (sis.getGeneration() == segmentInfos.getGeneration()) 
							currentCommitPoint = commitPoint;
						
						mCommits.add(commitPoint);
						increaseRef(sis, true);

						if (mLastSegmentInfos == null || sis.getGeneration() > mLastSegmentInfos.getGeneration()) 
							mLastSegmentInfos = sis;
					}
				}
			}
		}

		if (currentCommitPoint == null && currentSegmentsFile != null) {
			// We did not in fact see the segments_N file
			// corresponding to the segmentInfos that was passed
			// in.  Yet, it must exist, because our caller holds
			// the write lock.  This can happen when the directory
			// listing was stale (eg when index accessed via NFS
			// client with stale directory listing cache).  So we
			// try now to explicitly open this commit point:
			ISegmentInfos sis = null;
			try {
				sis = format.getSegmentInfosFormat().readSegmentInfos(mDirectory, currentSegmentsFile);
			} catch (IOException e) {
				throw new CorruptIndexException("failed to locate current segments_N file");
			}

			currentCommitPoint = new CommitPoint(mCommitsToDelete, sis);
			mCommits.add(currentCommitPoint);
			increaseRef(sis, true);
		}

		// We keep commits list in sorted order (oldest to newest):
		CollectionUtil.mergeSort(mCommits);

		// Now delete anything with ref count at 0.  These are
		// presumably abandoned files eg due to crash of
		// IndexWriter.
		for (Map.Entry<String, RefCount> entry : mRefCounts.entrySet() ) {
			RefCount rc = entry.getValue();
			final String fileName = entry.getKey();
			if (0 == rc.mCount) 
				deleteFile(fileName);
		}

		// Finally, give policy a chance to remove things on
		// startup:
		if (currentSegmentsFile != null) 
			policy.onInit(mCommits);

		// Always protect the incoming segmentInfos since
		// sometime it may not be the most recent commit
		checkpoint(segmentInfos, false);

		mStartingCommitDeleted = (currentCommitPoint == null) ? false : currentCommitPoint.isDeleted();

		deleteCommits();
	}

	public boolean isStartingCommitDeleted() { return mStartingCommitDeleted; }
	public ISegmentInfos getLastSegmentInfos() { return mLastSegmentInfos; }

	/**
	 * Remove the CommitPoints in the commitsToDelete List by
	 * DecRef'ing all files from each SegmentInfos.
	 */
	private void deleteCommits() throws IOException {
		int size = mCommitsToDelete.size();
		if (size > 0) {
			// First decref all files that had been referred to by
			// the now-deleted commits:
			for (int i=0; i < size; i++) {
				CommitPoint commit = mCommitsToDelete.get(i);
				for (final String file : commit.mFiles) {
					decreaseRef(file);
				}
			}
			mCommitsToDelete.clear();

			// Now compact commits to remove deleted ones (preserving the sort):
			size = mCommits.size();
			
			int readFrom = 0;
			int writeTo = 0;
			
			while (readFrom < size) {
				CommitPoint commit = mCommits.get(readFrom);
				if (!commit.mDeleted) {
					if (writeTo != readFrom) 
						mCommits.set(writeTo, mCommits.get(readFrom));
					
					writeTo ++;
				}
				readFrom ++;
			}

			while (size > writeTo) {
				mCommits.remove(size-1);
				size --;
			}
		}
	}

	/**
	 * Writer calls this when it has hit an error and had to
	 * roll back, to tell us that there may now be
	 * unreferenced files in the filesystem.  So we re-list
	 * the filesystem and delete such files.  If segmentName
	 * is non-null, we will only delete files corresponding to
	 * that segment.
	 */
	public void refresh(String segmentName) throws IOException {
		String[] files = mDirectory.listAll();
		String segmentPrefix1;
		String segmentPrefix2;
		if (segmentName != null) {
			segmentPrefix1 = segmentName + ".";
			segmentPrefix2 = segmentName + "_";
		} else {
			segmentPrefix1 = null;
			segmentPrefix2 = null;
		}

		for (int i=0; i < files.length; i++) {
			String fileName = files[i];
			if ((segmentName == null || fileName.startsWith(segmentPrefix1) || fileName.startsWith(segmentPrefix2)) &&
				!fileName.endsWith(IndexFileNames.WRITE_LOCK_NAME) && !mRefCounts.containsKey(fileName) &&
				!fileName.equals(IndexFileNames.SEGMENTS_GEN)) {
				// Unreferenced file, so remove it
				deleteFile(fileName);
			}
		}
	}

	public void refresh() throws IOException {
		// Set to null so that we regenerate the list of pending
		// files; else we can accumulate same file more than
		// once
		//assert locked();
		
		mDeletable = null;
		refresh(null);
	}

	public void close() throws IOException {
		// DecRef old files from the last checkpoint, if any:
		//assert locked();
	  
		int size = mLastFiles.size();
		if (size > 0) {
			for (int i=0; i < size; i++) {
				decreaseRef(mLastFiles.get(i));
			}
			mLastFiles.clear();
		}

		deletePendingFiles();
	}

	/**
	 * Revisits the {@link IndexDeletionPolicy} by calling its
	 * {@link IndexDeletionPolicy#onCommit(List)} again with the known commits.
	 * This is useful in cases where a deletion policy which holds onto index
	 * commits is used. The application may know that some commits are not held by
	 * the deletion policy anymore and call
	 * {@link IndexWriter#deleteUnusedFiles()}, which will attempt to delete the
	 * unused commits again.
	 */
	public void revisitPolicy() throws IOException {
		if (mCommits.size() > 0) {
			mPolicy.onCommit(mCommits);
			deleteCommits();
		}
	}

	public void deletePendingFiles() throws IOException {
		if (mDeletable != null) {
			List<String> oldDeletable = mDeletable;
			mDeletable = null;
			
			int size = oldDeletable.size();
			for (int i=0; i < size; i++) {
				deleteFile(oldDeletable.get(i));
			}
		}
	}

	/**
	 * For definition of "check point" see IndexWriter comments:
	 * "Clarification: Check Points (and commits)".
	 *
	 * Writer calls this when it has made a "consistent
	 * change" to the index, meaning new files are written to
	 * the index and the in-memory SegmentInfos have been
	 * modified to point to those files.
	 *
	 * This may or may not be a commit (segments_N may or may
	 * not have been written).
	 *
	 * We simply incref the files referenced by the new
	 * SegmentInfos and decref the files we had previously
	 * seen (if any).
	 *
	 * If this is a commit, we also call the policy to give it
	 * a chance to remove other commits.  If any commits are
	 * removed, we decref their files as well.
   	 */
	public void checkpoint(ISegmentInfos segmentInfos, boolean isCommit) throws IOException {
		// Try again now to delete any previously un-deletable
		// files (because they were in use, on Windows):
		deletePendingFiles();

		// Incref the files:
		increaseRef(segmentInfos, isCommit);

		if (isCommit) {
			// Append to our commits list:
			mCommits.add(new CommitPoint(mCommitsToDelete, segmentInfos));

			// Tell policy so it can remove commits:
			mPolicy.onCommit(mCommits);

			// Decref files for commits that were deleted by the policy:
			deleteCommits();
		} else {
			// DecRef old files from the last checkpoint, if any:
			for (Collection<String> lastFile : mLastFiles) {
				decreaseRef(lastFile);
			}
			mLastFiles.clear();

			// Save files so we can decr on next checkpoint/commit:
			mLastFiles.add(segmentInfos.getFileNames(false));
		}
	}

	public void increaseRef(ISegmentInfos segmentInfos, boolean isCommit) throws IOException {
		// If this is a commit point, also incRef the
		// segments_N file:
		for (final String fileName : segmentInfos.getFileNames(isCommit)) {
			increaseRef(fileName);
		}
	}

	public void increaseRef(Collection<String> files) throws IOException {
		for (final String file : files) {
			increaseRef(file);
		}
	}

	public void increaseRef(String fileName) throws IOException {
		RefCount rc = getRefCount(fileName);
		rc.increaseRef();
	}

	public void decreaseRef(Collection<String> files) throws IOException {
		for (final String file : files) {
			decreaseRef(file);
		}
	}

	public void decreaseRef(String fileName) throws IOException {
		RefCount rc = getRefCount(fileName);
		if (0 == rc.decreaseRef()) {
			// This file is no longer referenced by any past
			// commit points nor by the in-memory SegmentInfos:
			deleteFile(fileName);
			mRefCounts.remove(fileName);
		}
	}

	public void decreaseRef(ISegmentInfos segmentInfos) throws IOException {
		for (final String file : segmentInfos.getFileNames(false)) {
			decreaseRef(file);
		}
	}

	public boolean exists(String fileName) {
		if (!mRefCounts.containsKey(fileName)) 
			return false;
		else 
			return getRefCount(fileName).mCount > 0;
	}

	private RefCount getRefCount(String fileName) {
		final RefCount rc;
		if (!mRefCounts.containsKey(fileName)) {
			rc = new RefCount(fileName);
			mRefCounts.put(fileName, rc);
		} else {
			rc = mRefCounts.get(fileName);
		}
		
		return rc;
	}

	public void deleteFiles(List<String> files) throws IOException {
		for (final String file: files) {
			deleteFile(file);
		}
	}

	/** 
	 * Deletes the specified files, but only if they are new
	 *  (have not yet been incref'd). 
	 */
	public void deleteNewFiles(Collection<String> files) throws IOException {
		for (final String fileName: files) {
			if (!mRefCounts.containsKey(fileName)) 
				deleteFile(fileName);
		}
	}

	public void deleteFile(String fileName) throws IOException {
		try {
			mDirectory.deleteFile(fileName);
		} catch (IOException e) {		// if delete fails
			if (mDirectory.fileExists(fileName)) {
				// Some operating systems (e.g. Windows) don't
				// permit a file to be deleted while it is opened
				// for read (e.g. by another process or thread). So
				// we assume that when a delete fails it is because
				// the file is open in another process, and queue
				// the file for subsequent deletion.
				if (mDeletable == null) 
					mDeletable = new ArrayList<String>();
				
				mDeletable.add(fileName);	// add to deletable
			}
		}
	}

	/**
	 * Tracks the reference count for a single index file:
	 */
	private static class RefCount {
		// fileName used only for better assert error messages
		private final String mFileName;
		private int mCount = 0;
		private boolean mInitDone = false;
		
		public RefCount(String fileName) {
			mFileName = fileName;
		}

		public int increaseRef() {
			if (!mInitDone) {
				mInitDone = true;
			} else {
				assert mCount > 0: Thread.currentThread().getName() + 
					": RefCount is 0 pre-increment for file \"" + mFileName + "\"";
			}
			return ++mCount;
		}

		public int decreaseRef() {
			assert mCount > 0: Thread.currentThread().getName() + 
				": RefCount is 0 pre-decrement for file \"" + mFileName + "\"";
			return --mCount;
		}
	}

	/**
	 * Holds details for each commit point.  This class is
	 * also passed to the deletion policy.  Note: this class
	 * has a natural ordering that is inconsistent with
	 * equals.
	 */
	private static class CommitPoint extends IndexCommit {
		private Collection<String> mFiles;
		private String mSegmentsFileName;
		private boolean mDeleted;
		private IDirectory mDirectory;
		private Collection<CommitPoint> mCommitsToDelete;
		private long mGeneration;
		private final Map<String,String> mUserData;
		private final int mSegmentCount;

		public CommitPoint(Collection<CommitPoint> commitsToDelete, 
				ISegmentInfos segmentInfos) throws IOException {
			mDirectory = segmentInfos.getDirectory();
			mCommitsToDelete = commitsToDelete;
			mUserData = segmentInfos.getUserData();
			mSegmentsFileName = segmentInfos.getSegmentsFileName();
			mGeneration = segmentInfos.getGeneration();
			mFiles = Collections.unmodifiableCollection(segmentInfos.getFileNames(true));
			mSegmentCount = segmentInfos.size();
		}

		@Override
		public int getSegmentCount() {
			return mSegmentCount;
		}

		@Override
		public String getSegmentsFileName() {
			return mSegmentsFileName;
		}

		@Override
		public Collection<String> getFileNames() throws IOException {
			return mFiles;
		}

		@Override
		public IDirectory getDirectory() {
			return mDirectory;
		}

		@Override
		public long getGeneration() {
			return mGeneration;
		}

		@Override
		public Map<String,String> getUserData() {
			return mUserData;
		}

		/**
		 * Called only be the deletion policy, to remove this
		 * commit point from the index.
		 */
		@Override
		public void delete() {
			if (!mDeleted) {
				mDeleted = true;
				mCommitsToDelete.add(this);
			}
		}

		@Override
		public boolean isDeleted() {
			return mDeleted;
		}
		
		@Override
		public String toString() {
			return "IndexFileDeleter.CommitPoint(" + mSegmentsFileName + ")";
		}
	}
	
}
