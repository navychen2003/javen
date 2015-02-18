package org.javenstudio.common.indexdb.util;

import java.util.Comparator;

/**
 * Represents char[], as a slice (offset + length) into an existing char[].
 * The {@link #chars} member should never be null; use
 * {@link #EMPTY_CHARS} if necessary.
 * 
 */
public final class CharsRef implements Comparable<CharsRef>, CharSequence, Cloneable {
  public static final char[] EMPTY_CHARS = new char[0];
  
  public char[] mChars;
  public int mOffset;
  public int mLength;

  /**
   * Creates a new {@link CharsRef} initialized an empty array zero-length
   */
  public CharsRef() {
    this(EMPTY_CHARS, 0, 0);
  }

  /**
   * Creates a new {@link CharsRef} initialized with an array of the given
   * capacity
   */
  public CharsRef(int capacity) {
    mChars = new char[capacity];
  }

  /**
   * Creates a new {@link CharsRef} initialized with the given array, offset and
   * length
   */
  public CharsRef(char[] chars, int offset, int length) {
    assert chars != null;
    assert offset >= 0;
    assert length >= 0;
    assert chars.length >= offset + length;
    mChars = chars;
    mOffset = offset;
    mLength = length;
  }

  /**
   * Creates a new {@link CharsRef} initialized with the given Strings character
   * array
   */
  public CharsRef(String string) {
    mChars = string.toCharArray();
    mOffset = 0;
    mLength = mChars.length;
  }

  public final char[] getChars() { return mChars; }
  public final int getOffset() { return mOffset; }
  public final int getLength() { return mLength; }
  
  @Override
  public CharsRef clone() {
    return new CharsRef(mChars, mOffset, mLength);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 0;
    final int end = mOffset + mLength;
    for (int i = mOffset; i < end; i++) {
      result = prime * result + mChars[i];
    }
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other instanceof CharsRef) {
      return this.charsEquals((CharsRef) other);
    }
    return false;
  }

  public boolean charsEquals(CharsRef other) {
    if (mLength == other.mLength) {
      int otherUpto = other.mOffset;
      final char[] otherChars = other.mChars;
      final int end = mOffset + mLength;
      for (int upto = mOffset; upto < end; upto++, otherUpto++) {
        if (mChars[upto] != otherChars[otherUpto]) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  /** Signed int order comparison */
  public int compareTo(CharsRef other) {
    if (this == other)
      return 0;

    final char[] aChars = this.mChars;
    int aUpto = this.mOffset;
    final char[] bChars = other.mChars;
    int bUpto = other.mOffset;

    final int aStop = aUpto + Math.min(this.mLength, other.mLength);

    while (aUpto < aStop) {
      int aInt = aChars[aUpto++];
      int bInt = bChars[bUpto++];
      if (aInt > bInt) {
        return 1;
      } else if (aInt < bInt) {
        return -1;
      }
    }

    // One is a prefix of the other, or, they are equal:
    return this.mLength - other.mLength;
  }
  
  /**
   * Copies the given {@link CharsRef} referenced content into this instance.
   * 
   * @param other the {@link CharsRef} to copy
   */
  public void copyChars(CharsRef other) {
    copyChars(other.mChars, other.mOffset, other.mLength);
  }

  /** 
   * Used to grow the reference array. 
   * 
   * In general this should not be used as it does not take the offset into account.
   */
  public void grow(int newLength) {
    assert mOffset == 0;
    if (mChars.length < newLength) {
      mChars = ArrayUtil.grow(mChars, newLength);
    }
  }

  /**
   * Copies the given array into this CharsRef.
   */
  public void copyChars(char[] otherChars, int otherOffset, int otherLength) {
    if (mChars.length - mOffset < otherLength) {
      mChars = new char[otherLength];
      mOffset = 0;
    }
    System.arraycopy(otherChars, otherOffset, mChars, mOffset, otherLength);
    mLength = otherLength;
  }

  /**
   * Appends the given array to this CharsRef
   */
  public void append(char[] otherChars, int otherOffset, int otherLength) {
    int newLen = mLength + otherLength;
    if (mChars.length - mOffset < newLen) {
      char[] newChars = new char[newLen];
      System.arraycopy(mChars, mOffset, newChars, 0, mLength);
      mOffset = 0;
      mChars = newChars;
    }
    System.arraycopy(otherChars, otherOffset, mChars, mLength+mOffset, otherLength);
    mLength = newLen;
  }

  @Override
  public String toString() {
    return new String(mChars, mOffset, mLength);
  }

  public int length() {
    return mLength;
  }

  public char charAt(int index) {
    // NOTE: must do a real check here to meet the specs of CharSequence
    if (index < 0 || index >= mLength) {
      throw new IndexOutOfBoundsException();
    }
    return mChars[mOffset + index];
  }

  public CharSequence subSequence(int start, int end) {
    // NOTE: must do a real check here to meet the specs of CharSequence
    if (start < 0 || end > mLength || start > end) {
      throw new IndexOutOfBoundsException();
    }
    return new CharsRef(mChars, mOffset + start, mOffset + end);
  }
  
  /** @deprecated */
  @Deprecated
  public final static Comparator<CharsRef> utf16SortedAsUTF8SortOrder = 
  		new UTF16SortedAsUTF8Comparator();
  
  /** @deprecated This comparator is only a transition mechanism */
  @Deprecated
  public static Comparator<CharsRef> getUTF16SortedAsUTF8Comparator() {
    return utf16SortedAsUTF8SortOrder;
  }
  
  /** @deprecated */
  @Deprecated
  private static class UTF16SortedAsUTF8Comparator implements Comparator<CharsRef> {
    // Only singleton
    private UTF16SortedAsUTF8Comparator() {};

    public int compare(CharsRef a, CharsRef b) {
      if (a == b)
        return 0;

      final char[] aChars = a.mChars;
      int aUpto = a.mOffset;
      final char[] bChars = b.mChars;
      int bUpto = b.mOffset;

      final int aStop = aUpto + Math.min(a.mLength, b.mLength);

      while (aUpto < aStop) {
        char aChar = aChars[aUpto++];
        char bChar = bChars[bUpto++];
        if (aChar != bChar) {
          // http://icu-project.org/docs/papers/utf16_code_point_order.html
          
          /* aChar != bChar, fix up each one if they're both in or above the surrogate range, then compare them */
          if (aChar >= 0xd800 && bChar >= 0xd800) {
            if (aChar >= 0xe000) {
              aChar -= 0x800;
            } else {
              aChar += 0x2000;
            }
            
            if (bChar >= 0xe000) {
              bChar -= 0x800;
            } else {
              bChar += 0x2000;
            }
          }
          
          /* now aChar and bChar are in code point order */
          return (int)aChar - (int)bChar; /* int must be 32 bits wide */
        }
      }

      // One is a prefix of the other, or, they are equal:
      return a.mLength - b.mLength;
    }
  }
  
  /**
   * Creates a new CharsRef that points to a copy of the chars from 
   * <code>other</code>
   * <p>
   * The returned CharsRef will have a length of other.length
   * and an offset of zero.
   */
  public static CharsRef deepCopyOf(CharsRef other) {
    CharsRef clone = new CharsRef();
    clone.copyChars(other);
    return clone;
  }
  
}