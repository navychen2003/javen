package org.javenstudio.common.indexdb.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IDirectory;
import org.javenstudio.common.indexdb.IMergeOne;
import org.javenstudio.common.indexdb.ISegmentCommitInfo;
import org.javenstudio.common.indexdb.ISegmentReader;
import org.javenstudio.common.indexdb.MergeAbortedException;

/** 
 * MergeOne provides the information necessary to perform
 *  an individual primitive merge operation, resulting in
 *  a single new segment.  The merge spec includes the
 *  subset of segments to be merged as well as whether the
 *  new segment should use the compound file format. 
 */
public class MergeOne implements IMergeOne {

	private final List<ISegmentCommitInfo> mSegments;
	private final List<ISegmentReader> mReaders;
	private final int mTotalDocCount;
    
    private ISegmentCommitInfo mInfo;  		// used by MergeControl
    private boolean mRegisterDone;    		// used by MergeControl
    private long mMergeGen;           		// used by MergeControl
    private boolean mIsExternal;        	// used by MergeControl
    private int mMaxNumSegments = -1;    	// used by MergeControl
    private long mEstimatedMergeBytes;    	// used by MergeControl

    private boolean mAborted;
    private Throwable mError;
    private boolean mPaused;
	
    public MergeOne(List<ISegmentCommitInfo> segments) {
    	if (0 == segments.size())
    		throw new RuntimeException("segments must include at least one segment");
    	
    	// clone the list, as the in list may be based off original SegmentInfos and may be modified
    	mSegments = new ArrayList<ISegmentCommitInfo>(segments);
    	mReaders = new ArrayList<ISegmentReader>();
    	
    	int count = 0;
    	for (ISegmentCommitInfo info : segments) {
    		count += info.getSegmentInfo().getDocCount();
    	}
    	
    	mTotalDocCount = count;
    }

    public final ISegmentCommitInfo getCommitInfo() { return mInfo; }
    public final void setCommitInfo(ISegmentCommitInfo info) { mInfo = info; }
    
    public final boolean isRegisterDone() { return mRegisterDone; }
    public final void setRegisterDone(boolean done) { mRegisterDone = done; }
    
    public final boolean isExternal() { return mIsExternal; }
    public final void setExternal(boolean val) { mIsExternal = val; }
    
    public final long getMergeGen() { return mMergeGen; }
    public final void setMergeGen(long gen) { mMergeGen = gen; }
    
    public final int getMaxNumSegments() { return mMaxNumSegments; }
    public final void setMaxNumSegments(int num) { mMaxNumSegments = num; }
    
    public final int getSegmentSize() { return mSegments.size(); }
    public final ISegmentCommitInfo getSegmentAt(int idx) { return mSegments.get(idx); }
    
    public final int getReaderSize() { return mReaders.size(); }
    public final ISegmentReader getReaderAt(int idx) { return mReaders.get(idx); }
    
    public final long getEstimatedMergeBytes() { return mEstimatedMergeBytes; }
    public final void setEstimatedMergeBytes(long bytes) { mEstimatedMergeBytes = bytes; }
    public final void increaseEstimatedMergeBytes(long bytes) { mEstimatedMergeBytes += bytes; }
    
    final List<ISegmentCommitInfo> getSegments() { return mSegments; }
    
    public final boolean containsSegment(ISegmentCommitInfo info) { 
    	return mSegments.contains(info); 
    }
    
    final boolean removeSegment(ISegmentCommitInfo info) { 
    	return mSegments.remove(info);
    }
    
    final void addReader(ISegmentReader reader) { 
    	mReaders.add(reader);
    }
    
    final void setReaderAt(int idx, ISegmentReader reader) { 
    	mReaders.set(idx, reader);
    }
    
    /** 
     * Record that an exception occurred while executing
     *  this merge 
     */
    public synchronized void setException(Throwable error) {
    	mError = error;
    }

    /** 
     * Retrieve previous exception set by {@link
     *  #setException}. 
     */
    public synchronized Throwable getException() {
    	return mError;
    }

    /** 
     * Mark this merge as aborted.  If this is called
     *  before the merge is committed then the merge will
     *  not be committed. 
     */
    @Override
    public synchronized void abort() {
    	mAborted = true;
    	notifyAll();
    }

    /** Returns true if this merge was aborted. */
    @Override
    public synchronized boolean isAborted() {
    	return mAborted;
    }

    @Override
    public synchronized void checkAborted(IDirectory dir) 
    		throws MergeAbortedException {
    	if (mAborted) 
    		throw new MergeAbortedException("merge is aborted: " + toSegmentString(dir));

    	while (mPaused) {
    		try {
    			// In theory we could wait() indefinitely, but we
    			// do 1000 msec, defensively
    			wait(1000);
    		} catch (InterruptedException ie) {
    			throw new RuntimeException(ie);
    		}
    		if (mAborted) 
    			throw new MergeAbortedException("merge is aborted: " + toSegmentString(dir));
    	}
    }

    public synchronized void setPause(boolean paused) {
    	mPaused = paused;
    	if (!paused) {
    		// Wakeup merge thread, if it's waiting
    		notifyAll();
    	}
    }

    public synchronized boolean getPause() {
    	return mPaused;
    }
  
    /**
     * Returns the total size in bytes of this merge. Note that this does not
     * indicate the size of the merged segment, but the input total size.
     */
    public long getTotalBytesSize() throws IOException {
    	long total = 0;
    	for (ISegmentCommitInfo info : mSegments) {
    		total += info.getSegmentInfo().getSizeInBytes();
    	}
    	return total;
    }

    /**
     * Returns the total number of documents that are included with this merge.
     * Note that this does not indicate the number of documents after the merge.
     */
    public int getTotalNumDocs() throws IOException {
    	int total = 0;
    	for (ISegmentCommitInfo info : mSegments) {
    		total += info.getSegmentInfo().getDocCount();
    	}
    	return total;
    }
  
    public MergeInfo getMergeInfo() {
    	return new MergeInfo(mTotalDocCount, mEstimatedMergeBytes, mIsExternal, mMaxNumSegments);
    }    
    
    public String toSegmentString(IDirectory dir) {
    	StringBuilder b = new StringBuilder();
    	
    	final int numSegments = mSegments.size();
    	for (int i=0; i < numSegments; i++) {
    		if (i > 0) b.append(' ');
    		b.append(mSegments.get(i).toString(dir, 0));
    	}
    	
    	if (mInfo != null) 
    		b.append(" into ").append(mInfo.getSegmentInfo().getName());
    	if (mMaxNumSegments != -1)
    		b.append(" [maxNumSegments=" + mMaxNumSegments + "]");
    	if (mAborted) 
    		b.append(" [ABORTED]");
    	
    	return b.toString();
    }
    
}
