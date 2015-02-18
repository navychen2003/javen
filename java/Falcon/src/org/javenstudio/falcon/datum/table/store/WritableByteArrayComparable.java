package org.javenstudio.falcon.datum.table.store;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.javenstudio.raptor.io.Writable;

/** Base class, combines Comparable<byte[]> and Writable. */
public abstract class WritableByteArrayComparable implements Writable, Comparable<byte[]> {

  private byte[] mValue;

  /**
   * Nullary constructor, for Writable
   */
  public WritableByteArrayComparable() { }

  /**
   * Constructor.
   * @param value the value to compare against
   */
  public WritableByteArrayComparable(byte[] value) {
    this.mValue = value;
  }

  public byte[] getValue() {
    return mValue;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    mValue = Bytes.readByteArray(in);
  }

  @Override
  public void write(DataOutput out) throws IOException {
    Bytes.writeByteArray(out, mValue);
  }

  @Override
  public int compareTo(byte[] value) {
    return Bytes.compareTo(this.mValue, value);
  }

}
