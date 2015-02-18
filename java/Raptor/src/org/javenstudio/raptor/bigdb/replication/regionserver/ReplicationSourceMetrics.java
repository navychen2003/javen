package org.javenstudio.raptor.bigdb.replication.regionserver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.javenstudio.raptor.bigdb.metrics.MetricsRate;
import org.javenstudio.raptor.metrics.MetricsContext;
import org.javenstudio.raptor.metrics.MetricsRecord;
import org.javenstudio.raptor.metrics.MetricsUtil;
import org.javenstudio.raptor.metrics.Updater;
import org.javenstudio.raptor.metrics.util.MetricsIntValue;
import org.javenstudio.raptor.metrics.util.MetricsLongValue;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

/**
 * This class is for maintaining the various replication statistics
 * for a source and publishing them through the metrics interfaces.
 */
public class ReplicationSourceMetrics implements Updater {
  private final MetricsRecord metricsRecord;
  private MetricsRegistry registry = new MetricsRegistry();

  /** Rate of shipped operations by the source */
  public final MetricsRate shippedOpsRate =
      new MetricsRate("shippedOpsRate", registry);

  /** Rate of shipped batches by the source */
  public final MetricsRate shippedBatchesRate =
      new MetricsRate("shippedBatchesRate", registry);

  /** Rate of log entries (can be multiple Puts) read from the logs */
  public final MetricsRate logEditsReadRate =
      new MetricsRate("logEditsReadRate", registry);

  /** Rate of log entries filtered by the source */
  public final MetricsRate logEditsFilteredRate =
      new MetricsRate("logEditsFilteredRate", registry);

  /** Age of the last operation that was shipped by the source */
  private final MetricsLongValue ageOfLastShippedOp =
      new MetricsLongValue("ageOfLastShippedOp", registry);

  /**
   * Current size of the queue of logs to replicate,
   * excluding the one being processed at the moment
   */
  public final MetricsIntValue sizeOfLogQueue =
      new MetricsIntValue("sizeOfLogQueue", registry);

  /**
   * Constructor used to register the metrics
   * @param id Name of the source this class is monitoring
   */
  public ReplicationSourceMetrics(String id) {
    MetricsContext context = MetricsUtil.getContext("bigdb");
    String name = Thread.currentThread().getName();
    metricsRecord = MetricsUtil.createRecord(context, "replication");
    metricsRecord.setTag("RegionServer", name);
    context.registerUpdater(this);
    try {
      id = URLEncoder.encode(id, "UTF8");
    } catch (UnsupportedEncodingException e) {
      id = "CAN'T ENCODE UTF8";
    }
    // export for JMX
    new ReplicationStatistics(this.registry, "ReplicationSource for " + id);
  }

  /**
   * Set the age of the last edit that was shipped
   * @param timestamp write time of the edit
   */
  public void setAgeOfLastShippedOp(long timestamp) {
    ageOfLastShippedOp.set(System.currentTimeMillis() - timestamp);
  }

  @Override
  public void doUpdates(MetricsContext metricsContext) {
    synchronized (this) {
      this.shippedOpsRate.pushMetric(this.metricsRecord);
      this.shippedBatchesRate.pushMetric(this.metricsRecord);
      this.logEditsReadRate.pushMetric(this.metricsRecord);
      this.logEditsFilteredRate.pushMetric(this.metricsRecord);
      this.ageOfLastShippedOp.pushMetric(this.metricsRecord);
      this.sizeOfLogQueue.pushMetric(this.metricsRecord);
    }
    this.metricsRecord.update();
  }
}

