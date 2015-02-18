package org.javenstudio.raptor.bigdb.filter;

/**
 * A binary comparator which lexicographically compares against the specified
 * byte array using {@link org.javenstudio.raptor.bigdb.util.Bytes#compareTo(byte[], byte[])}.
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

