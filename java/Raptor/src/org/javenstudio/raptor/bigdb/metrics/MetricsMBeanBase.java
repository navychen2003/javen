package org.javenstudio.raptor.bigdb.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.metrics.util.MetricsBase;
import org.javenstudio.raptor.metrics.util.MetricsDynamicMBeanBase;
import org.javenstudio.raptor.metrics.util.MetricsRegistry;

/**
 * Extends the Hadoop MetricsDynamicMBeanBase class to provide JMX support for
 * custom BigDB MetricsBase implementations.  MetricsDynamicMBeanBase ignores
 * registered MetricsBase instance that are not instances of one of the
 * org.javenstudio.raptor.metrics.util implementations.
 *
 */
public class MetricsMBeanBase extends MetricsDynamicMBeanBase {

  private static final Logger LOG = Logger.getLogger(MetricsMBeanBase.class);

  protected final MetricsRegistry registry;
  protected final String description;
  protected int registryLength;
  /** BigDB MetricsBase implementations that MetricsDynamicMBeanBase does
   * not understand
   */
  protected Map<String,MetricsBase> extendedAttributes =
      new HashMap<String,MetricsBase>();
  protected MBeanInfo extendedInfo;

  protected MetricsMBeanBase( MetricsRegistry mr, String description ) {
    super(copyMinusDBMetrics(mr), description);
    this.registry = mr;
    this.description = description;
    this.init();
  }

  /*
   * @param mr MetricsRegistry.
   * @return A copy of the passed MetricsRegistry minus the bigdb metrics
   */
  private static MetricsRegistry copyMinusDBMetrics(final MetricsRegistry mr) {
    MetricsRegistry copy = new MetricsRegistry();
    for (MetricsBase metric : mr.getMetricsList()) {
      if (metric instanceof org.javenstudio.raptor.bigdb.metrics.MetricsRate) {
        continue;
      }
      copy.add(metric.getName(), metric);
    }
    return copy;
  }

  protected void init() {
    List<MBeanAttributeInfo> attributes = new ArrayList<MBeanAttributeInfo>();
    MBeanInfo parentInfo = super.getMBeanInfo();
    List<String> parentAttributes = new ArrayList<String>();
    for (MBeanAttributeInfo attr : parentInfo.getAttributes()) {
      attributes.add(attr);
      parentAttributes.add(attr.getName());
    }

    this.registryLength = this.registry.getMetricsList().size();

    for (MetricsBase metric : this.registry.getMetricsList()) {
      if (metric.getName() == null || parentAttributes.contains(metric.getName()))
        continue;

      // add on custom BigDB metric types
      if (metric instanceof org.javenstudio.raptor.bigdb.metrics.MetricsRate) {
        attributes.add( new MBeanAttributeInfo(metric.getName(),
            "java.lang.Float", metric.getDescription(), true, false, false) );
        extendedAttributes.put(metric.getName(), metric);
      }
      // else, its probably a hadoop metric already registered. Skip it.
    }

    this.extendedInfo = new MBeanInfo( this.getClass().getName(),
        this.description, attributes.toArray( new MBeanAttributeInfo[0] ),
        parentInfo.getConstructors(), parentInfo.getOperations(),
        parentInfo.getNotifications() );
  }

  private void checkAndUpdateAttributes() {
    if (this.registryLength != this.registry.getMetricsList().size())
      this.init();
  }

  @Override
  public Object getAttribute( String name )
      throws AttributeNotFoundException, MBeanException,
      ReflectionException {

    if (name == null) {
      throw new IllegalArgumentException("Attribute name is NULL");
    }

    /*
     * Ugly.  Since MetricsDynamicMBeanBase implementation is private,
     * we need to first check the parent class for the attribute.
     * In case that the MetricsRegistry contents have changed, this will
     * allow the parent to update it's internal structures (which we rely on
     * to update our own.
     */
    try {
      return super.getAttribute(name);
    } catch (AttributeNotFoundException ex) {

      checkAndUpdateAttributes();

      MetricsBase metric = this.extendedAttributes.get(name);
      if (metric != null) {
        if (metric instanceof MetricsRate) {
          return ((MetricsRate) metric).getPreviousIntervalValue();
        } else {
          LOG.warn( String.format("unknown metrics type %s for attribute %s",
                        metric.getClass().getName(), name) );
        }
      }
    }

    throw new AttributeNotFoundException();
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    return this.extendedInfo;
  }

}

