package org.javenstudio.raptor.bigdb.filter;

import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.client.Scan;

/**
 * This filter is used to filter based on the key. It takes an operator
 * (equal, greater, not equal, etc) and a byte [] comparator for the row,
 * and column qualifier portions of a key.
 * <p>
 * This filter can be wrapped with {@link WhileMatchFilter} to add more control.
 * <p>
 * Multiple filters can be combined using {@link FilterList}.
 * <p>
 * If an already known row range needs to be scanned, use {@link Scan} start
 * and stop rows directly rather than a filter.
 */
public class RowFilter extends CompareFilter {

  private boolean filterOutRow = false;

  /**
   * Writable constructor, do not use.
   */
  public RowFilter() {
    super();
  }

  /**
   * Constructor.
   * @param rowCompareOp the compare op for row matching
   * @param rowComparator the comparator for row matching
   */
  public RowFilter(final CompareOp rowCompareOp,
      final WritableByteArrayComparable rowComparator) {
    super(rowCompareOp, rowComparator);
  }

  @Override
  public void reset() {
    this.filterOutRow = false;
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue v) {
    if(this.filterOutRow) {
      return ReturnCode.NEXT_ROW;
    }
    return ReturnCode.INCLUDE;
  }

  @Override
  public boolean filterRowKey(byte[] data, int offset, int length) {
    if(doCompare(this.compareOp, this.comparator, data, offset, length)) {
      this.filterOutRow = true;
    }
    return this.filterOutRow;
  }

  @Override
  public boolean filterRow() {
    return this.filterOutRow;
  }
}
