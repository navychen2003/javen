package org.javenstudio.raptor.bigdb;

import java.util.concurrent.atomic.AtomicBoolean;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.util.Sleeper;

/**
 * Chore is a task performed on a period in bigdb.  The chore is run in its own
 * thread. This base abstract class provides while loop and sleeping facility.
 * If an unhandled exception, the threads exit is logged.
 * Implementers just need to add checking if there is work to be done and if
 * so, do it.  Its the base of most of the chore threads in bigdb.
 *
 * Don't subclass Chore if the task relies on being woken up for something to
 * do, such as an entry being added to a queue, etc.
 */
public abstract class Chore extends Thread {
  private static final Logger LOG = Logger.getLogger(Chore.class);
  private final Sleeper sleeper;
  protected volatile AtomicBoolean stop;

  /**
   * @param p Period at which we should run.  Will be adjusted appropriately
   * should we find work and it takes time to complete.
   * @param s When this flag is set to true, this thread will cleanup and exit
   * cleanly.
   */
  public Chore(String name, final int p, final AtomicBoolean s) {
    super(name);
    this.sleeper = new Sleeper(p, s);
    this.stop = s;
  }

  /**
   * @see java.lang.Thread#run()
   */
  @Override
  public void run() {
    try {
      boolean initialChoreComplete = false;
      while (!this.stop.get()) {
        long startTime = System.currentTimeMillis();
        try {
          if (!initialChoreComplete) {
            initialChoreComplete = initialChore();
          } else {
            chore();
          }
        } catch (Exception e) {
          LOG.error("Caught exception", e);
          if (this.stop.get()) {
            continue;
          }
        }
        this.sleeper.sleep(startTime);
      }
    } catch (Throwable t) {
      LOG.fatal("Caught error. Starting shutdown.", t);
      this.stop.set(true);
    } finally {
      LOG.info(getName() + " exiting");
    }
  }

  /**
   * If the thread is currently sleeping, trigger the core to happen immediately.
   * If it's in the middle of its operation, will begin another operation
   * immediately after finishing this one.
   */
  public void triggerNow() {
    this.sleeper.skipSleepCycle();
  }

  /**
   * Override to run a task before we start looping.
   * @return true if initial chore was successful
   */
  protected boolean initialChore() {
    // Default does nothing.
    return true;
  }

  /**
   * Look for chores.  If any found, do them else just return.
   */
  protected abstract void chore();

  /**
   * Sleep for period.
   */
  protected void sleep() {
    this.sleeper.sleep();
  }
}

