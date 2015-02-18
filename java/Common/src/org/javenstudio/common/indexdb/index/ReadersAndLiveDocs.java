package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.codec.ILiveDocsFormat;
import org.javenstudio.common.indexdb.util.Bits;
import org.javenstudio.common.indexdb.util.MutableBits;

// Used by IndexWriter to hold open SegmentReaders (for
// searching or merging), plus pending deletes,
// for a given segment
public class ReadersAndLiveDocs {
	
	// Not final because we replace (clone) when we need to
	// change it and it's been shared:
	private final ISegmentCommitInfo mInfo;

	// Tracks how many consumers are using this instance:
	private final AtomicInteger mRefCount = new AtomicInteger(1);

	private final IndexWriter mWriter;

	// Set once (null, and then maybe set, and never set again):
	private ISegmentReader mReader;

	// TODO: it's sometimes wasteful that we hold open two
	// separate SRs (one for merging one for
	// reading)... maybe just use a single SR?  The gains of
	// not loading the terms index (for merging in the
	// non-NRT case) are far less now... and if the app has
	// any deletes it'll open real readers anyway.

	// Set once (null, and then maybe set, and never set again):
	private ISegmentReader mMergeReader;

	// Holds the current shared (readable and writable
	// liveDocs).  This is null when there are no deleted
	// docs, and it's copy-on-write (cloned whenever we need
	// to change it but it's been shared to an external NRT
	// reader).
	private Bits mLiveDocs;

	// How many further deletions we've done against
	// liveDocs vs when we loaded it or last wrote it:
	private int mPendingDeleteCount;

	// True if the current liveDocs is referenced by an
	// external NRT reader:
	private boolean mShared;

	public ReadersAndLiveDocs(IndexWriter writer, ISegmentCommitInfo info) {
		mInfo = info;
		mWriter = writer;
		mShared = true;
	}

	public final ISegmentCommitInfo getCommitInfo() { 
		return mInfo;
	}
	
	public void increaseRef() {
		final int rc = mRefCount.incrementAndGet();
		assert rc > 1;
	}

	public void decreaseRef() {
		final int rc = mRefCount.decrementAndGet();
		assert rc >= 0;
	}

	public int refCount() {
		final int rc = mRefCount.get();
		assert rc >= 0;
		return rc;
	}

	public synchronized int getPendingDeleteCount() {
		return mPendingDeleteCount;
	}

	// Call only from assert!
	public synchronized boolean verifyDocCounts() {
		final int docCount = mInfo.getSegmentInfo().getDocCount();
		
		int count;
		if (mLiveDocs != null) {
			count = 0;
			for (int docID=0; docID < docCount; docID++) {
				if (mLiveDocs.get(docID)) 
					count ++;
			}
		} else {
			count = docCount;
		}

		assert docCount - mInfo.getDelCount() - mPendingDeleteCount == count: 
			"info.docCount=" + docCount + " info.getDelCount()=" + mInfo.getDelCount() + 
			" pendingDeleteCount=" + mPendingDeleteCount + " count=" + count;
		
		return true;
	}

	// Get reader for searching/deleting
	public synchronized ISegmentReader getReader() throws IOException {
		if (mReader == null) {
			// We steal returned ref:
			mReader = mWriter.getContext().newSegmentReader(mInfo);
			if (mLiveDocs == null) 
				mLiveDocs = mReader.getLiveDocs();
		}

		// Ref for caller
		mReader.increaseRef();
		return mReader;
	}

	// Get reader for merging (does not load the terms
	// index):
	public synchronized ISegmentReader getMergeReader() throws IOException {
		if (mMergeReader == null) {
			if (mReader != null) {
				// Just use the already opened non-merge reader
				// for merging.  In the NRT case this saves us
				// pointless double-open:
				// Ref for us:
				mReader.increaseRef();
				mMergeReader = mReader;
				
			} else {
				// We steal returned ref:
				mMergeReader = mWriter.getContext().newSegmentReader(mInfo);
				if (mLiveDocs == null) 
					mLiveDocs = mMergeReader.getLiveDocs();
			}
		}

		// Ref for caller
		mMergeReader.increaseRef();
		return mMergeReader;
	}

	public synchronized void release(ISegmentReader sr) throws IOException {
		assert mInfo == sr.getCommitInfo();
		sr.decreaseRef();
	}

