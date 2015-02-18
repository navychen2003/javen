package org.javenstudio.raptor.bigdb.master.metrics;

import javax.management.ObjectName;

import org.javenstudio.raptor.bigdb.metrics.MetricsMBeanBase;
import org.javenstudio.raptor.metrics.util.MBeanUtil;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

/**
 * Exports the {@link MasterMetrics} statistics as an MBean
 * for JMX.
 */
public class MasterStatistics extends MetricsMBeanBase {
  private final ObjectName mbeanName;

  public MasterStatistics(MetricsRegistry registry) {
    super(registry, "MasterStatistics");
    mbeanName = MBeanUtil.registerMBean("Master", "MasterStatistics", this);
  }

  public void shutdown() {
    if (mbeanName != null)
      MBeanUtil.unregisterMBean(mbeanName);
  }
}

