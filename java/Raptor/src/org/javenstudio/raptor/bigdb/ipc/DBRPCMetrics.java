package org.javenstudio.raptor.bigdb.ipc;

import java.lang.reflect.Method;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.metrics.MetricsContext;
import org.javenstudio.raptor.metrics.MetricsRecord;
import org.javenstudio.raptor.metrics.MetricsUtil;
import org.javenstudio.raptor.metrics.Updater;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;
import org.javenstudio.raptor.metrics.util.MetricsTimeVaryingRate;

/**
 *
 * This class is for maintaining  the various RPC statistics
 * and publishing them through the metrics interfaces.
 * This also registers the JMX MBean for RPC.
 * <p>
 * This class has a number of metrics variables that are publicly accessible;
 * these variables (objects) have methods to update their values;
 * for example:
 *  <p> {@link #rpcQueueTime}.inc(time)
 *
 */
public class DBRPCMetrics implements Updater {
  private MetricsRecord metricsRecord;
  private static Logger LOG = Logger.getLogger(DBRPCMetrics.class);
  private final DBRPCStatistics rpcStatistics;

  public DBRPCMetrics(String hostName, String port) {
    MetricsContext context = MetricsUtil.getContext("rpc");
    metricsRecord = MetricsUtil.createRecord(context, "metrics");

    metricsRecord.setTag("port", port);

    LOG.info("Initializing RPC Metrics with hostName="
        + hostName + ", port=" + port);

    context.registerUpdater(this);

    initMethods(DBMasterInterface.class);
    initMethods(DBMasterRegionInterface.class);
    initMethods(DBRegionInterface.class);
    rpcStatistics = new DBRPCStatistics(this.registry, hostName, port);
  }


  /**
   * The metrics variables are public:
   *  - they can be set directly by calling their set/inc methods
   *  -they can also be read directly - e.g. JMX does this.
   */
  public final MetricsRegistry registry = new MetricsRegistry();

  public MetricsTimeVaryingRate rpcQueueTime = new MetricsTimeVaryingRate("RpcQueueTime", registry);
  public MetricsTimeVaryingRate rpcProcessingTime = new MetricsTimeVaryingRate("RpcProcessingTime", registry);

  //public Map <String, MetricsTimeVaryingRate> metricsList = Collections.synchronizedMap(new HashMap<String, MetricsTimeVaryingRate>());

  private void initMethods(Class<? extends DBRPCProtocolVersion> protocol) {
    for (Method m : protocol.getDeclaredMethods()) {
      if (get(m.getName()) == null)
        create(m.getName());
    }
  }

  private MetricsTimeVaryingRate get(String key) {
    return (MetricsTimeVaryingRate) registry.get(key);
  }
  private MetricsTimeVaryingRate create(String key) {
    return new MetricsTimeVaryingRate(key, this.registry);
  }

  public void inc(String name, int amt) {
    MetricsTimeVaryingRate m = get(name);
    if (m == null) {
      LOG.warn("Got inc() request for method that doesnt exist: " +
      name);
      return; // ignore methods that dont exist.
    }
    m.inc(amt);
  }

  public void createMetrics(Class<?> []ifaces) {
    for (Class<?> iface : ifaces) {
      Method[] methods = iface.getMethods();
      for (Method method : methods) {
        if (get(method.getName()) == null)
          create(method.getName());
      }
    }
  }

  /**
   * Push the metrics to the monitoring subsystem on doUpdate() call.
   * @param context ctx
   */
  public void doUpdates(MetricsContext context) {
    rpcQueueTime.pushMetric(metricsRecord);
    rpcProcessingTime.pushMetric(metricsRecord);

    synchronized (registry) {
      // Iterate through the registry to propagate the different rpc metrics.

      for (String metricName : registry.getKeyList() ) {
        MetricsTimeVaryingRate value = (MetricsTimeVaryingRate) registry.get(metricName);

        value.pushMetric(metricsRecord);
      }
    }
    metricsRecord.update();
  }

  public void shutdown() {
    if (rpcStatistics != null)
      rpcStatistics.shutdown();
  }
}
