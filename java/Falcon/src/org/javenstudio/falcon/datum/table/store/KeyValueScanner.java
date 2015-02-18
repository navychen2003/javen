package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;

/**
 * Scanner that returns the next KeyValue.
 */
public interface KeyValueScanner {
  /**
   * Look at the next KeyValue in this scanner, but do not iterate scanner.
   * @return the next KeyValue
   */
  public KeyValue peek();

  /**
   * Return the next KeyValue in this scanner, iterating the scanner
   * @return the next KeyValue
   */
  public KeyValue next() throws IOException;

  /**
   * Seek the scanner at or after the specified KeyValue.
   * @param key seek value
   * @return true if scanner has values left, false if end of scanner
   */
  public boolean seek(KeyValue key) throws IOException;

  /**
   * Reseek the scanner at or after the specified KeyValue.
   * This method is guaranteed to seek to or before the required key only if the
   * key comes after the current position of the scanner. Should not be used
   * to seek to a key which may come before the current position.
   * @param key seek value (should be non-null)
   * @return true if scanner has values left, false if end of scanner
   */
  public boolean reseek(KeyValue key) throws IOException;

  /**
   * Close the KeyValue scanner.
   */
  public void close();
}
