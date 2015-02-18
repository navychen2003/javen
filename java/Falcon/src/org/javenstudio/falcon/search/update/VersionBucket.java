package org.javenstudio.falcon.search.update;

// TODO: make inner?
// TODO: store the highest possible in the index on a commit (but how to not block adds?)
// TODO: could also store highest possible in the transaction log after a commit.
// Or on a new index, just scan "version" for the max?
public class VersionBucket {
	
  	private long mHighest = 0;

  	public long getHighest() { return mHighest; }
  	public void setHighest(long val) { mHighest = val; }
  	
  	public void updateHighest(long val) {
  		if (mHighest != 0) 
  			mHighest = Math.max(mHighest, Math.abs(val));
  	}
  
}
