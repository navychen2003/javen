package org.javenstudio.raptor.bigdb.filter;

import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.filter.CompareFilter.CompareOp;

/**
 * A {@link Filter} that checks a single column value, but does not emit the
 * tested column. This will enable a performance boost over
 * {@link SingleColumnValueFilter}, if the tested column value is not actually
 * needed as input (besides for the filtering itself).
 */
public class SingleColumnValueExcludeFilter extends SingleColumnValueFilter {

  /**
   * Writable constructor, do not use.
   */
  public SingleColumnValueExcludeFilter() {
    super();
  }

  /**
   * Constructor for binary compare of the value of a single column. If the
   * column is found and the condition passes, all columns of the row will be
   * emitted; except for the tested column value. If the column is not found or
   * the condition fails, the row will not be emitted.
   *
   * @param family name of column family
   * @param qualifier name of column qualifier
   * @param compareOp operator
   * @param value value to compare column values against
   */
  public SingleColumnValueExcludeFilter(byte[] family, byte[] qualifier,
      CompareOp compareOp, byte[] value) {
    super(family, qualifier, compareOp, value);
  }

  /**
   * Constructor for binary compare of the value of a single column. If the
   * column is found and the condition passes, all columns of the row will be
   * emitted; except for the tested column value. If the condition fails, the
   * row will not be emitted.
   * <p>
   * Use the filterIfColumnMissing flag to set whether the rest of the columns
   * in a row will be emitted if the specified column to check is not found in
   * the row.
   *
   * @param family name of column family
   * @param qualifier name of column qualifier
   * @param compareOp operator
   * @param comparator Comparator to use.
   */
  public SingleColumnValueExcludeFilter(byte[] family, byte[] qualifier,
      CompareOp compareOp, WritableByteArrayComparable comparator) {
    super(family, qualifier, compareOp, comparator);
  }

  public ReturnCode filterKeyValue(KeyValue keyValue) {
    ReturnCode superRetCode = super.filterKeyValue(keyValue);
    if (superRetCode == ReturnCode.INCLUDE) {
      // If the current column is actually the tested column,
      // we will skip it instead.
      if (keyValue.matchingColumn(this.columnFamily, this.columnQualifier)) {
        return ReturnCode.SKIP;
      }
    }
    return superRetCode;
  }
}

