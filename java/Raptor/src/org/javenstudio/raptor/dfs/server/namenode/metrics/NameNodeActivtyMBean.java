package org.javenstudio.raptor.dfs.server.namenode.metrics;

import javax.management.ObjectName;

import org.javenstudio.raptor.metrics.util.MBeanUtil;
import org.javenstudio.raptor.metrics.util.MetricsDynamicMBeanBase;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

/**
 * 
 * This is the JMX MBean for reporting the NameNode Activity.
 * The MBean is register using the name
 *        "raptor:service=NameNode,name=NameNodeActivity"
 * 
 * Many of the activity metrics are sampled and averaged on an interval 
 * which can be specified in the metrics config file.
 * <p>
 * For the metrics that are sampled and averaged, one must specify 
 * a metrics context that does periodic update calls. Most metrics contexts do.
 * The default Null metrics context however does NOT. So if you aren't
 * using any other metrics context then you can turn on the viewing and averaging
 * of sampled metrics by  specifying the following two lines
 *  in the raptor-meterics.properties file:
*  <pre>
 *        dfs.class=org.javenstudio.raptor.metrics.spi.NullContextWithUpdateThread
 *        dfs.period=10
 *  </pre>
 *<p>
 * Note that the metrics are collected regardless of the context used.
 * The context with the update thread is used to average the data periodically
 *
 *
 *
 * Impl details: We use a dynamic mbean that gets the list of the metrics
 * from the metrics registry passed as an argument to the constructor
 */

public class NameNodeActivtyMBean extends MetricsDynamicMBeanBase {
  final private ObjectName mbeanName;

  protected NameNodeActivtyMBean(final MetricsRegistry mr) {
    super(mr, "Activity statistics at the NameNode");
    mbeanName = MBeanUtil.registerMBean("NameNode", "NameNodeActivity", this);
  }

  public void shutdown() {
    if (mbeanName != null)
      MBeanUtil.unregisterMBean(mbeanName);
  }
}

