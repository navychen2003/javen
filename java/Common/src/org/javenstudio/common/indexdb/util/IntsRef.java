package org.javenstudio.common.indexdb.util;

/** 
 * Represents int[], as a slice (offset + length) into an
 *  existing int[].  The {@link #ints} member should never be null; use
 *  {@link #EMPTY_INTS} if necessary.
 *
 */
public final class IntsRef implements Comparable<IntsRef>, Cloneable {
  public static final int[] EMPTY_INTS = new int[0];

  public int[] mInts;
  public int mOffset;
  public int mLength;

  public final int getLength() { return mLength; }
  public final int getOffset() { return mOffset; }
  public final int getIntAt(int pos) { return mInts[pos]; }
  public final int[] getInts() { return mInts; }
  
  public void setLength(int len) { 
	  if (len < 0) throw new IllegalArgumentException("input length: "+len+" wrong");
	  mLength = len;
  }
  
  public void setIntAt(int pos, int val) { 
	  mInts[pos] = val;
  }
  
  public IntsRef() {
    mInts = EMPTY_INTS;
  }

  public IntsRef(int capacity) {
    mInts = new int[capacity];
  }

  public IntsRef(int[] ints, int offset, int length) {
    assert ints != null;
    assert offset >= 0;
    assert length >= 0;
    assert ints.length >= offset + length;
    mInts = ints;
    mOffset = offset;
    mLength = length;
  }

  @Override
  public IntsRef clone() {
    return new IntsRef(mInts, mOffset, mLength);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 0;
    final int end = mOffset + mLength;
    for (int i = mOffset; i < end; i++) {
      result = prime * result + mInts[i];
    }
    return result;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other instanceof IntsRef) {
      return this.intsEquals((IntsRef) other);
    }
    return false;
  }

  public boolean intsEquals(IntsRef other) {
    if (mLength == other.mLength) {
      int otherUpto = other.mOffset;
      final int[] otherInts = other.mInts;
      final int end = mOffset + mLength;
      for (int upto=mOffset; upto < end; upto++,otherUpto++) {
        if (mInts[upto] != otherInts[otherUpto]) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  /** Signed int order comparison */
  public int compareTo(IntsRef other) {
    if (this == other) return 0;

    final int[] aInts = this.mInts;
    int aUpto = this.mOffset;
    final int[] bInts = other.mInts;
    int bUpto = other.mOffset;

    final int aStop = aUpto + Math.min(this.mLength, other.mLength);

    while (aUpto < aStop) {
      int aInt = aInts[aUpto++];
      int bInt = bInts[bUpto++];
      if (aInt > bInt) {
        return 1;
      } else if (aInt < bInt) {
        return -1;
      }
    }

    // One is a prefix of the other, or, they are equal:
    return this.mLength - other.mLength;
  }

  public void copyInts(IntsRef other) {
    if (mInts.length - mOffset < other.mLength) {
      mInts = new int[other.mLength];
      mOffset = 0;
    }
    System.arraycopy(other.mInts, other.mOffset, mInts, mOffset, other.mLength);
    mLength = other.mLength;
  }

  /** 
   * Used to grow the reference array. 
   * 
   * In general this should not be used as it does not take the offset into account.
   */
  public void grow(int newLength) {
    assert mOffset == 0;
    if (mInts.length < newLength) {
      mInts = ArrayUtil.grow(mInts, newLength);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    final int end = mOffset + mLength;
    for (int i=mOffset; i < end; i++) {
      if (i > mOffset) {
        sb.append(' ');
      }
      sb.append(Integer.toHexString(mInts[i]));
    }
    sb.append(']');
    return sb.toString();
  }
  
  /**
   * Creates a new IntsRef that points to a copy of the ints from 
   * <code>other</code>
   * <p>
   * The returned IntsRef will have a length of other.length
   * and an offset of zero.
   */
  public static IntsRef deepCopyOf(IntsRef other) {
    IntsRef clone = new IntsRef();
    clone.copyInts(other);
    return clone;
  }
  
}
