package org.javenstudio.raptor.io;

import java.io.*;

import org.javenstudio.raptor.classification.InterfaceAudience;
import org.javenstudio.raptor.classification.InterfaceStability;

/** A WritableComparable for integer values stored in variable-length format.
 * Such values take between one and five bytes.  Smaller values take fewer bytes.
 * 
 * @see org.javenstudio.raptor.io.WritableUtils#readVInt(DataInput)
 */
@SuppressWarnings("rawtypes")
@InterfaceAudience.Public
@InterfaceStability.Stable
public class VIntWritable implements WritableComparable {
  private int value;

  public VIntWritable() {}

  public VIntWritable(int value) { set(value); }

  /** Set the value of this VIntWritable. */
  public void set(int value) { this.value = value; }

  /** Return the value of this VIntWritable. */
  public int get() { return value; }

  public void readFields(DataInput in) throws IOException {
    value = WritableUtils.readVInt(in);
  }

  public void write(DataOutput out) throws IOException {
    WritableUtils.writeVInt(out, value);
  }

  /** Returns true iff <code>o</code> is a VIntWritable with the same value. */
  public boolean equals(Object o) {
    if (!(o instanceof VIntWritable))
      return false;
    VIntWritable other = (VIntWritable)o;
    return this.value == other.value;
  }

  public int hashCode() {
    return value;
  }

  /** Compares two VIntWritables. */
  public int compareTo(Object o) {
    int thisValue = this.value;
    int thatValue = ((VIntWritable)o).value;
    return (thisValue < thatValue ? -1 : (thisValue == thatValue ? 0 : 1));
  }

  public String toString() {
    return Integer.toString(value);
  }

}

