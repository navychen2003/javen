package org.javenstudio.raptor.bigdb.replication.regionserver;

import org.javenstudio.raptor.bigdb.metrics.MetricsRate;
import org.javenstudio.raptor.metrics.MetricsContext;
import org.javenstudio.raptor.metrics.MetricsRecord;
import org.javenstudio.raptor.metrics.MetricsUtil;
import org.javenstudio.raptor.metrics.Updater;
import org.javenstudio.raptor.metrics.util.MetricsLongValue;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

/**
 * This class is for maintaining the various replication statistics
 * for a sink and publishing them through the metrics interfaces.
 */
public class ReplicationSinkMetrics implements Updater {
  private final MetricsRecord metricsRecord;
  private MetricsRegistry registry = new MetricsRegistry();
  @SuppressWarnings("unused")
  private static ReplicationSinkMetrics instance;

  /** Rate of operations applied by the sink */
  public final MetricsRate appliedOpsRate =
      new MetricsRate("appliedOpsRate", registry);

  /** Rate of batches (of operations) applied by the sink */
  public final MetricsRate appliedBatchesRate =
      new MetricsRate("appliedBatchesRate", registry);

  /** Age of the last operation that was applied by the sink */
  private final MetricsLongValue ageOfLastAppliedOp =
      new MetricsLongValue("ageOfLastAppliedOp", registry);

  /**
   * Constructor used to register the metrics
   */
  public ReplicationSinkMetrics() {
    MetricsContext context = MetricsUtil.getContext("bigdb");
    String name = Thread.currentThread().getName();
    metricsRecord = MetricsUtil.createRecord(context, "replication");
    metricsRecord.setTag("RegionServer", name);
    context.registerUpdater(this);
    // export for JMX
    new ReplicationStatistics(this.registry, "ReplicationSink");
  }

  /**
   * Set the age of the last edit that was applied
   * @param timestamp write time of the edit
   */
  public void setAgeOfLastAppliedOp(long timestamp) {
    ageOfLastAppliedOp.set(System.currentTimeMillis() - timestamp);
  }
  @Override
  public void doUpdates(MetricsContext metricsContext) {
    synchronized (this) {
      this.appliedOpsRate.pushMetric(this.metricsRecord);
      this.appliedBatchesRate.pushMetric(this.metricsRecord);
      this.ageOfLastAppliedOp.pushMetric(this.metricsRecord);
    }
    this.metricsRecord.update();
  }
}

