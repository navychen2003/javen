package org.javenstudio.raptor.bigdb.util;

/**
 * Has some basic interaction with the environment. Alternate implementations
 * can be used where required (eg in tests).
 *
 * @see EnvironmentEdgeManager
 */
public interface EnvironmentEdge {

  /**
   * Returns the currentTimeMillis.
   *
   * @return currentTimeMillis.
   */
  long currentTimeMillis();
}

