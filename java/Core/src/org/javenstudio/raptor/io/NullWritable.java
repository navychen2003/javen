package org.javenstudio.raptor.io;

import java.io.*;


/** Singleton Writable with no data. */
@SuppressWarnings("rawtypes")
public class NullWritable implements WritableComparable {

  private static final NullWritable THIS = new NullWritable();

  private NullWritable() {}                       // no public ctor

  /** Returns the single instance of this class. */
  public static NullWritable get() { return THIS; }
  
  public String toString() {
    return "(null)";
  }

  public int hashCode() { return 0; }
  public int compareTo(Object other) {
    if (!(other instanceof NullWritable)) {
      throw new ClassCastException("can't compare " + other.getClass().getName() 
                                   + " to NullWritable");
    }
    return 0;
  }
  
  public boolean equals(Object other) { return other instanceof NullWritable; }
  public void readFields(DataInput in) throws IOException {}
  public void write(DataOutput out) throws IOException {}

  /** A Comparator &quot;optimized&quot; for NullWritable. */
  public static class Comparator extends WritableComparator {
    public Comparator() {
      super(NullWritable.class);
    }

    /**
     * Compare the buffers in serialized form.
     */
    public int compare(byte[] b1, int s1, int l1,
                       byte[] b2, int s2, int l2) {
      assert 0 == l1;
      assert 0 == l2;
      return 0;
    }
  }

  static {                                        // register this comparator
    WritableComparator.define(NullWritable.class, new Comparator());
  }
}
