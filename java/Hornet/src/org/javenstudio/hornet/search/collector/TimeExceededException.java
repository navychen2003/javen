package org.javenstudio.hornet.search.collector;

/** Thrown when elapsed search time exceeds allowed search time. */
public class TimeExceededException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private long mTimeAllowed;
    private long mTimeElapsed;
    private int mLastDocCollected;
    
    public TimeExceededException(long timeAllowed, long timeElapsed, int lastDocCollected) {
    	super("Elapsed time: " + timeElapsed + "Exceeded allowed search time: " + timeAllowed + " ms.");
    	
    	mTimeAllowed = timeAllowed;
    	mTimeElapsed = timeElapsed;
    	mLastDocCollected = lastDocCollected;
    }
    
    /** Returns allowed time (milliseconds). */
    public long getTimeAllowed() {
    	return mTimeAllowed;
    }
    
    /** Returns elapsed time (milliseconds). */
    public long getTimeElapsed() {
    	return mTimeElapsed;
    }
    
    /** Returns last doc (absolute doc id) that was collected when the search time exceeded. */
    public int getLastDocCollected() {
    	return mLastDocCollected;
    }
	
}
