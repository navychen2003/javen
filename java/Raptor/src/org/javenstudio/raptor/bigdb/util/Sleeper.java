package org.javenstudio.raptor.bigdb.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.common.util.Logger;

/**
 * Sleeper for current thread.
 * Sleeps for passed period.  Also checks passed boolean and if interrupted,
 * will return if the flag is set (rather than go back to sleep until its
 * sleep time is up).
 */
public class Sleeper {
  private final Logger LOG = Logger.getLogger(Sleeper.class);
  private final int period;
  private final AtomicBoolean stop;
  private static final long MINIMAL_DELTA_FOR_LOGGING = 10000;

  private final Object sleepLock = new Object();
  private boolean triggerWake = false;

  /**
   * @param sleep sleep time in milliseconds
   * @param stop flag for when we stop
   */
  public Sleeper(final int sleep, final AtomicBoolean stop) {
    this.period = sleep;
    this.stop = stop;
  }

  /**
   * Sleep for period.
   */
  public void sleep() {
    sleep(System.currentTimeMillis());
  }

  /**
   * If currently asleep, stops sleeping; if not asleep, will skip the next
   * sleep cycle.
   */
  public void skipSleepCycle() {
    synchronized (sleepLock) {
      triggerWake = true;
      sleepLock.notify();
    }
  }

  /**
   * Sleep for period adjusted by passed <code>startTime<code>
   * @param startTime Time some task started previous to now.  Time to sleep
   * will be docked current time minus passed <code>startTime<code>.
   */
  public void sleep(final long startTime) {
    if (this.stop.get()) {
      return;
    }
    long now = System.currentTimeMillis();
    long waitTime = this.period - (now - startTime);
    if (waitTime > this.period) {
      LOG.warn("Calculated wait time > " + this.period +
        "; setting to this.period: " + System.currentTimeMillis() + ", " +
        startTime);
      waitTime = this.period;
    }
    while (waitTime > 0) {
      long woke = -1;
      try {
        synchronized (sleepLock) {
          if (triggerWake) break;
          sleepLock.wait(waitTime);
        }
        woke = System.currentTimeMillis();
        long slept = woke - now;
        if (slept - this.period > MINIMAL_DELTA_FOR_LOGGING) {
          LOG.warn("We slept " + slept + "ms instead of " + this.period +
              "ms, this is likely due to a long " +
              "garbage collecting pause and it's usually bad, " +
              "see http://wiki.apache.org/hadoop/Hbase/Troubleshooting#A9");
        }
      } catch(InterruptedException iex) {
        // We we interrupted because we're meant to stop?  If not, just
        // continue ignoring the interruption
        if (this.stop.get()) {
          return;
        }
      }
      // Recalculate waitTime.
      woke = (woke == -1)? System.currentTimeMillis(): woke;
      waitTime = this.period - (woke - startTime);
    }
    triggerWake = false;
  }
}

