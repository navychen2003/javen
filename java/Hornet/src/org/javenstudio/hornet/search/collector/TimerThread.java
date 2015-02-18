package org.javenstudio.hornet.search.collector;

import org.javenstudio.common.indexdb.ThreadInterruptedException;
import org.javenstudio.common.indexdb.util.Counter;

/**
 * Thread used to timeout search requests.
 * Can be stopped completely with {@link TimerThread#stopTimer()}
 * 
 */
public class TimerThread extends Thread {

    public static final String THREAD_NAME = "TimeLimitedCollector timer thread";
    public static final int DEFAULT_RESOLUTION = 20;
    
    // NOTE: we can avoid explicit synchronization here for several reasons:
    // * updates to volatile long variables are atomic
    // * only single thread modifies this value
    // * use of volatile keyword ensures that it does not reside in
    //   a register, but in main memory (so that changes are visible to
    //   other threads).
    // * visibility of changes does not need to be instantaneous, we can
    //   afford losing a tick or two.
    //
    // See section 17 of the Java Language Specification for details.
    private volatile long mTime = 0;
    private volatile boolean mStop = false;
    private volatile long mResolution;
    private final Counter mCounter;
    
    public TimerThread(long resolution, Counter counter) {
    	super(THREAD_NAME);
      
    	mResolution = resolution;
    	mCounter = counter;
    	setDaemon(true);
    }
    
    public TimerThread(Counter counter) {
    	this(DEFAULT_RESOLUTION, counter);
    }

    @Override
    public void run() {
    	while (!mStop) {
    		// TODO: Use System.nanoTime() when Lucene moves to Java SE 5.
    		mCounter.addAndGet(mResolution);
    		try {
    			Thread.sleep(mResolution);
    		} catch (InterruptedException ie) {
    			throw new ThreadInterruptedException(ie);
    		}
    	}
    }

    public Counter getCounter() { 
    	return mCounter;
    }
    
    /**
     * Get the timer value in milliseconds.
     */
    public long getMilliseconds() {
    	return mTime;
    }
    
    /**
     * Stops the timer thread 
     */
    public void stopTimer() {
    	mStop = true;
    }
    
    /** 
     * Return the timer resolution.
     * @see #setResolution(long)
     */
    public long getResolution() {
    	return mResolution;
    }
    
    /**
     * Set the timer resolution.
     * The default timer resolution is 20 milliseconds. 
     * This means that a search required to take no longer than 
     * 800 milliseconds may be stopped after 780 to 820 milliseconds.
     * <br>Note that: 
     * <ul>
     * <li>Finer (smaller) resolution is more accurate but less efficient.</li>
     * <li>Setting resolution to less than 5 milliseconds will be silently modified to 5 milliseconds.</li>
     * <li>Setting resolution smaller than current resolution might take effect only after current 
     * resolution. (Assume current resolution of 20 milliseconds is modified to 5 milliseconds, 
     * then it can take up to 20 milliseconds for the change to have effect.</li>
     * </ul>      
     */
    public void setResolution(long resolution) {
    	// 5 milliseconds is about the minimum reasonable time for a Object.wait(long) call.
    	mResolution = Math.max(resolution, 5); 
    }
	
}
