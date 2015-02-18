package org.javenstudio.common.indexdb.util;

/** 
 * Represents long[], as a slice (offset + length) into an
 *  existing long[].  The {@link #longs} member should never be null; use
 *  {@link #EMPTY_LONGS} if necessary.
 */
public final class LongsRef implements Comparable<LongsRef>, Cloneable {

  public static final long[] EMPTY_LONGS = new long[0];

  public long[] mLongs;
  public int mOffset;
  public int mLength;

  public final int getLength() { return mLength; }
  public final int getOffset() { return mOffset; }
  public final long getLongAt(int pos) { return mLongs[pos]; }
  public final long[] getLongs() { return mLongs; }
  
  public void setLength(int len) { 
	  if (len < 0) throw new IllegalArgumentException("input length: "+len+" wrong");
	  mLength = len;
  }
  
  public void setLongAt(int pos, long val) { 
	  mLongs[pos] = val;
  }
  
  public LongsRef() {
    mLongs = EMPTY_LONGS;
  }

  public LongsRef(int capacity) {
    mLongs = new long[capacity];
  }

  public LongsRef(long[] longs, int offset, int length) {
    assert longs != null;
    assert offset >= 0;
    assert length >= 0;
    assert longs.length >= offset + length;
    mLongs = longs;
    mOffset = offset;
    mLength = length;
  }

  @Override
  public LongsRef clone() {
    return new LongsRef(mLongs, mOffset, mLength);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 0;
    final long end = mOffset + mLength;
    for (int i = mOffset; i < end; i++) {
      result = prime * result + (int) (mLongs[i] ^ (mLongs[i]>>>32));
    }
    return result;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other instanceof LongsRef) {
      return this.longsEquals((LongsRef) other);
    }
    return false;
  }

  public boolean longsEquals(LongsRef other) {
    if (mLength == other.mLength) {
      int otherUpto = other.mOffset;
      final long[] otherInts = other.mLongs;
      final long end = mOffset + mLength;
      for (int upto=mOffset; upto<end; upto++,otherUpto++) {
        if (mLongs[upto] != otherInts[otherUpto]) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  /** Signed int order comparison */
  public int compareTo(LongsRef other) {
    if (this == other) return 0;

    final long[] aInts = this.mLongs;
    int aUpto = this.mOffset;
    final long[] bInts = other.mLongs;
    int bUpto = other.mOffset;

    final long aStop = aUpto + Math.min(this.mLength, other.mLength);

    while(aUpto < aStop) {
      long aInt = aInts[aUpto++];
      long bInt = bInts[bUpto++];
      if (aInt > bInt) {
        return 1;
      } else if (aInt < bInt) {
        return -1;
      }
    }

    // One is a prefix of the other, or, they are equal:
    return this.mLength - other.mLength;
  }

  public void copyLongs(LongsRef other) {
    if (mLongs.length - mOffset < other.mLength) {
      mLongs = new long[other.mLength];
      mOffset = 0;
    }
    System.arraycopy(other.mLongs, other.mOffset, mLongs, mOffset, other.mLength);
    mLength = other.mLength;
  }

  /** 
   * Used to grow the reference array. 
   * 
   * In general this should not be used as it does not take the offset into account.
   */
  public void grow(int newLength) {
    assert mOffset == 0;
    if (mLongs.length < newLength) {
      mLongs = ArrayUtil.grow(mLongs, newLength);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    final long end = mOffset + mLength;
    for (int i=mOffset; i < end; i++) {
      if (i > mOffset) {
        sb.append(' ');
      }
      sb.append(Long.toHexString(mLongs[i]));
    }
    sb.append(']');
    return sb.toString();
  }
  
  /**
   * Creates a new IntsRef that points to a copy of the longs from 
   * <code>other</code>
   * <p>
   * The returned IntsRef will have a length of other.length
   * and an offset of zero.
   */
  public static LongsRef deepCopyOf(LongsRef other) {
    LongsRef clone = new LongsRef();
    clone.copyLongs(other);
    return clone;
  }
  
}
