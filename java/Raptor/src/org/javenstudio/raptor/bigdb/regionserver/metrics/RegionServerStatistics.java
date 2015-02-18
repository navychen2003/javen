package org.javenstudio.raptor.bigdb.regionserver.metrics;

import org.javenstudio.raptor.bigdb.metrics.MetricsMBeanBase;
import org.javenstudio.raptor.metrics.util.MBeanUtil;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

import javax.management.ObjectName;

/**
 * Exports metrics recorded by {@link RegionServerMetrics} as an MBean
 * for JMX monitoring.
 */
public class RegionServerStatistics extends MetricsMBeanBase {

  private final ObjectName mbeanName;

  public RegionServerStatistics(MetricsRegistry registry, String rsName) {
    super(registry, "RegionServerStatistics");
    mbeanName = MBeanUtil.registerMBean("RegionServer",
        "RegionServerStatistics", this);
  }

  public void shutdown() {
    if (mbeanName != null)
      MBeanUtil.unregisterMBean(mbeanName);
  }

}
