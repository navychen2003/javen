package org.javenstudio.raptor.metrics.util;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.metrics.MetricsRecord;
import org.javenstudio.raptor.util.StringUtils;


/**
 * The MetricsTimeVaryingLong class is for a metric that naturally
 * varies over time (e.g. number of files created). The metrics is accumulated
 * over an interval (set in the metrics config file); the metrics is
 *  published at the end of each interval and then 
 * reset to zero. Hence the counter has the value in the current interval. 
 * 
 * Note if one wants a time associated with the metric then use
 * @see org.javenstudio.raptor.metrics.util.MetricsTimeVaryingRate
 *
 */
public class MetricsTimeVaryingLong extends MetricsBase{
  private static final Logger LOG = Logger.getLogger(MetricsTimeVaryingLong.class);
 
  private long currentValue;
  private long previousIntervalValue;
  
  /**
   * Constructor - create a new metric
   * @param nam the name of the metrics to be used to publish the metric
   * @param registry - where the metrics object will be registered
   */
  public MetricsTimeVaryingLong(final String nam, MetricsRegistry registry, final String description) {
    super(nam, description);
    currentValue = 0;
    previousIntervalValue = 0;
    registry.add(nam, this);
  }
  
  
  /**
   * Constructor - create a new metric
   * @param nam the name of the metrics to be used to publish the metric
   * @param registry - where the metrics object will be registered
   * A description of {@link #NO_DESCRIPTION} is used
   */
  public MetricsTimeVaryingLong(final String nam, MetricsRegistry registry) {
    this(nam, registry, NO_DESCRIPTION);
  }
  
  /**
   * Inc metrics for incr vlaue
   * @param incr - number of operations
   */
  public synchronized void inc(final long incr) {
    currentValue += incr;
  }
  
  /**
   * Inc metrics by one
   */
  public synchronized void inc() {
    currentValue++;
  }

  private synchronized void intervalHeartBeat() {
     previousIntervalValue = currentValue;
     currentValue = 0;
  }
  
  /**
   * Push the delta  metrics to the mr.
   * The delta is since the last push/interval.
   * 
   * Note this does NOT push to JMX
   * (JMX gets the info via {@link #previousIntervalValue}
   *
   * @param mr
   */
  public synchronized void pushMetric(final MetricsRecord mr) {
    intervalHeartBeat();
    try {
      mr.incrMetric(getName(), getPreviousIntervalValue());
    } catch (Exception e) {
      LOG.info("pushMetric failed for " + getName() + "\n" +
          StringUtils.stringifyException(e));
    }
  }
  
  
  /**
   * The Value at the Previous interval
   * @return prev interval value
   */
  public synchronized long getPreviousIntervalValue() { 
    return previousIntervalValue;
  } 
  
  /**
   * The Value at the current interval
   * @return prev interval value
   */
  public synchronized long getCurrentIntervalValue() { 
    return currentValue;
  } 
}
