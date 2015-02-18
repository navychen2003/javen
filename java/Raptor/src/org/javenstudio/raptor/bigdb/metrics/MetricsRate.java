package org.javenstudio.raptor.bigdb.metrics;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.metrics.MetricsRecord;
import org.javenstudio.raptor.metrics.util.MetricsBase;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;
import org.javenstudio.raptor.util.StringUtils;

/**
 * Publishes a rate based on a counter - you increment the counter each
 * time an event occurs (eg: an RPC call) and this publishes a rate.
 */
public class MetricsRate extends MetricsBase {
  private static final Logger LOG = Logger.getLogger(MetricsRate.class);

  private int value;
  private float prevRate;
  private long ts;

  public MetricsRate(final String name, final MetricsRegistry registry,
      final String description) {
    super(name, description);
    this.value = 0;
    this.prevRate = 0;
    this.ts = System.currentTimeMillis();
    registry.add(name, this);
  }

  public MetricsRate(final String name, final MetricsRegistry registry) {
    this(name, registry, NO_DESCRIPTION);
  }

  public synchronized void inc(final int incr) {
    value += incr;
  }

  public synchronized void inc() {
    value++;
  }

  private synchronized void intervalHeartBeat() {
    long now = System.currentTimeMillis();
    long diff = (now-ts)/1000;
    if (diff == 0) diff = 1; // sigh this is crap.
    this.prevRate = (float)value / diff;
    this.value = 0;
    this.ts = now;
  }

  @Override
  public synchronized void pushMetric(final MetricsRecord mr) {
    intervalHeartBeat();
    try {
      mr.setMetric(getName(), getPreviousIntervalValue());
    } catch (Exception e) {
      LOG.info("pushMetric failed for " + getName() + "\n" +
          StringUtils.stringifyException(e));
    }
  }

  public synchronized float getPreviousIntervalValue() {
    return this.prevRate;
  }
}

