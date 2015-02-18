package org.javenstudio.raptor.bigdb.regionserver.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.io.dbfile.DBFile;
import org.javenstudio.raptor.bigdb.metrics.MetricsRate;
import org.javenstudio.raptor.bigdb.regionserver.wal.DBLog;
import org.javenstudio.raptor.bigdb.util.Strings;
import org.javenstudio.raptor.metrics.MetricsContext;
import org.javenstudio.raptor.metrics.MetricsRecord;
import org.javenstudio.raptor.metrics.MetricsUtil;
import org.javenstudio.raptor.metrics.Updater;
import org.javenstudio.raptor.metrics.jvm.JvmMetrics;
import org.javenstudio.raptor.metrics.util.MetricsIntValue;
import org.javenstudio.raptor.metrics.util.MetricsLongValue;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;
import org.javenstudio.raptor.metrics.util.MetricsTimeVaryingRate;

/**
 * This class is for maintaining the various regionserver statistics
 * and publishing them through the metrics interfaces.
 * <p>
 * This class has a number of metrics variables that are publicly accessible;
 * these variables (objects) have methods to update their values.
 */
public class RegionServerMetrics implements Updater {
  private final Logger LOG = Logger.getLogger(this.getClass());
  private final MetricsRecord metricsRecord;
  private long lastUpdate = System.currentTimeMillis();
  private static final int MB = 1024*1024;
  private MetricsRegistry registry = new MetricsRegistry();
  private final RegionServerStatistics statistics;

  public final MetricsTimeVaryingRate atomicIncrementTime =
      new MetricsTimeVaryingRate("atomicIncrementTime", registry);

  /**
   * Count of regions carried by this regionserver
   */
  public final MetricsIntValue regions =
    new MetricsIntValue("regions", registry);

  /**
   * Block cache size.
   */
  public final MetricsLongValue blockCacheSize = new MetricsLongValue("blockCacheSize", registry);

  /**
   * Block cache free size.
   */
  public final MetricsLongValue blockCacheFree = new MetricsLongValue("blockCacheFree", registry);

  /**
   * Block cache item count.
   */
  public final MetricsLongValue blockCacheCount = new MetricsLongValue("blockCacheCount", registry);

  /**
   * Block hit ratio.
   */
  public final MetricsIntValue blockCacheHitRatio = new MetricsIntValue("blockCacheHitRatio", registry);

  /*
   * Count of requests to the regionservers since last call to metrics update
   */
  private final MetricsRate requests = new MetricsRate("requests", registry);

  /**
   * Count of stores open on the regionserver.
   */
  public final MetricsIntValue stores = new MetricsIntValue("stores", registry);

  /**
   * Count of storefiles open on the regionserver.
   */
  public final MetricsIntValue storefiles = new MetricsIntValue("storefiles", registry);

  /**
   * Sum of all the storefile index sizes in this regionserver in MB
   */
  public final MetricsIntValue storefileIndexSizeMB =
    new MetricsIntValue("storefileIndexSizeMB", registry);

  /**
   * Sum of all the memstore sizes in this regionserver in MB
   */
  public final MetricsIntValue memstoreSizeMB =
    new MetricsIntValue("memstoreSizeMB", registry);

  /**
   * Size of the compaction queue.
   */
  public final MetricsIntValue compactionQueueSize =
    new MetricsIntValue("compactionQueueSize", registry);

  /**
   * filesystem read latency
   */
  public final MetricsTimeVaryingRate fsReadLatency =
    new MetricsTimeVaryingRate("fsReadLatency", registry);

  /**
   * filesystem write latency
   */
  public final MetricsTimeVaryingRate fsWriteLatency =
    new MetricsTimeVaryingRate("fsWriteLatency", registry);

  /**
   * filesystem sync latency
   */
  public final MetricsTimeVaryingRate fsSyncLatency =
    new MetricsTimeVaryingRate("fsSyncLatency", registry);

  public RegionServerMetrics() {
    MetricsContext context = MetricsUtil.getContext("bigdb");
    metricsRecord = MetricsUtil.createRecord(context, "regionserver");
    String name = Thread.currentThread().getName();
    metricsRecord.setTag("RegionServer", name);
    context.registerUpdater(this);
    // Add jvmmetrics.
    JvmMetrics.init("RegionServer", name);

    // export for JMX
    statistics = new RegionServerStatistics(this.registry, name);

    LOG.info("Initialized");
  }

  public void shutdown() {
    if (statistics != null)
      statistics.shutdown();
  }

