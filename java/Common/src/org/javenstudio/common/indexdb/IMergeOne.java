package org.javenstudio.common.indexdb;

public interface IMergeOne {

    /** 
     * Mark this merge as aborted.  If this is called
     *  before the merge is committed then the merge will
     *  not be committed. 
     */
    public void abort();
    
    /** Returns true if this merge was aborted. */
    public boolean isAborted();
    
	public void checkAborted(IDirectory dir) throws MergeAbortedException;
	
}
