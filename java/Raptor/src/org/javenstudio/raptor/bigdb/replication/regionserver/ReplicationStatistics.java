package org.javenstudio.raptor.bigdb.replication.regionserver;

import org.javenstudio.raptor.bigdb.metrics.MetricsMBeanBase;
import org.javenstudio.raptor.metrics.util.MBeanUtil;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

import javax.management.ObjectName;

/**
 * Exports metrics recorded by {@link ReplicationSourceMetrics} as an MBean
 * for JMX monitoring.
 */
public class ReplicationStatistics extends MetricsMBeanBase {

  @SuppressWarnings("unused")
  private final ObjectName mbeanName;

  /**
   * Constructor to register the MBean
   * @param registry which rehistry to use
   * @param name name to get to this bean
   */
  public ReplicationStatistics(MetricsRegistry registry, String name) {
    super(registry, name);
    mbeanName = MBeanUtil.registerMBean("Replication", name, this);
  }
}

