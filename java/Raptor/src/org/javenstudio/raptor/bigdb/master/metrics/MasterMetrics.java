package org.javenstudio.raptor.bigdb.master.metrics;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.metrics.MetricsRate;
import org.javenstudio.raptor.metrics.MetricsContext;
import org.javenstudio.raptor.metrics.MetricsRecord;
import org.javenstudio.raptor.metrics.MetricsUtil;
import org.javenstudio.raptor.metrics.Updater;
import org.javenstudio.raptor.metrics.jvm.JvmMetrics;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;


/**
 * This class is for maintaining the various master statistics
 * and publishing them through the metrics interfaces.
 * <p>
 * This class has a number of metrics variables that are publicly accessible;
 * these variables (objects) have methods to update their values.
 */
public class MasterMetrics implements Updater {
  private static final Logger LOG = Logger.getLogger(MasterMetrics.class);
  private final MetricsRecord metricsRecord;
  private final MetricsRegistry registry = new MetricsRegistry();
  private final MasterStatistics masterStatistics;
  /*
   * Count of requests to the cluster since last call to metrics update
   */
  private final MetricsRate cluster_requests =
    new MetricsRate("cluster_requests", registry);

  public MasterMetrics(final String name) {
    MetricsContext context = MetricsUtil.getContext("bigdb");
    metricsRecord = MetricsUtil.createRecord(context, "master");
    metricsRecord.setTag("Master", name);
    context.registerUpdater(this);
    JvmMetrics.init("Master", name);

    // expose the MBean for metrics
    masterStatistics = new MasterStatistics(this.registry);

    LOG.info("Initialized");
  }

  public void shutdown() {
    if (masterStatistics != null)
      masterStatistics.shutdown();
  }

  /**
   * Since this object is a registered updater, this method will be called
   * periodically, e.g. every 5 seconds.
   * @param unused
   */
  public void doUpdates(MetricsContext unused) {
    synchronized (this) {
      this.cluster_requests.pushMetric(metricsRecord);
    }
    this.metricsRecord.update();
  }

  public void resetAllMinMax() {
    // Nothing to do
  }

  /**
   * @return Count of requests.
   */
  public float getRequests() {
    return this.cluster_requests.getPreviousIntervalValue();
  }

  /**
   * @param inc How much to add to requests.
   */
  public void incrementRequests(final int inc) {
    this.cluster_requests.inc(inc);
  }
}