	public synchronized boolean delete(int docID) {
		assert mLiveDocs != null;
		assert Thread.holdsLock(mWriter);
		assert docID >= 0 && docID < mLiveDocs.length() : "out of bounds: docid=" + docID + 
				" liveDocsLength=" + mLiveDocs.length() + " seg=" + mInfo.getSegmentInfo().getName() + 
				" docCount=" + mInfo.getSegmentInfo().getDocCount();
		assert !mShared;
		
		final boolean didDelete = mLiveDocs.get(docID);
		if (didDelete) {
			((MutableBits) mLiveDocs).clear(docID);
			mPendingDeleteCount ++;
		}
		
		return didDelete;
	}

	// NOTE: removes callers ref
	public synchronized void dropReaders() throws IOException {
		if (mReader != null) {
			mReader.decreaseRef();
			mReader = null;
		}
		if (mMergeReader != null) {
			mMergeReader.decreaseRef();
			mMergeReader = null;
		}
		decreaseRef();
	}

	/**
	 * Returns a ref to a clone.  NOTE: this clone is not
	 * enrolled in the pool, so you should simply close()
	 * it when you're done (ie, do not call release()).
	 */
	public synchronized ISegmentReader getReadOnlyClone() throws IOException {
		if (mReader == null) {
			getReader().decreaseRef();
			assert mReader != null;
		}
		mShared = true;
		if (mLiveDocs != null) {
			return mWriter.getContext().newSegmentReader(mReader, mLiveDocs, 
					mInfo.getSegmentInfo().getDocCount() - mInfo.getDelCount() - mPendingDeleteCount);
		} else {
			assert mReader.getLiveDocs() == mLiveDocs;
			mReader.increaseRef();
			return mReader;
		}
	}

	public synchronized void initWritableLiveDocs() throws IOException {
		assert Thread.holdsLock(mWriter);
		assert mInfo.getSegmentInfo().getDocCount() > 0;
		
		if (mShared) {
			// Copy on write: this means we've cloned a
			// SegmentReader sharing the current liveDocs
			// instance; must now make a private clone so we can
			// change it:
			ILiveDocsFormat liveDocsFormat = mWriter.getIndexFormat().getLiveDocsFormat();
			if (mLiveDocs == null) 
				mLiveDocs = liveDocsFormat.newLiveDocs(mInfo.getSegmentInfo().getDocCount());
			else 
				mLiveDocs = liveDocsFormat.newLiveDocs(mLiveDocs);
			
			mShared = false;
		} else {
			assert mLiveDocs != null;
		}
	}

	public synchronized Bits getLiveDocs() {
		assert Thread.holdsLock(mWriter);
		return mLiveDocs;
	}

	public synchronized Bits getReadOnlyLiveDocs() {
		assert Thread.holdsLock(mWriter);
		mShared = true;
		return mLiveDocs;
	}

	public synchronized void dropChanges() {
		// Discard (don't save) changes when we are dropping
		// the reader; this is used only on the sub-readers
		// after a successful merge.  If deletes had
		// accumulated on those sub-readers while the merge
		// is running, by now we have carried forward those
		// deletes onto the newly merged segment, so we can
		// discard them on the sub-readers:
		mPendingDeleteCount = 0;
	}

	// Commit live docs to the directory (writes new
	// _X_N.del files); returns true if it wrote the file
	// and false if there were no new deletes to write:
	public synchronized boolean writeLiveDocs() throws IOException {
		if (mPendingDeleteCount != 0) {
			// We have new deletes
			assert mLiveDocs.length() == mInfo.getSegmentInfo().getDocCount();

			// We can write directly to the actual name (vs to a
			// .tmp & renaming it) because the file is not live
			// until segments file is written:
			ILiveDocsFormat liveDocsFormat = mWriter.getIndexFormat().getLiveDocsFormat();
			liveDocsFormat.writeLiveDocs(mWriter.getDirectory(), 
					(MutableBits)mLiveDocs, mInfo, mPendingDeleteCount);

			// If we hit an exc in the line above (eg disk full)
			// then info remains pointing to the previous
			// (successfully written) del docs:
			mInfo.advanceDelGen();
			mInfo.setDelCount(mInfo.getDelCount() + mPendingDeleteCount);

			mPendingDeleteCount = 0;
			return true;
		}
		
		return false;
	}

	@Override
	public String toString() {
		return "ReadersAndLiveDocs{seg=" + mInfo + " pendingDeleteCount=" 
				+ mPendingDeleteCount + " shared=" + mShared + "}";
	}
	
}
