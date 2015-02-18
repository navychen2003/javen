package org.javenstudio.raptor.metrics.util;

import org.javenstudio.raptor.metrics.MetricsRecord;


/**
 * The MetricsLongValue class is for a metric that is not time varied
 * but changes only when it is set. 
 * Each time its value is set, it is published only *once* at the next update
 * call.
 *
 */
public class MetricsLongValue extends MetricsBase{  
  private long value;
  private boolean changed;
  
  /**
   * Constructor - create a new metric
   * @param nam the name of the metrics to be used to publish the metric
   * @param registry - where the metrics object will be registered
   */
  public MetricsLongValue(final String nam, final MetricsRegistry registry, final String description) {
    super(nam, description);
    value = 0;
    changed = false;
    registry.add(nam, this);
  }
  
  /**
   * Constructor - create a new metric
   * @param nam the name of the metrics to be used to publish the metric
   * @param registry - where the metrics object will be registered
   * A description of {@link #NO_DESCRIPTION} is used
   */
  public MetricsLongValue(final String nam, MetricsRegistry registry) {
    this(nam, registry, NO_DESCRIPTION);
  }
  
  /**
   * Set the value
   * @param newValue
   */
  public synchronized void set(final long newValue) {
    value = newValue;
    changed = true;
  }
  
  /**
   * Get value
   * @return the value last set
   */
  public synchronized long get() { 
    return value;
  } 
 

  /**
   * Push the metric to the mr.
   * The metric is pushed only if it was updated since last push
   * 
   * Note this does NOT push to JMX
   * (JMX gets the info via {@link #get()}
   *
   * @param mr
   */
  public synchronized void pushMetric(final MetricsRecord mr) {
    if (changed) 
      mr.setMetric(getName(), value);
    changed = false;
  }
}
