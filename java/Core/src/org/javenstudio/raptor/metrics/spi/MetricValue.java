package org.javenstudio.raptor.metrics.spi;


/**
 * A Number that is either an absolute or an incremental amount.
 */
public class MetricValue {
    
  public static final boolean ABSOLUTE = false;
  public static final boolean INCREMENT = true;
    
  private boolean isIncrement;
  private Number number;
    
  /** Creates a new instance of MetricValue */
  public MetricValue(Number number, boolean isIncrement) {
    this.number = number;
    this.isIncrement = isIncrement;
  }

  public boolean isIncrement() {
    return isIncrement;
  }
    
  public boolean isAbsolute() {
    return !isIncrement;
  }

  public Number getNumber() {
    return number;
  }
}
