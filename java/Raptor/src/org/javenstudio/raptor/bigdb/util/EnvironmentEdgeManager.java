package org.javenstudio.raptor.bigdb.util;

/**
 * Manages a singleton instance of the environment edge. This class shall
 * implement static versions of the interface {@link EnvironmentEdge}, then
 * defer to the delegate on invocation.
 */
public class EnvironmentEdgeManager {
  private static volatile EnvironmentEdge delegate = new DefaultEnvironmentEdge();

  private EnvironmentEdgeManager() {

  }

  /**
   * Retrieves the singleton instance of the {@link EnvironmentEdge} that is
   * being managed.
   *
   * @return the edge.
   */
  public static EnvironmentEdge getDelegate() {
    return delegate;
  }

  /**
   * Resets the managed instance to the default instance: {@link
   * DefaultEnvironmentEdge}.
   */
  static void reset() {
    injectEdge(new DefaultEnvironmentEdge());
  }

  /**
   * Injects the given edge such that it becomes the managed entity. If null is
   * passed to this method, the default type is assigned to the delegate.
   *
   * @param edge the new edge.
   */
  static void injectEdge(EnvironmentEdge edge) {
    if (edge == null) {
      reset();
    } else {
      delegate = edge;
    }
  }

  /**
   * Defers to the delegate and calls the
   * {@link EnvironmentEdge#currentTimeMillis()} method.
   *
   * @return current time in millis according to the delegate.
   */
  public static long currentTimeMillis() {
    return getDelegate().currentTimeMillis();
  }
}

