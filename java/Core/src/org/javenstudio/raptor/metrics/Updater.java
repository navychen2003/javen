package org.javenstudio.raptor.metrics;


/**
 * Call-back interface.  See <code>MetricsContext.registerUpdater()</code>.
 */
public interface Updater {
    
  /**
   * Timer-based call-back from the metric library. 
   */
  public abstract void doUpdates(MetricsContext context);

}
