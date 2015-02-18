package org.javenstudio.raptor.metrics.spi;

import java.util.Collections;
import java.util.Set;

import org.javenstudio.raptor.metrics.spi.AbstractMetricsContext.MetricMap;
import org.javenstudio.raptor.metrics.spi.AbstractMetricsContext.TagMap;


/**
 * Represents a record of metric data to be sent to a metrics system.
 */
public class OutputRecord {
    
  private TagMap tagMap;
  private MetricMap metricMap;
    
  /** Creates a new instance of OutputRecord */
  OutputRecord(TagMap tagMap, MetricMap metricMap) {
    this.tagMap = tagMap;
    this.metricMap = metricMap;
  }
    
  /**
   * Returns the set of tag names
   */
  public Set<String> getTagNames() {
    return Collections.unmodifiableSet(tagMap.keySet());
  }
    
  /**
   * Returns a tag object which is can be a String, Integer, Short or Byte.
   *
   * @return the tag value, or null if there is no such tag
   */
  public Object getTag(String name) {
    return tagMap.get(name);
  }
    
  /**
   * Returns the set of metric names.
   */
  public Set<String> getMetricNames() {
    return Collections.unmodifiableSet(metricMap.keySet());
  }
    
  /**
   * Returns the metric object which can be a Float, Integer, Short or Byte.
   */
  public Number getMetric(String name) {
    return metricMap.get(name);
  }

}
