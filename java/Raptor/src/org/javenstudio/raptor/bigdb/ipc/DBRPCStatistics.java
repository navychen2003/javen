package org.javenstudio.raptor.bigdb.ipc;

import org.javenstudio.raptor.metrics.util.MBeanUtil;
import org.javenstudio.raptor.metrics.util.MetricsDynamicMBeanBase;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

import javax.management.ObjectName;

/**
 * Exports BigDB RPC statistics recorded in {@link DBRPCMetrics} as an MBean
 * for JMX monitoring.
 */
public class DBRPCStatistics extends MetricsDynamicMBeanBase {
  private final ObjectName mbeanName;

  public DBRPCStatistics(MetricsRegistry registry,
      String hostName, String port) {
	  super(registry, "DBRPCStatistics");

    String name = String.format("RPCStatistics-%s",
        (port != null ? port : "unknown"));

    mbeanName = MBeanUtil.registerMBean("BigDB", name, this);
  }

  public void shutdown() {
    if (mbeanName != null)
      MBeanUtil.unregisterMBean(mbeanName);
  }

}

