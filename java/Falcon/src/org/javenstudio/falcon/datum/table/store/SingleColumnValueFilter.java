package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * This filter is used to filter cells based on value. It takes a {@link CompareFilter.CompareOp}
 * operator (equal, greater, not equal, etc), and either a byte[] value or
 * a WritableByteArrayComparable.
 * <p>
 * If we have a byte[] value then we just do a lexicographic compare. For
 * example, if passed value is 'b' and cell has 'a' and the compare operator
 * is LESS, then we will filter out this cell (return true).  If this is not
 * sufficient (eg you want to deserialize a long and then compare it to a fixed
 * long value), then you can pass in your own comparator instead.
 * <p>
 * You must also specify a family and qualifier.  Only the value of this column
 * will be tested. When using this filter on a {@link Scan} with specified
 * inputs, the column to be tested should also be added as input (otherwise
 * the filter will regard the column as missing).
 * <p>
 * To prevent the entire row from being emitted if the column is not found
 * on a row, use {@link #setFilterIfMissing}.
 * Otherwise, if the column is found, the entire row will be emitted only if
 * the value passes.  If the value fails, the row will be filtered out.
 * <p>
 * In order to test values of previous versions (timestamps), set
 * {@link #setLatestVersionOnly} to false. The default is true, meaning that
 * only the latest version's value is tested and all previous versions are ignored.
 * <p>
 * To filter based on the value of all scanned columns, use {@link ValueFilter}.
 */
public class SingleColumnValueFilter extends FilterBase {
  //private static final Logger LOG = Logger.getLogger(SingleColumnValueFilter.class);

  protected byte[] mColumnFamily;
  protected byte[] mColumnQualifier;
  private CompareFilter.CompareOp mCompareOp;
  private WritableByteArrayComparable mComparator;
  private boolean mFoundColumn = false;
  private boolean mMatchedColumn = false;
  private boolean mFilterIfMissing = false;
  private boolean mLatestVersionOnly = true;

  /**
   * Writable constructor, do not use.
   */
  public SingleColumnValueFilter() {
  }

  /**
   * Constructor for binary compare of the value of a single column.  If the
   * column is found and the condition passes, all columns of the row will be
   * emitted.  If the column is not found or the condition fails, the row will
   * not be emitted.
   *
   * @param family name of column family
   * @param qualifier name of column qualifier
   * @param compareOp operator
   * @param value value to compare column values against
   */
  public SingleColumnValueFilter(final byte[] family, final byte[] qualifier,
      final CompareFilter.CompareOp compareOp, final byte[] value) {
    this(family, qualifier, compareOp, new BinaryComparator(value));
  }

  /**
   * Constructor for binary compare of the value of a single column.  If the
   * column is found and the condition passes, all columns of the row will be
   * emitted.  If the condition fails, the row will not be emitted.
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
  public SingleColumnValueFilter(final byte[] family, final byte[] qualifier,
      final CompareFilter.CompareOp compareOp, final WritableByteArrayComparable comparator) {
    this.mColumnFamily = family;
    this.mColumnQualifier = qualifier;
    this.mCompareOp = compareOp;
    this.mComparator = comparator;
  }

  /**
   * @return operator
   */
  public CompareFilter.CompareOp getOperator() {
    return mCompareOp;
  }

  /**
   * @return the comparator
   */
  public WritableByteArrayComparable getComparator() {
    return mComparator;
  }

  /**
   * @return the family
   */
  public byte[] getFamily() {
    return mColumnFamily;
  }

  /**
   * @return the qualifier
   */
  public byte[] getQualifier() {
    return mColumnQualifier;
  }

  public ReturnCode filterKeyValue(KeyValue keyValue) {
    if (this.mMatchedColumn) {
      // We already found and matched the single column, all keys now pass
      return ReturnCode.INCLUDE;
    } else if (this.mLatestVersionOnly && this.mFoundColumn) {
      // We found but did not match the single column, skip to next row
      return ReturnCode.NEXT_ROW;
    }
    if (!keyValue.matchingColumn(this.mColumnFamily, this.mColumnQualifier)) {
      return ReturnCode.INCLUDE;
    }
    mFoundColumn = true;
    if (filterColumnValue(keyValue.getBuffer(),
        keyValue.getValueOffset(), keyValue.getValueLength())) {
      return this.mLatestVersionOnly? ReturnCode.NEXT_ROW: ReturnCode.INCLUDE;
    }
    this.mMatchedColumn = true;
    return ReturnCode.INCLUDE;
  }

  private boolean filterColumnValue(final byte[] data, final int offset,
      final int length) {
    // TODO: Can this filter take a rawcomparator so don't have to make this
    // byte array copy?
    int compareResult =
      this.mComparator.compareTo(Arrays.copyOfRange(data, offset, offset + length));
    switch (this.mCompareOp) {
    case LESS:
      return compareResult <= 0;
    case LESS_OR_EQUAL:
      return compareResult < 0;
    case EQUAL:
      return compareResult != 0;
    case NOT_EQUAL:
      return compareResult == 0;
    case GREATER_OR_EQUAL:
      return compareResult > 0;
    case GREATER:
      return compareResult >= 0;
    default:
      throw new RuntimeException("Unknown Compare op " + mCompareOp.name());
    }
  }

  public boolean filterRow() {
    // If column was found, return false if it was matched, true if it was not
    // If column not found, return true if we filter if missing, false if not
    return this.mFoundColumn? !this.mMatchedColumn: this.mFilterIfMissing;
  }

  public void reset() {
    mFoundColumn = false;
    mMatchedColumn = false;
  }

  /**
   * Get whether entire row should be filtered if column is not found.
   * @return true if row should be skipped if column not found, false if row
   * should be let through anyways
   */
  public boolean getFilterIfMissing() {
    return mFilterIfMissing;
  }

  /**
   * Set whether entire row should be filtered if column is not found.
   * <p>
   * If true, the entire row will be skipped if the column is not found.
   * <p>
   * If false, the row will pass if the column is not found.  This is default.
   * @param filterIfMissing flag
   */
  public void setFilterIfMissing(boolean filterIfMissing) {
    this.mFilterIfMissing = filterIfMissing;
  }

  /**
   * Get whether only the latest version of the column value should be compared.
   * If true, the row will be returned if only the latest version of the column
   * value matches. If false, the row will be returned if any version of the
   * column value matches. The default is true.
   * @return return value
   */
  public boolean getLatestVersionOnly() {
    return mLatestVersionOnly;
  }

  /**
   * Set whether only the latest version of the column value should be compared.
   * If true, the row will be returned if only the latest version of the column
   * value matches. If false, the row will be returned if any version of the
   * column value matches. The default is true.
   * @param latestVersionOnly flag
   */
  public void setLatestVersionOnly(boolean latestVersionOnly) {
    this.mLatestVersionOnly = latestVersionOnly;
  }

  @Override
  public void readFields(final DataInput in) throws IOException {
    this.mColumnFamily = Bytes.readByteArray(in);
    if (this.mColumnFamily.length == 0) {
      this.mColumnFamily = null;
    }
    this.mColumnQualifier = Bytes.readByteArray(in);
    if (this.mColumnQualifier.length == 0) {
      this.mColumnQualifier = null;
    }
    this.mCompareOp = CompareFilter.CompareOp.valueOf(in.readUTF());
    this.mComparator =
      (WritableByteArrayComparable)DBObjectWritable.readObject(in, null);
    this.mFoundColumn = in.readBoolean();
    this.mMatchedColumn = in.readBoolean();
    this.mFilterIfMissing = in.readBoolean();
    this.mLatestVersionOnly = in.readBoolean();
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    Bytes.writeByteArray(out, this.mColumnFamily);
    Bytes.writeByteArray(out, this.mColumnQualifier);
    out.writeUTF(mCompareOp.name());
    DBObjectWritable.writeObject(out, mComparator,
        WritableByteArrayComparable.class, null);
    out.writeBoolean(mFoundColumn);
    out.writeBoolean(mMatchedColumn);
    out.writeBoolean(mFilterIfMissing);
    out.writeBoolean(mLatestVersionOnly);
  }
  
}
