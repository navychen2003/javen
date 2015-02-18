package org.javenstudio.raptor.metrics.spi;


/**
 * Null metrics context: a metrics context which does nothing.  Used as the
 * default context, so that no performance data is emitted if no configuration
 * data is found.
 * 
 */
public class NullContext extends AbstractMetricsContext {
    
  /** Creates a new instance of NullContext */
  public NullContext() {
  }
    
  /**
   * Do-nothing version of startMonitoring
   */
  public void startMonitoring() {
  }
    
  /**
   * Do-nothing version of emitRecord
   */
  protected void emitRecord(String contextName, String recordName,
                            OutputRecord outRec) 
  {}
    
  /**
   * Do-nothing version of update
   */
  protected void update(MetricsRecordImpl record) {
  }
    
  /**
   * Do-nothing version of remove
   */
  protected void remove(MetricsRecordImpl record) {
  }
}
