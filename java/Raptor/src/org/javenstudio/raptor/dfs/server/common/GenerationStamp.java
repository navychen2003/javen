package org.javenstudio.raptor.dfs.server.common;

import java.io.*;

import org.javenstudio.raptor.io.*;


/****************************************************************
 * A GenerationStamp is a Hadoop FS primitive, identified by a long.
 ****************************************************************/
@SuppressWarnings("rawtypes")
public class GenerationStamp implements WritableComparable { //<GenerationStamp> {
  public static final long WILDCARD_STAMP = 1;
  public static final long FIRST_VALID_STAMP = 1000L;

  static {                                  // register a ctor
    WritableFactories.setFactory
      (GenerationStamp.class,
       new WritableFactory() {
         public Writable newInstance() { return new GenerationStamp(0); }
       });
  }

  long genstamp;

  /**
   * Create a new instance, initialized to FIRST_VALID_STAMP.
   */
  public GenerationStamp() {this(GenerationStamp.FIRST_VALID_STAMP);}

  /**
   * Create a new instance, initialized to the specified value.
   */
  GenerationStamp(long stamp) {this.genstamp = stamp;}

  /**
   * Returns the current generation stamp
   */
  public long getStamp() {
    return this.genstamp;
  }

  /**
   * Sets the current generation stamp
   */
  public void setStamp(long stamp) {
    this.genstamp = stamp;
  }

  /**
   * First increments the counter and then returns the stamp 
   */
  public synchronized long nextStamp() {
    this.genstamp++;
    return this.genstamp;
  }

  /////////////////////////////////////
  // Writable
  /////////////////////////////////////
  public void write(DataOutput out) throws IOException {
    out.writeLong(genstamp);
  }

  public void readFields(DataInput in) throws IOException {
    this.genstamp = in.readLong();
    if (this.genstamp < 0) {
      throw new IOException("Bad Generation Stamp: " + this.genstamp);
    }
  }

  /////////////////////////////////////
  // Comparable
  /////////////////////////////////////
  public static int compare(long x, long y) {
    return x < y? -1: x == y? 0: 1;
  }

  /** {@inheritDoc} */
  public int compareTo(GenerationStamp that) {
    return compare(this.genstamp, that.genstamp);
  }

  public int compareTo(Object that) {
    return compareTo((GenerationStamp)that); 
  }

  /** {@inheritDoc} */
  public boolean equals(Object o) {
    if (!(o instanceof GenerationStamp)) {
      return false;
    }
    return genstamp == ((GenerationStamp)o).genstamp;
  }

  public static boolean equalsWithWildcard(long x, long y) {
    return x == y || x == WILDCARD_STAMP || y == WILDCARD_STAMP;  
  }

  /** {@inheritDoc} */
  public int hashCode() {
    return 37 * 17 + (int) (genstamp^(genstamp>>>32));
  }
}
