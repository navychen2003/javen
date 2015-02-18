package org.javenstudio.common.indexdb.util;

import java.util.Comparator;

/** 
 * Represents byte[], as a slice (offset + length) into an
 *  existing byte[].  The {@link #bytes} member should never be null;
 *  use {@link #EMPTY_BYTES} if necessary.
 *
 * <p><b>Important note:</b> Unless otherwise noted, Indexdb uses this class to
 * represent terms that are encoded as <b>UTF8</b> bytes in the index. To
 * convert them to a Java {@link String} (which is UTF16), use {@link #utf8ToString}.
 * Using code like {@code new String(bytes, offset, length)} to do this
 * is <b>wrong</b>, as it does not respect the correct character set
 * and may return wrong results (depending on the platform's defaults)!
 * 
 */
public final class BytesRef implements Comparable<BytesRef>,Cloneable {
  /** An empty byte array for convenience */
  public static final byte[] EMPTY_BYTES = new byte[0]; 

  /** The contents of the BytesRef. Should never be {@code null}. */
  public byte[] mBytes;

  /** Offset of first valid byte. */
  public int mOffset;

  /** Length of used bytes. */
  public int mLength;

  /** Create a BytesRef with {@link #EMPTY_BYTES} */
  public BytesRef() {
    this(EMPTY_BYTES);
  }

  /** 
   * This instance will directly reference bytes w/o making a copy.
   * bytes should not be null.
   */
  public BytesRef(byte[] bytes, int offset, int length) {
    assert bytes != null;
    assert offset >= 0;
    assert length >= 0;
    assert bytes.length >= offset + length;
    mBytes = bytes;
    mOffset = offset;
    mLength = length;
  }

  /** 
   * This instance will directly reference bytes w/o making a copy.
   * bytes should not be null 
   */
  public BytesRef(byte[] bytes) {
    this(bytes, 0, bytes.length);
  }

  /** 
   * Create a BytesRef pointing to a new array of size <code>capacity</code>.
   * Offset and length will both be zero.
   */
  public BytesRef(int capacity) {
    mBytes = new byte[capacity];
  }

  /**
   * Initialize the byte[] from the UTF8 bytes
   * for the provided String.  
   * 
   * @param text This must be well-formed
   * unicode text, with no unpaired surrogates.
   */
  public BytesRef(CharSequence text) {
    this();
    copyChars(text);
  }

  /**
   * Copies the UTF8 bytes for this string.
   * 
   * @param text Must be well-formed unicode text, with no
   * unpaired surrogates or invalid UTF16 code units.
   */
  public void copyChars(CharSequence text) {
    assert mOffset == 0;   // TODO broken if offset != 0
    UnicodeUtil.UTF16toUTF8(text, 0, text.length(), this);
  }
  
