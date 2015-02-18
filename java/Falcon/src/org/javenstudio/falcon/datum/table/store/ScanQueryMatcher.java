package org.javenstudio.falcon.datum.table.store;

import java.util.NavigableSet;

/**
 * A query matcher that is specifically designed for the scan case.
 */
public class ScanQueryMatcher {

  // Optimization so we can skip lots of compares when we decide to skip
  // to the next row.
  private boolean mStickyNextRow;
  private byte[] mStopRow;

  protected TimeRange mTr;
  protected Filter mFilter;

  /** Keeps track of deletes */
  protected DeleteTracker mDeletes;

  /** Keeps track of columns and versions */
  protected ColumnTracker mColumns;

  /** Key to seek to in memstore and StoreFiles */
  protected KeyValue mStartKey;

  /** Oldest allowed version stamp for TTL enforcement */
  protected long mOldestStamp;

  /** Row comparator for the region this query is for */
  protected KeyValue.KeyComparator mRowComparator;

  /** Row the query is on */
  protected byte[] mRow;

  /**
   * Constructs a ScanQueryMatcher for a Scan.
   * @param scan
   * @param family
   * @param columns
   * @param ttl
   * @param rowComparator
   */
  public ScanQueryMatcher(Scan scan, byte[] family,
      NavigableSet<byte[]> columns, long ttl,
      KeyValue.KeyComparator rowComparator, int maxVersions) {
    this.mTr = scan.getTimeRange();
    this.mOldestStamp = System.currentTimeMillis() - ttl;
    this.mRowComparator = rowComparator;
    this.mDeletes =  new ScanDeleteTracker();
    this.mStopRow = scan.getStopRow();
    this.mStartKey = KeyValue.createFirstOnRow(scan.getStartRow());
    this.mFilter = scan.getFilter();

    // Single branch to deal with two types of reads (columns vs all in family)
    if (columns == null || columns.size() == 0) {
      // use a specialized scan for wildcard column tracker.
      this.mColumns = new ScanWildcardColumnTracker(maxVersions);
    } else {
      // We can share the ExplicitColumnTracker, diff is we reset
      // between rows, not between storefiles.
      this.mColumns = new ExplicitColumnTracker(columns,maxVersions);
    }
  }

  /**
   * Determines if the caller should do one of several things:
   * - seek/skip to the next row (MatchCode.SEEK_NEXT_ROW)
   * - seek/skip to the next column (MatchCode.SEEK_NEXT_COL)
   * - include the current KeyValue (MatchCode.INCLUDE)
   * - ignore the current KeyValue (MatchCode.SKIP)
   * - got to the next row (MatchCode.DONE)
   *
   * @param kv KeyValue to check
   * @return The match code instance.
   */
  public MatchCode match(KeyValue kv) {
    if (mFilter != null && mFilter.filterAllRemaining()) 
      return MatchCode.DONE_SCAN;

    byte[] bytes = kv.getBuffer();
    int offset = kv.getOffset();
    int initialOffset = offset;

    int keyLength = Bytes.toInt(bytes, offset, Bytes.SIZEOF_INT);
    offset += KeyValue.ROW_OFFSET;

    short rowLength = Bytes.toShort(bytes, offset, Bytes.SIZEOF_SHORT);
    offset += Bytes.SIZEOF_SHORT;

    int ret = this.mRowComparator.compareRows(mRow, 0, mRow.length,
        bytes, offset, rowLength);
    
    if (ret <= -1) {
      return MatchCode.DONE;
      
    } else if (ret >= 1) {
      // could optimize this, if necessary?
      // Could also be called SEEK_TO_CURRENT_ROW, but this
      // should be rare/never happens.
      return MatchCode.SEEK_NEXT_ROW;
    }

    // optimize case.
    if (this.mStickyNextRow)
        return MatchCode.SEEK_NEXT_ROW;

    if (this.mColumns.done()) {
      mStickyNextRow = true;
      return MatchCode.SEEK_NEXT_ROW;
    }

    //Passing rowLength
    offset += rowLength;

    //Skipping family
    byte familyLength = bytes [offset];
    offset += familyLength + 1;

    int qualLength = keyLength + KeyValue.ROW_OFFSET -
      (offset - initialOffset) - KeyValue.TIMESTAMP_TYPE_SIZE;

    long timestamp = kv.getTimestamp();
    if (isExpired(timestamp)) {
      // done, the rest of this column will also be expired as well.
      return getNextRowOrNextColumn(bytes, offset, qualLength);
    }

    byte type = kv.getType();
    if (isDelete(type)) {
      if (mTr.withinOrAfterTimeRange(timestamp)) {
        this.mDeletes.add(bytes, offset, qualLength, timestamp, type);
        // Can't early out now, because DelFam come before any other keys
      }
      
      // May be able to optimize the SKIP here, if we matched
      // due to a DelFam, we can skip to next row
      // due to a DelCol, we can skip to next col
      // But it requires more info out of isDelete().
      // needful -> million column challenge.
      return MatchCode.SKIP;
    }

    if (!this.mDeletes.isEmpty() &&
        mDeletes.isDeleted(bytes, offset, qualLength, timestamp)) {
      return MatchCode.SKIP;
    }

    int timestampComparison = mTr.compare(timestamp);
    if (timestampComparison >= 1) {
      return MatchCode.SKIP;
    } else if (timestampComparison <= -1) {
      return getNextRowOrNextColumn(bytes, offset, qualLength);
    }

    /**
     * Filters should be checked before checking column trackers. If we do
     * otherwise, as was previously being done, ColumnTracker may increment its
     * counter for even that KV which may be discarded later on by Filter. This
     * would lead to incorrect results in certain cases.
     */
    if (mFilter != null) {
      Filter.ReturnCode filterResponse = mFilter.filterKeyValue(kv);
      if (filterResponse == Filter.ReturnCode.SKIP) {
        return MatchCode.SKIP;
        
      } else if (filterResponse == Filter.ReturnCode.NEXT_COL) {
        return getNextRowOrNextColumn(bytes, offset, qualLength);
        
      } else if (filterResponse == Filter.ReturnCode.NEXT_ROW) {
        mStickyNextRow = true;
        return MatchCode.SEEK_NEXT_ROW;
        
      } else if (filterResponse == Filter.ReturnCode.SEEK_NEXT_USING_HINT) {
        return MatchCode.SEEK_NEXT_USING_HINT;
      }
    }

    MatchCode colChecker = mColumns.checkColumn(bytes, offset, qualLength);
    
    // if SKIP -> SEEK_NEXT_COL
    // if (NEXT,DONE) -> SEEK_NEXT_ROW
    // if (INCLUDE) -> INCLUDE
    if (colChecker == MatchCode.SKIP) {
      return MatchCode.SEEK_NEXT_COL;
    } else if (colChecker == MatchCode.NEXT || colChecker == MatchCode.DONE) {
      mStickyNextRow = true;
      return MatchCode.SEEK_NEXT_ROW;
    }

    return MatchCode.INCLUDE;
  }

