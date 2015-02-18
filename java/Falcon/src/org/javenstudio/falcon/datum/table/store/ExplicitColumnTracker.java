package org.javenstudio.falcon.datum.table.store;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

/**
 * This class is used for the tracking and enforcement of columns and numbers
 * of versions during the course of a Get or Scan operation, when explicit
 * column qualifiers have been asked for in the query.
 *
 * With a little magic (see {@link ScanQueryMatcher}), we can use this matcher
 * for both scans and gets.  The main difference is 'next' and 'done' collapse
 * for the scan case (since we see all columns in order), and we only reset
 * between rows.
 *
 * <p>
 * This class is utilized by {@link ScanQueryMatcher} through two methods:
 * <ul><li>{@link #checkColumn} is called when a Put satisfies all other
 * conditions of the query.  This method returns a {@link ScanQueryMatcher.MatchCode} to define
 * what action should be taken.
 * <li>{@link #update} is called at the end of every StoreFile or memstore.
 * <p>
 * This class is NOT thread-safe as queries are never multi-threaded
 */
public class ExplicitColumnTracker implements ColumnTracker {

  private final int mMaxVersions;
  private final List<ColumnCount> mColumns;
  private final List<ColumnCount> mColumnsToReuse;
  private int mIndex;
  private ColumnCount mColumn;

  /**
   * Default constructor.
   * @param columns columns specified user in query
   * @param maxVersions maximum versions to return per column
   */
  public ExplicitColumnTracker(NavigableSet<byte[]> columns, int maxVersions) {
    this.mMaxVersions = maxVersions;
    this.mColumns = new ArrayList<ColumnCount>(columns.size());
    this.mColumnsToReuse = new ArrayList<ColumnCount>(columns.size());
    for (byte [] column : columns) {
      this.mColumnsToReuse.add(new ColumnCount(column, maxVersions));
    }
    reset();
  }

  /**
   * Done when there are no more columns to match against.
   */
  @Override
  public boolean done() {
    return this.mColumns.size() == 0;
  }

  @Override
  public ColumnCount getColumnHint() {
    return this.mColumn;
  }

  /**
   * Checks against the parameters of the query and the columns which have
   * already been processed by this query.
   * @param bytes KeyValue buffer
   * @param offset offset to the start of the qualifier
   * @param length length of the qualifier
   * @return MatchCode telling ScanQueryMatcher what action to take
   */
  @Override
  public ScanQueryMatcher.MatchCode checkColumn(byte [] bytes, int offset, int length) {
    do {
      // No more columns left, we are done with this query
      if (this.mColumns.size() == 0) {
        return ScanQueryMatcher.MatchCode.DONE; // done_row
      }

      // No more columns to match against, done with storefile
      if (this.mColumn == null) {
        return ScanQueryMatcher.MatchCode.NEXT; // done_row
      }

      // Compare specific column to current column
      int ret = Bytes.compareTo(mColumn.getBuffer(), mColumn.getOffset(),
          mColumn.getLength(), bytes, offset, length);

      // Matches, decrement versions left and include
      if (ret == 0) {
        if (this.mColumn.decrement() == 0) {
          // Done with versions for this column
          this.mColumns.remove(this.mIndex);
          
          if (this.mColumns.size() == this.mIndex) {
            // Will not hit any more columns in this storefile
            this.mColumn = null;
          } else {
            this.mColumn = this.mColumns.get(this.mIndex);
          }
        }
        return ScanQueryMatcher.MatchCode.INCLUDE;
      }

      if (ret > 0) {
         // Specified column is smaller than the current, skip to next column.
        return ScanQueryMatcher.MatchCode.SKIP;
      }

      // Specified column is bigger than current column
      // Move down current column and check again
      if (ret <= -1) {
        if (++this.mIndex == this.mColumns.size()) {
          // No more to match, do not include, done with storefile
          return ScanQueryMatcher.MatchCode.NEXT; // done_row
        }
        // This is the recursive case.
        this.mColumn = this.mColumns.get(this.mIndex);
      }
    } while(true);
  }

  /**
   * Called at the end of every StoreFile or memstore.
   */
  @Override
  public void update() {
    if (this.mColumns.size() != 0) {
      this.mIndex = 0;
      this.mColumn = this.mColumns.get(this.mIndex);
    } else {
      this.mIndex = -1;
      this.mColumn = null;
    }
  }

  // Called between every row.
  @Override
  public void reset() {
    buildColumnList();
    this.mIndex = 0;
    this.mColumn = this.mColumns.get(this.mIndex);
  }

  private void buildColumnList() {
    this.mColumns.clear();
    this.mColumns.addAll(this.mColumnsToReuse);
    for (ColumnCount col : this.mColumns) {
      col.setCount(this.mMaxVersions);
    }
  }

  /**
   * This method is used to inform the column tracker that we are done with
   * this column. We may get this information from external filters or
   * timestamp range and we then need to indicate this information to
   * tracker. It is required only in case of ExplicitColumnTracker.
   * @param bytes
   * @param offset
   * @param length
   */
  public void doneWithColumn(byte [] bytes, int offset, int length) {
    while (this.mColumn != null) {
      int compare = Bytes.compareTo(mColumn.getBuffer(), mColumn.getOffset(),
          mColumn.getLength(), bytes, offset, length);
      if (compare == 0) {
        this.mColumns.remove(this.mIndex);
        if (this.mColumns.size() == this.mIndex) {
          // Will not hit any more columns in this storefile
          this.mColumn = null;
        } else {
          this.mColumn = this.mColumns.get(this.mIndex);
        }
        return;
      } else if (compare <= -1) {
        if (++this.mIndex != this.mColumns.size()) {
          this.mColumn = this.mColumns.get(this.mIndex);
        } else {
          this.mColumn = null;
        }
      } else {
        return;
      }
    }
  }

}
