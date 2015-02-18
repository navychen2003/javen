package org.javenstudio.falcon.datum.table.store;

/**
 * Simple wrapper for a byte buffer and a counter.  Does not copy.
 * <p>
 * NOT thread-safe because it is not used in a multi-threaded context, yet.
 */
public class ColumnCount {

  private final byte[] mBytes;
  private final int mOffset;
  private final int mLength;
  private int mCount;

  /**
   * Constructor
   * @param column the qualifier to count the versions for
   */
  public ColumnCount(byte[] column) {
    this(column, 0);
  }

  /**
   * Constructor
   * @param column the qualifier to count the versions for
   * @param count initial count
   */
  public ColumnCount(byte[] column, int count) {
    this(column, 0, column.length, count);
  }

  /**
   * Constuctor
   * @param column the qualifier to count the versions for
   * @param offset in the passed buffer where to start the qualifier from
   * @param length of the qualifier
   * @param count initial count
   */
  public ColumnCount(byte[] column, int offset, int length, int count) {
    this.mBytes = column;
    this.mOffset = offset;
    this.mLength = length;
    this.mCount = count;
  }

  /**
   * @return the buffer
   */
  public byte[] getBuffer(){
    return this.mBytes;
  }

  /**
   * @return the offset
   */
  public int getOffset(){
    return this.mOffset;
  }

  /**
   * @return the length
   */
  public int getLength(){
    return this.mLength;
  }

  /**
   * Decrement the current version count
   * @return current count
   */
  public int decrement() {
    return --mCount;
  }

  /**
   * Increment the current version count
   * @return current count
   */
  public int increment() {
    return ++mCount;
  }

  /**
   * Set the current count to a new count
   * @param count new count to set
   */
  public void setCount(int count) {
    this.mCount = count;
  }

  /**
   * Check to see if needed to fetch more versions
   * @param max
   * @return true if more versions are needed, false otherwise
   */
  public boolean needMore(int max) {
    if (this.mCount < max) return true;
    return false;
  }
}
