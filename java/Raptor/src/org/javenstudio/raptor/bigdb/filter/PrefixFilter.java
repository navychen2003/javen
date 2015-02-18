package org.javenstudio.raptor.bigdb.filter;

import java.io.DataOutput;
import java.io.IOException;
import java.io.DataInput;

import org.javenstudio.raptor.bigdb.util.Bytes;

/**
 * Pass results that have same row prefix.
 */
public class PrefixFilter extends FilterBase {
  protected byte [] prefix = null;
  protected boolean passedPrefix = false;

  public PrefixFilter(final byte [] prefix) {
    this.prefix = prefix;
  }

  public PrefixFilter() {
    super();
  }

  public byte[] getPrefix() {
    return prefix;
  }

  public boolean filterRowKey(byte[] buffer, int offset, int length) {
    if (buffer == null || this.prefix == null)
      return true;
    if (length < prefix.length)
      return true;
    // if they are equal, return false => pass row
    // else return true, filter row
    // if we are passed the prefix, set flag
    int cmp = Bytes.compareTo(buffer, offset, this.prefix.length, this.prefix, 0,
        this.prefix.length);
    if(cmp > 0) {
      passedPrefix = true;
    }
    return cmp != 0;
  }

  public boolean filterAllRemaining() {
    return passedPrefix;
  }

  public void write(DataOutput out) throws IOException {
    Bytes.writeByteArray(out, this.prefix);
  }

  public void readFields(DataInput in) throws IOException {
    this.prefix = Bytes.readByteArray(in);
  }
}
