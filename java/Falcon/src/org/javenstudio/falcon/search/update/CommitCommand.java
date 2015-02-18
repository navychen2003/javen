package org.javenstudio.falcon.search.update;

import org.javenstudio.falcon.search.ISearchRequest;

/**
 *
 */
public class CommitCommand extends UpdateCommand {
	
	/**
	 * During optimize, optimize down to <= this many segments.  Must be >= 1
	 *
	 * @see IndexWriter#forceMerge(int)
	 */
	private int mMaxOptimizeSegments = 1;
	
	private boolean mOptimize;
	//open a new searcher as part of a hard commit
	private boolean mOpenSearcher = true; 
	private boolean mWaitSearcher = true;
	private boolean mExpungeDeletes = false;
	private boolean mSoftCommit = false;
	private boolean mPrepareCommit = false;

	public CommitCommand(ISearchRequest req, boolean optimize) {
		super(req);
		mOptimize = optimize;
	}

	@Override
	public String getName() {
		return "commit";
	}

	public int getMaxOptimizeSegments() { return mMaxOptimizeSegments; }
	public void setMaxOptimizeSegments(int val) { mMaxOptimizeSegments = val; }
	
	public boolean isOptimize() { return mOptimize; }
	public boolean isOpenSearcher() { return mOpenSearcher; }
	public boolean isWaitSearcher() { return mWaitSearcher; }
	public boolean isExpungeDeletes() { return mExpungeDeletes; }
	public boolean isSoftCommit() { return mSoftCommit; }
	public boolean isPrepareCommit() { return mPrepareCommit; }
	
	public void setOptimize(boolean val) { mOptimize = val; }
	public void setOpenSearcher(boolean val) { mOpenSearcher = val; }
	public void setWaitSearcher(boolean val) { mWaitSearcher = val; }
	public void setExpungeDeletes(boolean val) { mExpungeDeletes = val; }
	public void setSoftCommit(boolean val) { mSoftCommit = val; }
	public void setPrepareCommit(boolean val) { mPrepareCommit = val; }
	
	@Override
	protected void toString(StringBuilder sb) {
		sb.append(",optimize=").append(mOptimize);
		sb.append(",openSearcher=").append(mOpenSearcher);
		sb.append(",waitSearcher=").append(mWaitSearcher);
		sb.append(",expungeDeletes=").append(mExpungeDeletes);
		sb.append(",softCommit=").append(mSoftCommit);
	}
	
}