  /**
   * Expert: compares the bytes against another BytesRef,
   * returning true if the bytes are equal.
   * 
   * @param other Another BytesRef, should not be null.
   */
  public boolean bytesEquals(BytesRef other) {
    assert other != null;
    if (mLength == other.mLength) {
      int otherUpto = other.mOffset;
      final byte[] otherBytes = other.mBytes;
      final int end = mOffset + mLength;
      for (int upto=mOffset; upto < end; upto++,otherUpto++) {
        if (mBytes[upto] != otherBytes[otherUpto]) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  public final byte[] getBytes() { return mBytes; }
  public final byte getByteAt(int pos) { return mBytes[pos]; }
  public final int getOffset() { return mOffset; }
  public final int getLength() { return mLength; }
  
  public final void setLength(int len) { 
	  if (len < 0) throw new IllegalArgumentException("length: "+len+" input error");
	  mLength = len;
  }
  
  @Override
  public BytesRef clone() {
    return new BytesRef(mBytes, mOffset, mLength);
  }

  /** 
   * Calculates the hash code as required by TermsHash during indexing.
   * <p>It is defined as:
   * <pre>
   *  int hash = 0;
   *  for (int i = offset; i &lt; offset + length; i++) {
   *    hash = 31*hash + bytes[i];
   *  }
   * </pre>
   */
  @Override
  public int hashCode() {
    int hash = 0;
    final int end = mOffset + mLength;
    for (int i=mOffset; i < end; i++) {
      hash = 31 * hash + mBytes[i];
    }
    return hash;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other instanceof BytesRef) {
      return this.bytesEquals((BytesRef) other);
    }
    return false;
  }

  /** 
   * Interprets stored bytes as UTF8 bytes, returning the
   *  resulting string 
   */
  public String utf8ToString() {
    final CharsRef ref = new CharsRef(mLength);
    UnicodeUtil.UTF8toUTF16(mBytes, mOffset, mLength, ref);
    return ref.toString(); 
  }

  /** Returns hex encoded bytes, eg [0x6c 0x75 0x63 0x65 0x6e 0x65] */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    final int end = mOffset + mLength;
    for (int i=mOffset; i < end; i++) {
      if (i > mOffset) {
        sb.append(' ');
      }
      sb.append(Integer.toHexString(mBytes[i]&0xff));
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Copies the bytes from the given {@link BytesRef}
   * <p>
   * NOTE: if this would exceed the array size, this method creates a 
   * new reference array.
   */
  public void copyBytes(BytesRef other) {
    if (mBytes.length - mOffset < other.mLength) {
      mBytes = new byte[other.mLength];
      mOffset = 0;
    }
    System.arraycopy(other.mBytes, other.mOffset, mBytes, mOffset, other.mLength);
    mLength = other.mLength;
  }

  /**
   * Appends the bytes from the given {@link BytesRef}
   * <p>
   * NOTE: if this would exceed the array size, this method creates a 
   * new reference array.
   */
  public void append(BytesRef other) {
    int newLen = mLength + other.mLength;
    if (mBytes.length - mOffset < newLen) {
      byte[] newBytes = new byte[newLen];
      System.arraycopy(mBytes, mOffset, newBytes, 0, mLength);
      mOffset = 0;
      mBytes = newBytes;
    }
    System.arraycopy(other.mBytes, other.mOffset, mBytes, mLength+mOffset, other.mLength);
    mLength = newLen;
  }

  /** 
   * Used to grow the reference array. 
   * 
   * In general this should not be used as it does not take the offset into account.
   */
  public void grow(int newLength) {
    assert mOffset == 0; // NOTE: senseless if offset != 0
    mBytes = ArrayUtil.grow(mBytes, newLength);
  }

  /** Unsigned byte order comparison */
  public int compareTo(BytesRef other) {
    return utf8SortedAsUnicodeSortOrder.compare(this, other);
  }
  
  private final static Comparator<BytesRef> utf8SortedAsUnicodeSortOrder = new UTF8SortedAsUnicodeComparator();

  public static Comparator<BytesRef> getUTF8SortedAsUnicodeComparator() {
    return utf8SortedAsUnicodeSortOrder;
  }

  private static class UTF8SortedAsUnicodeComparator implements Comparator<BytesRef> {
    // Only singleton
    private UTF8SortedAsUnicodeComparator() {};

    public int compare(BytesRef a, BytesRef b) {
      final byte[] aBytes = a.mBytes;
      int aUpto = a.mOffset;
      final byte[] bBytes = b.mBytes;
      int bUpto = b.mOffset;
      
      final int aStop = aUpto + Math.min(a.mLength, b.mLength);
      while (aUpto < aStop) {
        int aByte = aBytes[aUpto++] & 0xff;
        int bByte = bBytes[bUpto++] & 0xff;

        int diff = aByte - bByte;
        if (diff != 0) {
          return diff;
        }
      }

      // One is a prefix of the other, or, they are equal:
      return a.mLength - b.mLength;
    }    
  }

  /** @deprecated */
  @Deprecated
  private final static Comparator<BytesRef> utf8SortedAsUTF16SortOrder = 
  		new UTF8SortedAsUTF16Comparator();

  /** @deprecated This comparator is only a transition mechanism */
  @SuppressWarnings("unused")
  @Deprecated
  private static Comparator<BytesRef> getUTF8SortedAsUTF16Comparator() {
    return utf8SortedAsUTF16SortOrder;
  }

  /** @deprecated */
  @Deprecated
  private static class UTF8SortedAsUTF16Comparator implements Comparator<BytesRef> {
    // Only singleton
    private UTF8SortedAsUTF16Comparator() {};

    public int compare(BytesRef a, BytesRef b) {
      final byte[] aBytes = a.mBytes;
      int aUpto = a.mOffset;
      final byte[] bBytes = b.mBytes;
      int bUpto = b.mOffset;
      
      final int aStop;
      if (a.mLength < b.mLength) {
        aStop = aUpto + a.mLength;
      } else {
        aStop = aUpto + b.mLength;
      }

      while (aUpto < aStop) {
        int aByte = aBytes[aUpto++] & 0xff;
        int bByte = bBytes[bUpto++] & 0xff;

        if (aByte != bByte) {
          // See http://icu-project.org/docs/papers/utf16_code_point_order.html#utf-8-in-utf-16-order

          // We know the terms are not equal, but, we may
          // have to carefully fixup the bytes at the
          // difference to match UTF16's sort order:
          
          // NOTE: instead of moving supplementary code points (0xee and 0xef) to the unused 0xfe and 0xff, 
          // we move them to the unused 0xfc and 0xfd [reserved for future 6-byte character sequences]
          // this reserves 0xff for preflex's term reordering (surrogate dance), and if unicode grows such
          // that 6-byte sequences are needed we have much bigger problems anyway.
          if (aByte >= 0xee && bByte >= 0xee) {
            if ((aByte & 0xfe) == 0xee) {
              aByte += 0xe;
            }
            if ((bByte&0xfe) == 0xee) {
              bByte += 0xe;
            }
          }
          return aByte - bByte;
        }
      }

      // One is a prefix of the other, or, they are equal:
      return a.mLength - b.mLength;
    }
  }
  
  /**
   * Creates a new BytesRef that points to a copy of the bytes from 
   * <code>other</code>
   * <p>
   * The returned BytesRef will have a length of other.length
   * and an offset of zero.
   */
  public static BytesRef deepCopyOf(BytesRef other) {
    BytesRef copy = new BytesRef();
    copy.copyBytes(other);
    return copy;
  }
  
}
