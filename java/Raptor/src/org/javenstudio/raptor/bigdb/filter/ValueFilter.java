package org.javenstudio.raptor.bigdb.filter;

import org.javenstudio.raptor.bigdb.KeyValue;

/**
 * This filter is used to filter based on column value. It takes an
 * operator (equal, greater, not equal, etc) and a byte [] comparator for the
 * cell value.
 * <p>
 * This filter can be wrapped with {@link WhileMatchFilter} and {@link SkipFilter}
 * to add more control.
 * <p>
 * Multiple filters can be combined using {@link FilterList}.
 * <p>
 * To test the value of a single qualifier when scanning multiple qualifiers,
 * use {@link SingleColumnValueFilter}.
 */
public class ValueFilter extends CompareFilter {

  /**
   * Writable constructor, do not use.
   */
  public ValueFilter() {
  }

  /**
   * Constructor.
   * @param valueCompareOp the compare op for value matching
   * @param valueComparator the comparator for value matching
   */
  public ValueFilter(final CompareOp valueCompareOp,
      final WritableByteArrayComparable valueComparator) {
    super(valueCompareOp, valueComparator);
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue v) {
    if (doCompare(this.compareOp, this.comparator, v.getBuffer(),
        v.getValueOffset(), v.getValueLength())) {
      return ReturnCode.SKIP;
    }
    return ReturnCode.INCLUDE;
  }
}

