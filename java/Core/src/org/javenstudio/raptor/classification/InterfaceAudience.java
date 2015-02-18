package org.javenstudio.raptor.classification;

import java.lang.annotation.Documented;

/**
 * Annotation to inform users of a package, class or method's intended audience.
 */
@InterfaceAudience.Public
@InterfaceStability.Evolving
public class InterfaceAudience {
  /**
   * Intended for use by any project or application.
   */
  @Documented public @interface Public {};
  
  /**
   * Intended only for the project(s) specified in the annotation.
   * For example, "Common", "HDFS", "MapReduce", "ZooKeeper", "HBase".
   */
  @Documented public @interface LimitedPrivate {
    String[] value();
  };
  
  /**
   * Intended for use only within Hadoop itself.
   */
  @Documented public @interface Private {};

  private InterfaceAudience() {} // Audience can't exist on its own
}

