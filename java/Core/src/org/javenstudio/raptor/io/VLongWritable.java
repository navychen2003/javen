package org.javenstudio.raptor.io;

import java.io.*;

import org.javenstudio.raptor.classification.InterfaceAudience;
import org.javenstudio.raptor.classification.InterfaceStability;

/** A WritableComparable for longs in a variable-length format. Such values take
 *  between one and five bytes.  Smaller values take fewer bytes.
 *  
 *  @see org.javenstudio.raptor.io.WritableUtils#readVLong(DataInput)
 */
@SuppressWarnings("rawtypes")
@InterfaceAudience.Public
@InterfaceStability.Stable
public class VLongWritable implements WritableComparable {
  private long value;

  public VLongWritable() {}

  public VLongWritable(long value) { set(value); }

  /** Set the value of this LongWritable. */
  public void set(long value) { this.value = value; }

  /** Return the value of this LongWritable. */
  public long get() { return value; }

  public void readFields(DataInput in) throws IOException {
    value = WritableUtils.readVLong(in);
  }

  public void write(DataOutput out) throws IOException {
    WritableUtils.writeVLong(out, value);
  }

  /** Returns true iff <code>o</code> is a VLongWritable with the same value. */
  public boolean equals(Object o) {
    if (!(o instanceof VLongWritable))
      return false;
    VLongWritable other = (VLongWritable)o;
    return this.value == other.value;
  }

  public int hashCode() {
    return (int)value;
  }

  /** Compares two VLongWritables. */
  public int compareTo(Object o) {
    long thisValue = this.value;
    long thatValue = ((VLongWritable)o).value;
    return (thisValue < thatValue ? -1 : (thisValue == thatValue ? 0 : 1));
  }

  public String toString() {
    return Long.toString(value);
  }

}

