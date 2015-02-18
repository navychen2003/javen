package org.javenstudio.raptor.bigdb.regionserver;

import org.javenstudio.common.util.Logger;
import org.javenstudio.raptor.bigdb.regionserver.ScanQueryMatcher.MatchCode;
import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * Keeps track of the columns for a scan if they are not explicitly specified
 */
public class ScanWildcardColumnTracker implements ColumnTracker {
  private static final Logger LOG =
		  Logger.getLogger(ScanWildcardColumnTracker.class);
  private byte [] columnBuffer = null;
  private int columnOffset = 0;
  private int columnLength = 0;
  private int currentCount = 0;
  private int maxVersions;

  /**
   * Return maxVersions of every row.
   * @param maxVersion
   */
  public ScanWildcardColumnTracker(int maxVersion) {
    this.maxVersions = maxVersion;
  }

  /**
   * Can only return INCLUDE or SKIP, since returning "NEXT" or
   * "DONE" would imply we have finished with this row, when
   * this class can't figure that out.
   *
   * @param bytes
   * @param offset
   * @param length
   * @return The match code instance.
   */
  @Override
  public MatchCode checkColumn(byte[] bytes, int offset, int length) {
    if (columnBuffer == null) {
      // first iteration.
      columnBuffer = bytes;
      columnOffset = offset;
      columnLength = length;
      currentCount = 0;

      if (++currentCount > maxVersions)
        return ScanQueryMatcher.MatchCode.SKIP;
      return ScanQueryMatcher.MatchCode.INCLUDE;
    }
    int cmp = Bytes.compareTo(bytes, offset, length,
        columnBuffer, columnOffset, columnLength);
    if (cmp == 0) {
      if (++currentCount > maxVersions)
        return ScanQueryMatcher.MatchCode.SKIP; // skip to next col
      return ScanQueryMatcher.MatchCode.INCLUDE;
    }

    // new col > old col
    if (cmp > 0) {
      // switched columns, lets do something.x
      columnBuffer = bytes;
      columnOffset = offset;
      columnLength = length;
      currentCount = 0;
      if (++currentCount > maxVersions)
        return ScanQueryMatcher.MatchCode.SKIP;
      return ScanQueryMatcher.MatchCode.INCLUDE;
    }

    // new col < oldcol
    // if (cmp < 0) {
    // WARNING: This means that very likely an edit for some other family
    // was incorrectly stored into the store for this one. Continue, but
    // complain.
    LOG.error("ScanWildcardColumnTracker.checkColumn ran " +
  		"into a column actually smaller than the previous column: " +
      Bytes.toStringBinary(bytes, offset, length));
    // switched columns
    columnBuffer = bytes;
    columnOffset = offset;
    columnLength = length;
    currentCount = 0;
    if (++currentCount > maxVersions)
      return ScanQueryMatcher.MatchCode.SKIP;
    return ScanQueryMatcher.MatchCode.INCLUDE;
  }

  @Override
  public void update() {
    // no-op, shouldn't even be called
    throw new UnsupportedOperationException(
        "ScanWildcardColumnTracker.update should never be called!");
  }

  @Override
  public void reset() {
    columnBuffer = null;
  }

  /**
   * Used by matcher and scan/get to get a hint of the next column
   * to seek to after checkColumn() returns SKIP.  Returns the next interesting
   * column we want, or NULL there is none (wildcard scanner).
   *
   * @return The column count.
   */
  public ColumnCount getColumnHint() {
    return null;
  }


  /**
   * We can never know a-priori if we are done, so always return false.
   * @return false
   */
  @Override
  public boolean done() {
    return false;
  }
}

