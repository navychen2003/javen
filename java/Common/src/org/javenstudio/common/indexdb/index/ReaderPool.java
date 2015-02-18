package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentInfos;

/** 
 * Holds shared SegmentReader instances. IndexWriter uses
 *  SegmentReaders for 1) applying deletes, 2) doing
 *  merges, 3) handing out a real-time reader.  This pool
 *  reuses instances of the SegmentReaders in all these
 *  places if it is in "near real-time mode" (getReader()
 *  has been called on this instance). 
 */
public class ReaderPool {

	private final IndexWriter mWriter;
    private final Map<ISegmentCommitInfo, ReadersAndLiveDocs> mReaderMap = 
    		new HashMap<ISegmentCommitInfo, ReadersAndLiveDocs>();

    public ReaderPool(IndexWriter writer) { 
    	mWriter = writer;
    }
    
    // used only by asserts
    public synchronized boolean infoIsLive(ISegmentCommitInfo info) {
    	int idx = mWriter.getSegmentInfos().indexOf(info);
    	assert idx != -1: "info=" + info + " isn't live";
    	assert mWriter.getSegmentInfos().getCommitInfo(idx) == info: "info=" + info + 
    			" doesn't match live info in segmentInfos";
    	return true;
    }

    public synchronized void drop(ISegmentCommitInfo info) throws IOException {
    	final ReadersAndLiveDocs rld = mReaderMap.get(info);
    	if (rld != null) {
    		assert info == rld.getCommitInfo();
    		mReaderMap.remove(info);
    		rld.dropReaders();
    	}
    }

    public synchronized void release(ReadersAndLiveDocs rld) throws IOException {
    	// Matches incRef in get:
    	rld.decreaseRef();

    	// Pool still holds a ref:
    	assert rld.refCount() >= 1;

    	if (!mWriter.isPoolReaders() && rld.refCount() == 1) {
    		// This is the last ref to this RLD, and we're not
    		// pooling, so remove it:
    		if (rld.writeLiveDocs()) {
    			// Make sure we only write del docs for a live segment:
    			assert infoIsLive(rld.getCommitInfo());
    			// Must checkpoint w/ deleter, because we just
    			// created created new _X_N.del file.
    			mWriter.getDeleter().checkpoint(mWriter.getSegmentInfos(), false);
    		}

    		rld.dropReaders();
    		mReaderMap.remove(rld.getCommitInfo());
    	}
    }

    /** 
     * Remove all our references to readers, and commits
     *  any pending changes. 
     */
    public synchronized void dropAll(boolean doSave) throws IOException {
    	final Iterator<Map.Entry<ISegmentCommitInfo, ReadersAndLiveDocs>> it = 
    			mReaderMap.entrySet().iterator();
    	
    	while (it.hasNext()) {
    		final ReadersAndLiveDocs rld = it.next().getValue();
    		if (doSave && rld.writeLiveDocs()) {
    			// Make sure we only write del docs for a live segment:
    			assert infoIsLive(rld.getCommitInfo());
    			// Must checkpoint w/ deleter, because we just
    			// created created new _X_N.del file.
    			mWriter.getDeleter().checkpoint(mWriter.getSegmentInfos(), false);
    		}

    		// Important to remove as-we-go, not with .clear()
    		// in the end, in case we hit an exception;
    		// otherwise we could over-decref if close() is
    		// called again:
    		it.remove();

    		// NOTE: it is allowed that these decRefs do not
    		// actually close the SRs; this happens when a
    		// near real-time reader is kept open after the
    		// IndexWriter instance is closed:
    		rld.dropReaders();
    	}
    	
    	assert mReaderMap.size() == 0;
    }

    /**
     * Commit live docs changes for the segment readers for
     * the provided infos.
     *
     * @throws IOException
     */
    public synchronized void commit(ISegmentInfos infos) throws IOException {
    	for (ISegmentCommitInfo info : infos) {
    		final ReadersAndLiveDocs rld = mReaderMap.get(info);
    		if (rld != null) {
    			assert rld.getCommitInfo() == info;
    			
    			if (rld.writeLiveDocs()) {
    				// Make sure we only write del docs for a live segment:
    				assert infoIsLive(info);
    				// Must checkpoint w/ deleter, because we just
    				// created created new _X_N.del file.
    				mWriter.getDeleter().checkpoint(mWriter.getSegmentInfos(), false);
    			}
    		}
    	}
    }

    /**
     * Obtain a ReadersAndLiveDocs instance from the
     * readerPool.  If create is true, you must later call
     * {@link #release(ReadersAndLiveDocs)}.
     */
    public synchronized ReadersAndLiveDocs get(ISegmentCommitInfo info, boolean create) {
    	assert info.getSegmentInfo().getDirectory() == mWriter.getDirectory(): 
    		"info.dir=" + info.getSegmentInfo().getDirectory() + " vs " + mWriter.getDirectory();

    	ReadersAndLiveDocs rld = mReaderMap.get(info);
    	if (rld == null) {
    		if (!create) 
    			return null;
    		
    		rld = new ReadersAndLiveDocs(mWriter, info);
    		// Steal initial reference:
    		mReaderMap.put(info, rld);
    		
    	} else {
    		assert rld.getCommitInfo() == info: "rld.info=" + rld.getCommitInfo() + 
    				" info=" + info + " isLive?=" + infoIsLive(rld.getCommitInfo()) + 
    				" vs " + infoIsLive(info);
    	}

    	if (create) {
    		// Return ref to caller:
    		rld.increaseRef();
    	}

    	return rld;
    }
    
}
