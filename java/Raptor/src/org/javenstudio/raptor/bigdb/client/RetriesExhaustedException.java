package org.javenstudio.raptor.bigdb.client;

import org.javenstudio.raptor.bigdb.util.Bytes;

import java.io.IOException;
import java.util.List;

/**
 * Exception thrown by HTable methods when an attempt to do something (like
 * commit changes) fails after a bunch of retries.
 */
public class RetriesExhaustedException extends IOException {
  private static final long serialVersionUID = 1876775844L;

  public RetriesExhaustedException(final String msg) {
    super(msg);
  }

  /**
   * Create a new RetriesExhaustedException from the list of prior failures.
   * @param serverName name of HRegionServer
   * @param regionName name of region
   * @param row The row we were pursuing when we ran out of retries
   * @param numTries The number of tries we made
   * @param exceptions List of exceptions that failed before giving up
   */
  public RetriesExhaustedException(String serverName, final byte [] regionName,
      final byte []  row, int numTries, List<Throwable> exceptions) {
    super(getMessage(serverName, regionName, row, numTries, exceptions));
  }

  private static String getMessage(String serverName, final byte [] regionName,
      final byte [] row,
      int numTries, List<Throwable> exceptions) {
    StringBuilder buffer = new StringBuilder("Trying to contact region server ");
    buffer.append(serverName);
    buffer.append(" for region ");
    buffer.append(regionName == null? "": Bytes.toStringBinary(regionName));
    buffer.append(", row '");
    buffer.append(row == null? "": Bytes.toStringBinary(row));
    buffer.append("', but failed after ");
    buffer.append(numTries + 1);
    buffer.append(" attempts.\nExceptions:\n");
    for (Throwable t : exceptions) {
      buffer.append(t.toString());
      buffer.append("\n");
    }
    return buffer.toString();
  }
}
