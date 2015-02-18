package org.javenstudio.falcon.datum.table.store;

import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;
import java.util.Arrays;
import java.util.List;

import org.javenstudio.raptor.io.BytesWritable;
import org.javenstudio.raptor.io.WritableComparable;
import org.javenstudio.raptor.io.WritableComparator;

/**
 * A byte sequence that is usable as a key or value.  Based on
 * {@link org.javenstudio.raptor.io.BytesWritable} only this class is NOT resizable
 * and DOES NOT distinguish between the size of the seqeunce and the current
 * capacity as {@link org.javenstudio.raptor.io.BytesWritable} does. Hence its
 * comparatively 'immutable'. When creating a new instance of this class,
 * the underlying byte[] is not copied, just referenced.  The backing
 * buffer is accessed when we go to serialize.
 */
public class ImmutableBytesWritable implements WritableComparable<ImmutableBytesWritable> {

  private byte[] mBytes;
  private int mOffset;
  private int mLength;

  /**
   * Create a zero-size sequence.
   */
  public ImmutableBytesWritable() {
    super();
  }

  /**
   * Create a ImmutableBytesWritable using the byte array as the initial value.
   * @param bytes This array becomes the backing storage for the object.
   */
  public ImmutableBytesWritable(byte[] bytes) {
    this(bytes, 0, bytes.length);
  }

  /**
   * Set the new ImmutableBytesWritable to the contents of the passed
   * <code>ibw</code>.
   * @param ibw the value to set this ImmutableBytesWritable to.
   */
  public ImmutableBytesWritable(final ImmutableBytesWritable ibw) {
    this(ibw.get(), 0, ibw.getSize());
  }

  /**
   * Set the value to a given byte range
   * @param bytes the new byte range to set to
   * @param offset the offset in newData to start at
   * @param length the number of bytes in the range
   */
  public ImmutableBytesWritable(final byte[] bytes, final int offset,
      final int length) {
    this.mBytes = bytes;
    this.mOffset = offset;
    this.mLength = length;
  }

  /**
   * Get the data from the BytesWritable.
   * @return The data is only valid between offset and offset+length.
   */
  public byte[] get() {
    if (this.mBytes == null) {
      throw new IllegalStateException("Uninitialiized. Null constructor " +
        "called w/o accompaying readFields invocation");
    }
    return this.mBytes;
  }

  /**
   * @param b Use passed bytes as backing array for this instance.
   */
  public void set(final byte[] b) {
    set(b, 0, b.length);
  }

  /**
   * @param b Use passed bytes as backing array for this instance.
   * @param offset
   * @param length
   */
  public void set(final byte[] b, final int offset, final int length) {
    this.mBytes = b;
    this.mOffset = offset;
    this.mLength = length;
  }

  /**
   * @return the number of valid bytes in the buffer
   */
  public int getSize() {
    if (this.mBytes == null) {
      throw new IllegalStateException("Uninitialiized. Null constructor " +
        "called w/o accompaying readFields invocation");
    }
    return this.mLength;
  }

  /**
   * @return the number of valid bytes in the buffer
   */
  //Should probably deprecate getSize() so that we keep the same calls for all
  //byte[]
  public int getLength() {
    if (this.mBytes == null) {
      throw new IllegalStateException("Uninitialiized. Null constructor " +
        "called w/o accompaying readFields invocation");
    }
    return this.mLength;
  }

  /**
   * @return offset
   */
  public int getOffset(){
    return this.mOffset;
  }

  @Override
  public void readFields(final DataInput in) throws IOException {
    this.mLength = in.readInt();
    this.mBytes = new byte[this.mLength];
    in.readFully(this.mBytes, 0, this.mLength);
    this.mOffset = 0;
  }

  @Override
  public void write(final DataOutput out) throws IOException {
    out.writeInt(this.mLength);
    out.write(this.mBytes, this.mOffset, this.mLength);
  }

  // Below methods copied from BytesWritable
  @Override
  public int hashCode() {
    int hash = 1;
    for (int i = mOffset; i < mOffset + mLength; i++)
      hash = (31 * hash) + (int)mBytes[i];
    return hash;
  }

  /**
   * Define the sort order of the BytesWritable.
   * @param that The other bytes writable
   * @return Positive if left is bigger than right, 0 if they are equal, and
   *         negative if left is smaller than right.
   */
  @Override
  public int compareTo(ImmutableBytesWritable that) {
    return WritableComparator.compareBytes(
      this.mBytes, this.mOffset, this.mLength,
      that.mBytes, that.mOffset, that.mLength);
  }

  /**
   * Compares the bytes in this object to the specified byte array
   * @param that
   * @return Positive if left is bigger than right, 0 if they are equal, and
   *         negative if left is smaller than right.
   */
  public int compareTo(final byte[] that) {
    return WritableComparator.compareBytes(
      this.mBytes, this.mOffset, this.mLength,
      that, 0, that.length);
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object right_obj) {
	if (right_obj == this) return true;
	if (right_obj == null) return false;
    if (right_obj instanceof byte[]) {
      return compareTo((byte[])right_obj) == 0;
    }
    if (right_obj instanceof ImmutableBytesWritable) {
      return compareTo((ImmutableBytesWritable)right_obj) == 0;
    }
    return false;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(3*this.mBytes.length);
    for (int idx = mOffset; idx < mOffset + mLength; idx++) {
      // if not the first, put a blank separator in
      if (idx != mOffset) 
        sb.append(' ');
      
      String num = Integer.toHexString(mBytes[idx]);
      // if it is only one digit, add a leading 0.
      if (num.length() < 2) 
        sb.append('0');
      
      sb.append(num);
    }
    return sb.toString();
  }

  /** 
   * A Comparator optimized for ImmutableBytesWritable.
   */
  public static class Comparator extends WritableComparator {
    private BytesWritable.Comparator mComparator =
      new BytesWritable.Comparator();

    /** constructor */
    public Comparator() {
      super(ImmutableBytesWritable.class);
    }

    /**
     * @see WritableComparator#compare(byte[], int, int, byte[], int, int)
     */
    @Override
    public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
      return mComparator.compare(b1, s1, l1, b2, s2, l2);
    }
  }

  static { // register this comparator
    WritableComparator.define(ImmutableBytesWritable.class, new Comparator());
  }

  /**
   * @param array List of byte[].
   * @return Array of byte[].
   */
  public static byte[][] toArray(final List<byte[]> array) {
    // List#toArray doesn't work on lists of byte[].
    byte[][] results = new byte[array.size()][];
    for (int i = 0; i < array.size(); i++) {
      results[i] = array.get(i);
    }
    return results;
  }

  /**
   * Returns a copy of the bytes referred to by this writable
   */
  public byte[] copyBytes() {
    return Arrays.copyOfRange(mBytes, mOffset, mOffset+mLength);
  }
}
