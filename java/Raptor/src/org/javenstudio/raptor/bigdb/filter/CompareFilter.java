package org.javenstudio.raptor.bigdb.filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.javenstudio.raptor.bigdb.io.DBObjectWritable;

/**
 * This is a generic filter to be used to filter by comparison.  It takes an
 * operator (equal, greater, not equal, etc) and a byte [] comparator.
 * <p>
 * To filter by row key, use {@link RowFilter}.
 * <p>
 * To filter by column qualifier, use {@link QualifierFilter}.
 * <p>
 * To filter by value, use {@link SingleColumnValueFilter}.
 * <p>
 * These filters can be wrapped with {@link SkipFilter} and {@link WhileMatchFilter}
 * to add more control.
 * <p>
 * Multiple filters can be combined using {@link FilterList}.
 */
public abstract class CompareFilter extends FilterBase {

  /** Comparison operators. */
  public enum CompareOp {
    /** less than */
    LESS,
    /** less than or equal to */
    LESS_OR_EQUAL,
    /** equals */
    EQUAL,
    /** not equal */
    NOT_EQUAL,
    /** greater than or equal to */
    GREATER_OR_EQUAL,
    /** greater than */
    GREATER,
    /** no operation */
    NO_OP,
  }

  protected CompareOp compareOp;
  protected WritableByteArrayComparable comparator;

  /**
   * Writable constructor, do not use.
   */
  public CompareFilter() {
  }

  /**
   * Constructor.
   * @param compareOp the compare op for row matching
   * @param comparator the comparator for row matching
   */
  public CompareFilter(final CompareOp compareOp,
      final WritableByteArrayComparable comparator) {
    this.compareOp = compareOp;
    this.comparator = comparator;
  }

  /**
   * @return operator
   */
  public CompareOp getOperator() {
    return compareOp;
  }

  /**
   * @return the comparator
   */
  public WritableByteArrayComparable getComparator() {
    return comparator;
  }

  protected boolean doCompare(final CompareOp compareOp,
      final WritableByteArrayComparable comparator, final byte [] data,
      final int offset, final int length) {
      if (compareOp == CompareOp.NO_OP) {
	  return true;
      }
    int compareResult =
      comparator.compareTo(Arrays.copyOfRange(data, offset,
        offset + length));
    switch (compareOp) {
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
        throw new RuntimeException("Unknown Compare op " +
          compareOp.name());
    }
  }

  public void readFields(DataInput in) throws IOException {
    compareOp = CompareOp.valueOf(in.readUTF());
    comparator = (WritableByteArrayComparable)
      DBObjectWritable.readObject(in, null);
  }

  public void write(DataOutput out) throws IOException {
    out.writeUTF(compareOp.name());
    DBObjectWritable.writeObject(out, comparator,
      WritableByteArrayComparable.class, null);
  }
}

