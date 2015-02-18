package org.javenstudio.raptor.io;

import java.io.*;

/** A WritableComparable for ints. */
@SuppressWarnings("rawtypes")
public class IntWritable implements WritableComparable {
  private int value;

  public IntWritable() {}

  public IntWritable(int value) { set(value); }

  /** Set the value of this IntWritable. */
  public void set(int value) { this.value = value; }

  /** Return the value of this IntWritable. */
  public int get() { return value; }

  public void readFields(DataInput in) throws IOException {
    value = in.readInt();
  }

  public void write(DataOutput out) throws IOException {
    out.writeInt(value);
  }

  /** Returns true iff <code>o</code> is a IntWritable with the same value. */
  public boolean equals(Object o) {
    if (!(o instanceof IntWritable))
      return false;
    IntWritable other = (IntWritable)o;
    return this.value == other.value;
  }

  public int hashCode() {
    return value;
  }

  /** Compares two IntWritables. */
  public int compareTo(Object o) {
    int thisValue = this.value;
    int thatValue = ((IntWritable)o).value;
    return (thisValue<thatValue ? -1 : (thisValue==thatValue ? 0 : 1));
  }

  public String toString() {
    return Integer.toString(value);
  }

  /** A Comparator optimized for IntWritable. */ 
  public static class Comparator extends WritableComparator {
    public Comparator() {
      super(IntWritable.class);
    }

    public int compare(byte[] b1, int s1, int l1,
                       byte[] b2, int s2, int l2) {
      int thisValue = readInt(b1, s1);
      int thatValue = readInt(b2, s2);
      return (thisValue<thatValue ? -1 : (thisValue==thatValue ? 0 : 1));
    }
  }

  static {                                        // register this comparator
    WritableComparator.define(IntWritable.class, new Comparator());
  }
}

