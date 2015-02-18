package org.javenstudio.raptor.bigdb.filter;

import org.javenstudio.raptor.bigdb.KeyValue;
import org.javenstudio.raptor.bigdb.client.Get;

/**
 * This filter is used to filter based on the column qualifier. It takes an
 * operator (equal, greater, not equal, etc) and a byte [] comparator for the
 * column qualifier portion of a key.
 * <p>
 * This filter can be wrapped with {@link WhileMatchFilter} and {@link SkipFilter}
 * to add more control.
 * <p>
 * Multiple filters can be combined using {@link FilterList}.
 * <p>
 * If an already known column qualifier is looked for, use {@link Get#addColumn}
 * directly rather than a filter.
 */
public class QualifierFilter extends CompareFilter {

  /**
   * Writable constructor, do not use.
   */
  public QualifierFilter() {
  }

  /**
   * Constructor.
   * @param qualifierCompareOp the compare op for column qualifier matching
   * @param qualifierComparator the comparator for column qualifier matching
   */
  public QualifierFilter(final CompareOp qualifierCompareOp,
      final WritableByteArrayComparable qualifierComparator) {
    super(qualifierCompareOp, qualifierComparator);
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue v) {
    int qualifierLength = v.getQualifierLength();
    if (qualifierLength > 0) {
      if (doCompare(this.compareOp, this.comparator, v.getBuffer(),
          v.getQualifierOffset(), qualifierLength)) {
        return ReturnCode.SKIP;
      }
    }
    return ReturnCode.INCLUDE;
  }
}

