package org.javenstudio.raptor.metrics;


/**
 * General-purpose, unchecked metrics exception.
 */
public class MetricsException extends RuntimeException {
    
  private static final long serialVersionUID = -1643257498540498497L;

  /** Creates a new instance of MetricsException */
  public MetricsException() {
  }
    
  /** Creates a new instance of MetricsException 
   *
   * @param message an error message
   */
  public MetricsException(String message) {
    super(message);
  }
    
}
