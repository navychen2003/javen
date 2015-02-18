package org.javenstudio.raptor.bigdb.filter;

import org.javenstudio.raptor.bigdb.util.Bytes;
import org.javenstudio.raptor.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Base class, combines Comparable<byte []> and Writable. */
public abstract class WritableByteArrayComparable implements Writable, Comparable<byte[]> {

  byte[] value;

  /**
   * Nullary constructor, for Writable
   */
  public WritableByteArrayComparable() { }

  /**
   * Constructor.
   * @param value the value to compare against
   */
  public WritableByteArrayComparable(byte [] value) {
    this.value = value;
  }

  public byte[] getValue() {
    return value;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    value = Bytes.readByteArray(in);
  }

  @Override
  public void write(DataOutput out) throws IOException {
    Bytes.writeByteArray(out, value);
  }

  @Override
  public int compareTo(byte [] value) {
    return Bytes.compareTo(this.value, value);
  }

}

