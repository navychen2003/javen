package org.javenstudio.raptor.bigdb.filter;

import org.javenstudio.raptor.bigdb.KeyValue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Simple filter that returns first N columns on row only.
 * This filter was written to test filters in Get and as soon as it gets
 * its quota of columns, {@link #filterAllRemaining()} returns true.  This
 * makes this filter unsuitable as a Scan filter.
 */
public class ColumnCountGetFilter extends FilterBase {
  private int limit = 0;
  private int count = 0;

  /**
   * Used during serialization.
   * Do not use.
   */
  public ColumnCountGetFilter() {
    super();
  }

  public ColumnCountGetFilter(final int n) {
    this.limit = n;
  }

  public int getLimit() {
    return limit;
  }

  @Override
  public boolean filterAllRemaining() {
    return this.count > this.limit;
  }

  @Override
  public ReturnCode filterKeyValue(KeyValue v) {
    this.count++;
    return filterAllRemaining() ? ReturnCode.SKIP: ReturnCode.INCLUDE;
  }

  @Override
  public void reset() {
    this.count = 0;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    this.limit = in.readInt();
  }

  @Override
  public void write(DataOutput out) throws IOException {
    out.writeInt(this.limit);
  }
}
