package org.javenstudio.raptor.metrics.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * This is the registry for metrics.
 * Related set of metrics should be declared in a holding class and registered
 * in a registry for those metrics which is also stored in the the holding class.
 *
 */
public class MetricsRegistry {
  private Map<String, MetricsBase> metricsList = new HashMap<String, MetricsBase>();

  public MetricsRegistry() {
  }
  
  /**
   * 
   * @return number of metrics in the registry
   */
  public int size() {
    return metricsList.size();
  }
  
  /**
   * Add a new metrics to the registry
   * @param metricsName - the name
   * @param theMetricsObj - the metrics
   * @throws IllegalArgumentException if a name is already registered
   */
  public synchronized void add(final String metricsName, final MetricsBase theMetricsObj) {
    if (metricsList.containsKey(metricsName)) {
      throw new IllegalArgumentException("Duplicate metricsName:" + metricsName);
    }
    metricsList.put(metricsName, theMetricsObj);
  }

  
  /**
   * 
   * @param metricsName
   * @return the metrics if there is one registered by the supplied name.
   *         Returns null if none is registered
   */
  public synchronized MetricsBase get(final String metricsName) {
    return metricsList.get(metricsName);
  }
  
  
  /**
   * 
   * @return the list of metrics names
   */
  public synchronized Collection<String> getKeyList() {
    return metricsList.keySet();
  }
  
  /**
   * 
   * @return the list of metrics
   */
  public synchronized Collection<MetricsBase> getMetricsList() {
    return metricsList.values();
  }
}
