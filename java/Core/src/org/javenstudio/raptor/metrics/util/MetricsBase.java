package org.javenstudio.raptor.metrics.util;

import org.javenstudio.raptor.metrics.MetricsRecord;


/**
 * 
 * This is base class for all metrics
 *
 */
public abstract class MetricsBase {
  public static final String NO_DESCRIPTION = "NoDescription";
  final private String name;
  final private String description;
  
  protected MetricsBase(final String nam) {
    name = nam;
    description = NO_DESCRIPTION;
  }
  
  protected MetricsBase(final String nam, final String desc) {
    name = nam;
    description = desc;
  }
  
  public abstract void pushMetric(final MetricsRecord mr);
  
  public String getName() { return name; }
  public String getDescription() { return description; };

}
