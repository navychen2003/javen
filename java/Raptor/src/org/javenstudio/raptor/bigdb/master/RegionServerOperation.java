package org.javenstudio.raptor.bigdb.master;

import java.io.IOException;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.javenstudio.common.util.Logger;

abstract class RegionServerOperation implements Delayed {
  protected static final Logger LOG =
		  Logger.getLogger(RegionServerOperation.class);

  private long expire;
  protected final DBMaster master;
  /* How long we stay on queue.
   */
  private int delay;

  protected RegionServerOperation(DBMaster master) {
    this.master = master;
    this.delay = this.master.getConfiguration().
      getInt("bigdb.server.thread.wakefrequency", 10 * 1000);
    // Set the future time at which we expect to be released from the
    // DelayQueue we're inserted in on lease expiration.
    resetExpiration();
  }

  /**
   * Call before putting this back on the delay queue.
   * @return When we will expire next.
   */
  long resetExpiration() {
    // Set the future time at which we expect to be released from the
    // DelayQueue we're inserted in on lease expiration.
    this.expire = System.currentTimeMillis() + this.delay;
    return this.expire;
  }

  public long getDelay(TimeUnit unit) {
    return unit.convert(this.expire - System.currentTimeMillis(),
      TimeUnit.MILLISECONDS);
  }

  void setDelay(final int d) {
    this.delay = d;
  }

  public int compareTo(Delayed o) {
    return Long.valueOf(getDelay(TimeUnit.MILLISECONDS)
        - o.getDelay(TimeUnit.MILLISECONDS)).intValue();
  }

  protected void requeue() {
    this.master.getRegionServerOperationQueue().putOnDelayQueue(this);
  }

  @SuppressWarnings("unused")
  private long whenToExpire() {
    return System.currentTimeMillis() + this.delay;
  }

  protected boolean rootAvailable() {
    boolean available = true;
    if (this.master.getRegionManager().getRootRegionLocation() == null) {
      available = false;
      requeue();
    }
    return available;
  }

  protected boolean metaTableAvailable() {
    boolean available = true;
    if ((master.getRegionManager().numMetaRegions() !=
      master.getRegionManager().numOnlineMetaRegions()) ||
      master.getRegionManager().metaRegionsInTransition()) {
      // We can't proceed because not all of the meta regions are online.
      // We can't block either because that would prevent the meta region
      // online message from being processed. In order to prevent spinning
      // in the run queue, put this request on the delay queue to give
      // other threads the opportunity to get the meta regions on-line.
      if (LOG.isDebugEnabled()) {
        LOG.debug("numberOfMetaRegions: " +
            master.getRegionManager().numMetaRegions() +
            ", onlineMetaRegions.size(): " +
            master.getRegionManager().numOnlineMetaRegions());
        LOG.debug("Requeuing because not all meta regions are online");
      }
      available = false;
      requeue();
    }
    return available;
  }

  public int compareTo(RegionServerOperation other) {
    return getPriority() - other.getPriority();
  }

  // the Priority of this operation, 0 is lowest priority
  protected int getPriority() {
    return Integer.MAX_VALUE;
  }

  protected abstract boolean process() throws IOException;
}

