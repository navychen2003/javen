package org.javenstudio.common.indexdb.util;

/**
 * Math static utility methods.
 */
public final class MathUtil {
  private MathUtil() {} // No instance:

  /**
   * Returns {@code x <= 0 ? 0 : Math.floor(Math.log(x) / Math.log(base))}
   * @param base must be {@code > 1}
   */
  public static int log(long x, int base) {
    if (base <= 1) {
      throw new IllegalArgumentException("base must be > 1");
    }
    int ret = 0;
    while (x >= base) {
      x /= base;
      ret++;
    }
    return ret;
  }
  
}
