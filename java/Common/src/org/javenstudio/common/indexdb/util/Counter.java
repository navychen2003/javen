package org.javenstudio.common.indexdb.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple counter class
 * 
 */
public abstract class Counter {

  /**
   * Adds the given delta to the counters current value
   * 
   * @param delta
   *          the delta to add
   * @return the counters updated value
   */
  public abstract long addAndGet(long delta);

  /**
   * Returns the counters current value
   * 
   * @return the counters current value
   */
  public abstract long get();

  /**
   * Returns a new counter. The returned counter is not thread-safe.
   */
  public static Counter newCounter() {
    return newCounter(false);
  }

  /**
   * Returns a new counter.
   * 
   * @param threadSafe
   *          <code>true</code> if the returned counter can be used by multiple
   *          threads concurrently.
   * @return a new counter.
   */
  public static Counter newCounter(boolean threadSafe) {
    return threadSafe ? new AtomicCounter() : new SerialCounter();
  }

  private final static class SerialCounter extends Counter {
    private long count = 0;

    @Override
    public long addAndGet(long delta) {
      return count += delta;
    }

    @Override
    public long get() {
      return count;
    };
  }

  private final static class AtomicCounter extends Counter {
    private final AtomicLong count = new AtomicLong();

    @Override
    public long addAndGet(long delta) {
      return count.addAndGet(delta);
    }

    @Override
    public long get() {
      return count.get();
    }
  }
  
}
