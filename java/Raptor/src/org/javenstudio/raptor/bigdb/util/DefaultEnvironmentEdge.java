package org.javenstudio.raptor.bigdb.util;

/**
 * Default implementation of an environment edge.
 */
public class DefaultEnvironmentEdge implements EnvironmentEdge {


  /**
   * {@inheritDoc}
   * <p/>
   * This implementation returns {@link System#currentTimeMillis()}
   */
  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}

