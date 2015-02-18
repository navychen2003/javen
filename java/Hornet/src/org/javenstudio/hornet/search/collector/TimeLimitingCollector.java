package org.javenstudio.hornet.search.collector;

import java.io.IOException;

import org.javenstudio.common.indexdb.IAtomicReaderRef;
import org.javenstudio.common.indexdb.ICollector;
import org.javenstudio.common.indexdb.IScorer;
import org.javenstudio.common.indexdb.search.Collector;
import org.javenstudio.common.indexdb.util.Counter;

/**
 * The {@link TimeLimitingCollector} is used to timeout search requests that
 * take longer than the maximum allowed search time limit. After this time is
 * exceeded, the search thread is stopped by throwing a
 * {@link TimeExceededException}.
 */
public class TimeLimitingCollector extends Collector {

	private final Counter mClock;
	private final long mTicksAllowed;
	
	private long mT0 = Long.MIN_VALUE;
	private long mTimeout = Long.MIN_VALUE;
	
	private ICollector mCollector;
  
	private boolean mGreedy = false;
	private int mDocBase;

	/**
	 * Create a TimeLimitedCollector wrapper over another {@link Collector} with a specified timeout.
	 * @param collector the wrapped {@link Collector}
	 * @param clock the timer clock
	 * @param ticksAllowed max time allowed for collecting
	 * hits after which {@link TimeExceededException} is thrown
	 */
	public TimeLimitingCollector(final ICollector collector, 
			Counter clock, final long ticksAllowed) {
		mCollector = collector;
		mClock = clock;
		mTicksAllowed = ticksAllowed;
	}
  
	/**
	 * Sets the baseline for this collector. By default the collectors baseline is 
	 * initialized once the first reader is passed to the collector. 
	 * To include operations executed in prior to the actual document collection
	 * set the baseline through this method in your prelude.
	 * <p>
	 * Example usage:
	 * <pre class="prettyprint">
	 *   Counter clock = ...;
	 *   long baseline = clock.get();
	 *   // ... prepare search
	 *   TimeLimitingCollector collector = new TimeLimitingCollector(c, clock, numTicks);
	 *   collector.setBaseline(baseline);
	 *   indexSearcher.search(query, collector);
	 * </pre>
	 * </p>
	 * @see #setBaseline() 
	 */
	public void setBaseline(long clockTime) {
		mT0 = clockTime;
		mTimeout = mT0 + mTicksAllowed;
	}
  
	/**
	 * Syntactic sugar for {@link #setBaseline(long)} using {@link Counter#get()}
	 * on the clock passed to the construcutor.
	 */
	public void setBaseline() {
		setBaseline(mClock.get());
	}
  
	/**
	 * Checks if this time limited collector is greedy in collecting the last hit.
	 * A non greedy collector, upon a timeout, would throw a {@link TimeExceededException} 
	 * without allowing the wrapped collector to collect current doc. A greedy one would 
	 * first allow the wrapped hit collector to collect current doc and only then 
	 * throw a {@link TimeExceededException}.
	 * @see #setGreedy(boolean)
	 */
	public boolean isGreedy() {
		return mGreedy;
	}

	/**
	 * Sets whether this time limited collector is greedy.
	 * @param greedy true to make this time limited greedy
	 * @see #isGreedy()
	 */
	public void setGreedy(boolean greedy) {
		mGreedy = greedy;
	}
  
	/**
	 * Calls {@link Collector#collect(int)} on the decorated {@link Collector}
	 * unless the allowed time has passed, in which case it throws an exception.
	 * 
	 * @throws TimeExceededException
	 *           if the time allowed has exceeded.
	 */
	@Override
	public void collect(final int doc) throws IOException {
		final long time = mClock.get();
		if (mTimeout < time) {
			if (mGreedy) 
				mCollector.collect(doc);
			
			throw new TimeExceededException(mTimeout-mT0, time-mT0, mDocBase + doc);
		}
		
		mCollector.collect(doc);
	}
  
	@Override
	public void setNextReader(IAtomicReaderRef context) throws IOException {
		mCollector.setNextReader(context);
		mDocBase = context.getDocBase();
		if (Long.MIN_VALUE == mT0) 
			setBaseline();
	}
  
	@Override
	public void setScorer(IScorer scorer) throws IOException {
		mCollector.setScorer(scorer);
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return mCollector.acceptsDocsOutOfOrder();
	}
  
	/**
	 * This is so the same timer can be used with a multi-phase search process such as grouping. 
	 * We don't want to create a new TimeLimitingCollector for each phase because that would 
	 * reset the timer for each phase.  Once time is up subsequent phases need to timeout quickly.
	 *
	 * @param collector The actual collector performing search functionality
	 */
	public void setCollector(ICollector collector) {
		mCollector = collector;
	}

	/**
	 * Returns the global TimerThreads {@link Counter}
	 * <p>
	 * Invoking this creates may create a new instance of {@link TimerThread} iff
	 * the global {@link TimerThread} has never been accessed before. The thread
	 * returned from this method is started on creation and will be alive unless
	 * you stop the {@link TimerThread} via {@link TimerThread#stopTimer()}.
	 * </p>
	 * @return the global TimerThreads {@link Counter}
	 */
	public static Counter getGlobalCounter() {
		return TimerThreadHolder.THREAD.getCounter();
	}
  
	/**
	 * Returns the global {@link TimerThread}.
	 * <p>
	 * Invoking this creates may create a new instance of {@link TimerThread} iff
	 * the global {@link TimerThread} has never been accessed before. The thread
	 * returned from this method is started on creation and will be alive unless
	 * you stop the {@link TimerThread} via {@link TimerThread#stopTimer()}.
	 * </p>
	 * 
	 * @return the global {@link TimerThread}
	 */
	public static TimerThread getGlobalTimerThread() {
		return TimerThreadHolder.THREAD;
	}
  
	private static final class TimerThreadHolder {
		static final TimerThread THREAD;
		static {
			THREAD = new TimerThread(Counter.newCounter(true));
			THREAD.start();
		}
	}

}