  /**
   * Since this object is a registered updater, this method will be called
   * periodically, e.g. every 5 seconds.
   * @param unused unused argument
   */
  public void doUpdates(MetricsContext unused) {
    synchronized (this) {
      this.stores.pushMetric(this.metricsRecord);
      this.storefiles.pushMetric(this.metricsRecord);
      this.storefileIndexSizeMB.pushMetric(this.metricsRecord);
      this.memstoreSizeMB.pushMetric(this.metricsRecord);
      this.regions.pushMetric(this.metricsRecord);
      this.requests.pushMetric(this.metricsRecord);
      this.compactionQueueSize.pushMetric(this.metricsRecord);
      this.blockCacheSize.pushMetric(this.metricsRecord);
      this.blockCacheFree.pushMetric(this.metricsRecord);
      this.blockCacheCount.pushMetric(this.metricsRecord);
      this.blockCacheHitRatio.pushMetric(this.metricsRecord);

      // Mix in DBFile and DBLog metrics
      // Be careful. Here is code for MTVR from up in hadoop:
      // public synchronized void inc(final int numOps, final long time) {
      //   currentData.numOperations += numOps;
      //   currentData.time += time;
      //   long timePerOps = time/numOps;
      //    minMax.update(timePerOps);
      // }
      // Means you can't pass a numOps of zero or get a ArithmeticException / by zero.
      int ops = (int)DBFile.getReadOps();
      if (ops != 0) this.fsReadLatency.inc(ops, DBFile.getReadTime());
      ops = (int)DBFile.getWriteOps();
      if (ops != 0) this.fsWriteLatency.inc(ops, DBFile.getWriteTime());
      // mix in DBLog metrics
      ops = (int)DBLog.getWriteOps();
      if (ops != 0) this.fsWriteLatency.inc(ops, DBLog.getWriteTime());
      ops = (int)DBLog.getSyncOps();
      if (ops != 0) this.fsSyncLatency.inc(ops, DBLog.getSyncTime());

      // push the result
      this.fsReadLatency.pushMetric(this.metricsRecord);
      this.fsWriteLatency.pushMetric(this.metricsRecord);
      this.fsSyncLatency.pushMetric(this.metricsRecord);
    }
    this.metricsRecord.update();
    this.lastUpdate = System.currentTimeMillis();
  }

  public void resetAllMinMax() {
    this.atomicIncrementTime.resetMinMax();
    this.fsReadLatency.resetMinMax();
    this.fsWriteLatency.resetMinMax();
  }

  /**
   * @return Count of requests.
   */
  public float getRequests() {
    return this.requests.getPreviousIntervalValue();
  }

  /**
   * @param inc How much to add to requests.
   */
  public void incrementRequests(final int inc) {
    this.requests.inc(inc);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    int seconds = (int)((System.currentTimeMillis() - this.lastUpdate)/1000);
    if (seconds == 0) {
      seconds = 1;
    }
    sb = Strings.appendKeyValue(sb, "request",
      Float.valueOf(this.requests.getPreviousIntervalValue()));
    sb = Strings.appendKeyValue(sb, "regions",
      Integer.valueOf(this.regions.get()));
    sb = Strings.appendKeyValue(sb, "stores",
      Integer.valueOf(this.stores.get()));
    sb = Strings.appendKeyValue(sb, "storefiles",
      Integer.valueOf(this.storefiles.get()));
    sb = Strings.appendKeyValue(sb, "storefileIndexSize",
      Integer.valueOf(this.storefileIndexSizeMB.get()));
    sb = Strings.appendKeyValue(sb, "memstoreSize",
      Integer.valueOf(this.memstoreSizeMB.get()));
    sb = Strings.appendKeyValue(sb, "compactionQueueSize",
      Integer.valueOf(this.compactionQueueSize.get()));
    // Duplicate from jvmmetrics because metrics are private there so
    // inaccessible.
    MemoryUsage memory =
      ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    sb = Strings.appendKeyValue(sb, "usedHeap",
      Long.valueOf(memory.getUsed()/MB));
    sb = Strings.appendKeyValue(sb, "maxHeap",
      Long.valueOf(memory.getMax()/MB));
    sb = Strings.appendKeyValue(sb, this.blockCacheSize.getName(),
        Long.valueOf(this.blockCacheSize.get()));
    sb = Strings.appendKeyValue(sb, this.blockCacheFree.getName(),
        Long.valueOf(this.blockCacheFree.get()));
    sb = Strings.appendKeyValue(sb, this.blockCacheCount.getName(),
        Long.valueOf(this.blockCacheCount.get()));
    sb = Strings.appendKeyValue(sb, this.blockCacheHitRatio.getName(),
        Long.valueOf(this.blockCacheHitRatio.get()));
    return sb.toString();
  }
}

