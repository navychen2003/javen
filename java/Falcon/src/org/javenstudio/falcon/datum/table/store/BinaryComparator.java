package org.javenstudio.falcon.datum.table.store;

/**
 * A binary comparator which lexicographically compares against the specified
 * byte array using {@link Bytes#compareTo(byte[], byte[])}.
 */
public class BinaryComparator extends WritableByteArrayComparable {

  /** Nullary constructor for Writable, do not use */
  public BinaryComparator() { }

  /**
   * Constructor
   * @param value value
   */
  public BinaryComparator(byte[] value) {
    super(value);
  }

}
