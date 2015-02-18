package org.javenstudio.falcon.datum.table.store;

import org.javenstudio.common.util.Logger;

/**
 * Keeps track of the columns for a scan if they are not explicitly specified
 */
public class ScanWildcardColumnTracker implements ColumnTracker {
  private static final Logger LOG = Logger.getLogger(ScanWildcardColumnTracker.class);
  
  private byte[] mColumnBuffer = null;
  private int mColumnOffset = 0;
  private int mColumnLength = 0;
  private int mCurrentCount = 0;
  private int mMaxVersions;

  /**
   * Return maxVersions of every row.
   * @param maxVersion
   */
  public ScanWildcardColumnTracker(int maxVersion) {
    this.mMaxVersions = maxVersion;
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
  public ScanQueryMatcher.MatchCode checkColumn(byte[] bytes, 
		  int offset, int length) {
    if (mColumnBuffer == null) {
      // first iteration.
      mColumnBuffer = bytes;
      mColumnOffset = offset;
      mColumnLength = length;
      mCurrentCount = 0;

      if (++mCurrentCount > mMaxVersions)
        return ScanQueryMatcher.MatchCode.SKIP;
      
      return ScanQueryMatcher.MatchCode.INCLUDE;
    }
    
    int cmp = Bytes.compareTo(bytes, offset, length,
        mColumnBuffer, mColumnOffset, mColumnLength);
    
    if (cmp == 0) {
      if (++mCurrentCount > mMaxVersions)
        return ScanQueryMatcher.MatchCode.SKIP; // skip to next col
      
      return ScanQueryMatcher.MatchCode.INCLUDE;
    }

    // new col > old col
    if (cmp > 0) {
      // switched columns, lets do something.x
      mColumnBuffer = bytes;
      mColumnOffset = offset;
      mColumnLength = length;
      mCurrentCount = 0;
      
      if (++mCurrentCount > mMaxVersions)
        return ScanQueryMatcher.MatchCode.SKIP;
      
      return ScanQueryMatcher.MatchCode.INCLUDE;
    }

    // new col < oldcol
    // if (cmp < 0) {
    // WARNING: This means that very likely an edit for some other family
    // was incorrectly stored into the store for this one. Continue, but
    // complain.
    if (LOG.isErrorEnabled()) {
      LOG.error("ScanWildcardColumnTracker.checkColumn ran " +
  		"into a column actually smaller than the previous column: " +
        Bytes.toStringBinary(bytes, offset, length));
    }
    
    // switched columns
    mColumnBuffer = bytes;
    mColumnOffset = offset;
    mColumnLength = length;
    mCurrentCount = 0;
    
    if (++mCurrentCount > mMaxVersions)
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
    mColumnBuffer = null;
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