  public MatchCode getNextRowOrNextColumn(byte[] bytes, int offset,
      int qualLength) {
    if (mColumns instanceof ExplicitColumnTracker) {
      //We only come here when we know that columns is an instance of
      //ExplicitColumnTracker so we should never have a cast exception
      ((ExplicitColumnTracker)mColumns).doneWithColumn(bytes, offset, qualLength);
      
      if (mColumns.getColumnHint() == null) {
        return MatchCode.SEEK_NEXT_ROW;
      } else {
        return MatchCode.SEEK_NEXT_COL;
      }
    } else {
      return MatchCode.SEEK_NEXT_COL;
    }
  }

  public boolean moreRowsMayExistAfter(KeyValue kv) {
    if (!Bytes.equals(mStopRow , DBConstants.EMPTY_END_ROW) &&
        mRowComparator.compareRows(kv.getBuffer(),kv.getRowOffset(),
            kv.getRowLength(), mStopRow, 0, mStopRow.length) >= 0) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Set current row
   * @param row
   */
  public void setRow(byte[] row) {
    this.mRow = row;
    reset();
  }

  public void reset() {
    this.mDeletes.reset();
    this.mColumns.reset();

    this.mStickyNextRow = false;
  }

  // should be in KeyValue.
  protected boolean isDelete(byte type) {
    return (type != KeyValue.Type.Put.getCode());
  }

  protected boolean isExpired(long timestamp) {
    return (timestamp < mOldestStamp);
  }

  /**
   *
   * @return the start key
   */
  public KeyValue getStartKey() {
    return this.mStartKey;
  }

  public KeyValue getNextKeyHint(KeyValue kv) {
    if (mFilter == null) {
      return null;
    } else {
      return mFilter.getNextKeyHint(kv);
    }
  }

  /**
   * {@link #match} return codes.  These instruct the scanner moving through
   * memstores and StoreFiles what to do with the current KeyValue.
   * <p>
   * Additionally, this contains "early-out" language to tell the scanner to
   * move on to the next File (memstore or Storefile), or to return immediately.
   */
  public static enum MatchCode {
    /**
     * Include KeyValue in the returned result
     */
    INCLUDE,

    /**
     * Do not include KeyValue in the returned result
     */
    SKIP,

    /**
     * Do not include, jump to next StoreFile or memstore (in time order)
     */
    NEXT,

    /**
     * Do not include, return current result
     */
    DONE,

    /**
     * These codes are used by the ScanQueryMatcher
     */

    /**
     * Done with the row, seek there.
     */
    SEEK_NEXT_ROW,
    /**
     * Done with column, seek to next.
     */
    SEEK_NEXT_COL,

    /**
     * Done with scan, thanks to the row filter.
     */
    DONE_SCAN,

    /*
     * Seek to next key which is given as hint.
     */
    SEEK_NEXT_USING_HINT,
  }
}
