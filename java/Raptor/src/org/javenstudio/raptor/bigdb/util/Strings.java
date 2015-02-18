package org.javenstudio.raptor.bigdb.util;

/**
 * Utility for Strings.
 */
public class Strings {
  public final static String DEFAULT_SEPARATOR = "=";
  public final static String DEFAULT_KEYVALUE_SEPARATOR = ", ";

  /**
   * Append to a StringBuilder a key/value.
   * Uses default separators.
   * @param sb StringBuilder to use
   * @param key Key to append.
   * @param value Value to append.
   * @return Passed <code>sb</code> populated with key/value.
   */
  public static StringBuilder appendKeyValue(final StringBuilder sb,
      final String key, final Object value) {
    return appendKeyValue(sb, key, value, DEFAULT_SEPARATOR,
      DEFAULT_KEYVALUE_SEPARATOR);
  }

  /**
   * Append to a StringBuilder a key/value.
   * Uses default separators.
   * @param sb StringBuilder to use
   * @param key Key to append.
   * @param value Value to append.
   * @param separator Value to use between key and value.
   * @param keyValueSeparator Value to use between key/value sets.
   * @return Passed <code>sb</code> populated with key/value.
   */
  public static StringBuilder appendKeyValue(final StringBuilder sb,
      final String key, final Object value, final String separator,
      final String keyValueSeparator) {
    if (sb.length() > 0) {
      sb.append(keyValueSeparator);
    }
    return sb.append(key).append(separator).append(value);
  }
}
