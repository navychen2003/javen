package org.javenstudio.raptor.bigdb.filter;

import java.io.DataOutput;
import java.io.IOException;
import java.io.DataInput;

import org.javenstudio.raptor.bigdb.KeyValue;

/**
 * A filter that will only return the first KV from each row.
 * <p>
 * This filter can be used to more efficiently perform row count operations.
 */
public class FirstKeyOnlyFilter extends FilterBase {
  private boolean foundKV = false;

  public FirstKeyOnlyFilter() {
  }

  public void reset() {
    foundKV = false;
  }

  public ReturnCode filterKeyValue(KeyValue v) {
    if(foundKV) return ReturnCode.NEXT_ROW;
    foundKV = true;
    return ReturnCode.INCLUDE;
  }

  public void write(DataOutput out) throws IOException {
  }

  public void readFields(DataInput in) throws IOException {
  }
}

