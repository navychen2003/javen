package org.javenstudio.raptor;

import java.lang.annotation.*;

/**
 * A package attribute that captures the version of Hawk that was compiled.
 * @author Owen O'Malley
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface VersionAnnotation {
 
  /**
   * Get the Raptor version
   * @return the version string "0.6.3-dev"
   */
  String version();
  
  /**
   * Get the username that compiled Hawk.
   */
  String user();
  
  /**
   * Get the date when Hawk was compiled.
   * @return the date in unix 'date' format
   */
  String date();
  
  /**
   * Get the url for the subversion repository.
   */
  String url();
  
  /**
   * Get the subversion revision.
   * @return the revision number as a string (eg. "451451")
   */
  String revision();

  /**
   * Get the javac version.
   * @return the version number as a string (eg. "1.6.0_14")
   */
  String javacversion();
}
