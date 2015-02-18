package org.javenstudio.raptor.bigdb.filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * A Filter that stops after the given row.  There is no "RowStopFilter" because
 * the Scan spec allows you to specify a stop row.
 *
 * Use this filter to include the stop row, eg: [A,Z].
 */
public class InclusiveStopFilter extends FilterBase {
  private byte [] stopRowKey;
  private boolean done = false;

  public InclusiveStopFilter() {
    super();
  }

  public InclusiveStopFilter(final byte [] stopRowKey) {
    this.stopRowKey = stopRowKey;
  }

  public byte[] getStopRowKey() {
    return this.stopRowKey;
  }

  public boolean filterRowKey(byte[] buffer, int offset, int length) {
    if (buffer == null) {
      //noinspection RedundantIfStatement
      if (this.stopRowKey == null) {
        return true; //filter...
      }
      return false;
    }
    // if stopRowKey is <= buffer, then true, filter row.
    int cmp = Bytes.compareTo(stopRowKey, 0, stopRowKey.length,
      buffer, offset, length);

    if(cmp < 0) {
      done = true;
    }
    return done;
  }

  public boolean filterAllRemaining() {
    return done;
  }

  public void write(DataOutput out) throws IOException {
    Bytes.writeByteArray(out, this.stopRowKey);
  }

  public void readFields(DataInput in) throws IOException {
    this.stopRowKey = Bytes.readByteArray(in);
  }
}
