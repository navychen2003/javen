package org.javenstudio.raptor.classification;

import java.lang.annotation.Documented;

/**
 * Annotation to inform users of how much to rely on a particular package,
 * class or method not changing over time.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class InterfaceStability {
  /**
   * Can evolve while retaining compatibility for minor release boundaries.; 
   * can break compatibility only at major release (ie. at m.0).
   */
  @Documented
  public @interface Stable {};
  
  /**
   * Evolving, but can break compatibility at minor release (i.e. m.x)
   */
  @Documented
  public @interface Evolving {};
  
  /**
   * No guarantee is provided as to reliability or stability across any
   * level of release granularity.
   */
  @Documented
  public @interface Unstable {};
}

