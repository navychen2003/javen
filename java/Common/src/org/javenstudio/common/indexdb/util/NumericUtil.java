package org.javenstudio.common.indexdb.util;

/**
 * This is a helper class to generate prefix-encoded representations for numerical values
 * and supplies converters to represent float/double values as sortable integers/longs.
 *
 * <p>To quickly execute range queries in Indexdb, a range is divided recursively
 * into multiple intervals for searching: The center of the range is searched only with
 * the lowest possible precision in the trie, while the boundaries are matched
 * more exactly. This reduces the number of terms dramatically.
 *
 * <p>This class generates terms to achieve this: First the numerical integer values need to
 * be converted to bytes. For that integer values (32 bit or 64 bit) are made unsigned
 * and the bits are converted to ASCII chars with each 7 bit. The resulting byte[] is
 * sortable like the original integer value (even using UTF-8 sort order). Each value is also
 * prefixed (in the first char) by the <code>shift</code> value (number of bits removed) used
 * during encoding.
 *
 * <p>To also index floating point numbers, this class supplies two methods to convert them
 * to integer values by changing their bit layout: {@link #doubleToSortableLong},
 * {@link #floatToSortableInt}. You will have no precision loss by
 * converting floating point numbers to integers and back (only that the integer form
 * is not usable). Other data types like dates can easily converted to longs or ints (e.g.
 * date to long: {@link java.util.Date#getTime}).
 *
 * <p>For easy usage, the trie algorithm is implemented for indexing inside
 * {@link NumericTokenStream} that can index <code>int</code>, <code>long</code>,
 * <code>float</code>, and <code>double</code>. For querying,
 * {@link NumericRangeQuery} and {@link NumericRangeFilter} implement the query part
 * for the same data types.
 *
 * <p>This class can also be used, to generate lexicographically sortable (according to
 * {@link BytesRef#getUTF8SortedAsUTF16Comparator()}) representations of numeric data
 * types for other usages (e.g. sorting).
 *
 */
public final class NumericUtil {
  private NumericUtil() {} // no instance!
  
  /**
   * The default precision step used by {@link IntField},
   * {@link FloatField}, {@link LongField}, {@link
   * DoubleField}, {@link NumericTokenStream}, {@link
   * NumericRangeQuery}, and {@link NumericRangeFilter}.
   */
  public static final int PRECISION_STEP_DEFAULT = 4;
  
  /**
   * Longs are stored at lower precision by shifting off lower bits. The shift count is
   * stored as <code>SHIFT_START_LONG+shift</code> in the first byte
   */
  public static final byte SHIFT_START_LONG = 0x20;

  /**
   * The maximum term length (used for <code>byte[]</code> buffer size)
   * for encoding <code>long</code> values.
   * @see #longToPrefixCoded(long,int,BytesRef)
   */
  public static final int BUF_SIZE_LONG = 63/7 + 2;

  /**
   * Integers are stored at lower precision by shifting off lower bits. The shift count is
   * stored as <code>SHIFT_START_INT+shift</code> in the first byte
   */
  public static final byte SHIFT_START_INT  = 0x60;

  /**
   * The maximum term length (used for <code>byte[]</code> buffer size)
   * for encoding <code>int</code> values.
   * @see #intToPrefixCoded(int,int,BytesRef)
   */
  public static final int BUF_SIZE_INT = 31/7 + 2;

  /**
   * Returns prefix coded bits after reducing the precision by <code>shift</code> bits.
   * This is method is used by {@link NumericTokenStream}.
   * After encoding, {@code bytes.offset} will always be 0. 
   * @param val the numeric value
   * @param shift how many bits to strip from the right
   * @param bytes will contain the encoded value
   * @return the hash code for indexing (TermsHash)
   */
  public static int longToPrefixCoded(final long val, final int shift, final BytesRef bytes) {
    if (shift>63 || shift<0)
      throw new IllegalArgumentException("Illegal shift value, must be 0..63");
    int hash, nChars = (63-shift)/7 + 1;
    bytes.mOffset = 0;
    bytes.mLength = nChars+1;
    if (bytes.mBytes.length < bytes.mLength) {
      bytes.grow(NumericUtil.BUF_SIZE_LONG);
    }
    bytes.mBytes[0] = (byte) (hash = (SHIFT_START_LONG + shift));
    long sortableBits = val ^ 0x8000000000000000L;
    sortableBits >>>= shift;
    while (nChars > 0) {
      // Store 7 bits per byte for compatibility
      // with UTF-8 encoding of terms
      bytes.mBytes[nChars--] = (byte)(sortableBits & 0x7f);
      sortableBits >>>= 7;
    }
    // calculate hash
    for (int i = 1; i < bytes.mLength; i++) {
      hash = 31*hash + bytes.mBytes[i];
    }
    return hash;
  }

  /**
   * Returns prefix coded bits after reducing the precision by <code>shift</code> bits.
   * This is method is used by {@link NumericTokenStream}.
   * After encoding, {@code bytes.offset} will always be 0. 
   * @param val the numeric value
   * @param shift how many bits to strip from the right
   * @param bytes will contain the encoded value
   * @return the hash code for indexing (TermsHash)
   */
  public static int intToPrefixCoded(final int val, final int shift, final BytesRef bytes) {
    if (shift>31 || shift<0)
      throw new IllegalArgumentException("Illegal shift value, must be 0..31");
    int hash, nChars = (31-shift)/7 + 1;
    bytes.mOffset = 0;
    bytes.mLength = nChars+1;
    if (bytes.mBytes.length < bytes.mLength) {
      bytes.grow(NumericUtil.BUF_SIZE_INT);
    }
    bytes.mBytes[0] = (byte) (hash = (SHIFT_START_INT + shift));
    int sortableBits = val ^ 0x80000000;
    sortableBits >>>= shift;
    while (nChars > 0) {
      // Store 7 bits per byte for compatibility
      // with UTF-8 encoding of terms
      bytes.mBytes[nChars--] = (byte)(sortableBits & 0x7f);
      sortableBits >>>= 7;
    }
    // calculate hash
    for (int i = 1; i < bytes.mLength; i++) {
      hash = 31*hash + bytes.mBytes[i];
    }
    return hash;
  }

  /**
   * Returns the shift value from a prefix encoded {@code long}.
   * @throws NumberFormatException if the supplied {@link BytesRef} is
   * not correctly prefix encoded.
   */
  public static int getPrefixCodedLongShift(final BytesRef val) {
    final int shift = val.mBytes[val.mOffset] - SHIFT_START_LONG;
    if (shift > 63 || shift < 0)
      throw new NumberFormatException("Invalid shift value (" + shift + ") in prefixCoded bytes (is encoded value really an INT?)");
    return shift;
  }

  /**
   * Returns the shift value from a prefix encoded {@code int}.
   * @throws NumberFormatException if the supplied {@link BytesRef} is
   * not correctly prefix encoded.
   */
  public static int getPrefixCodedIntShift(final BytesRef val) {
    final int shift = val.mBytes[val.mOffset] - SHIFT_START_INT;
    if (shift > 31 || shift < 0)
      throw new NumberFormatException("Invalid shift value in prefixCoded bytes (is encoded value really an INT?)");
    return shift;
  }

  /**
   * Returns a long from prefixCoded bytes.
   * Rightmost bits will be zero for lower precision codes.
   * This method can be used to decode a term's value.
   * @throws NumberFormatException if the supplied {@link BytesRef} is
   * not correctly prefix encoded.
   * @see #longToPrefixCoded(long,int,BytesRef)
   */
  public static long prefixCodedToLong(final BytesRef val) {
    long sortableBits = 0L;
    for (int i=val.mOffset+1, limit=val.mOffset+val.mLength; i<limit; i++) {
      sortableBits <<= 7;
      final byte b = val.mBytes[i];
      if (b < 0) {
        throw new NumberFormatException(
          "Invalid prefixCoded numerical value representation (byte "+
          Integer.toHexString(b&0xff)+" at position "+(i-val.mOffset)+" is invalid)"
        );
      }
      sortableBits |= b;
    }
    return (sortableBits << getPrefixCodedLongShift(val)) ^ 0x8000000000000000L;
  }

  /**
   * Returns an int from prefixCoded bytes.
   * Rightmost bits will be zero for lower precision codes.
   * This method can be used to decode a term's value.
   * @throws NumberFormatException if the supplied {@link BytesRef} is
   * not correctly prefix encoded.
   * @see #intToPrefixCoded(int,int,BytesRef)
   */
  public static int prefixCodedToInt(final BytesRef val) {
    int sortableBits = 0;
    for (int i=val.mOffset+1, limit=val.mOffset+val.mLength; i<limit; i++) {
      sortableBits <<= 7;
      final byte b = val.mBytes[i];
      if (b < 0) {
        throw new NumberFormatException(
          "Invalid prefixCoded numerical value representation (byte "+
          Integer.toHexString(b&0xff)+" at position "+(i-val.mOffset)+" is invalid)"
        );
      }
      sortableBits |= b;
    }
    return (sortableBits << getPrefixCodedIntShift(val)) ^ 0x80000000;
  }

  /**
   * Converts a <code>double</code> value to a sortable signed <code>long</code>.
   * The value is converted by getting their IEEE 754 floating-point &quot;double format&quot;
   * bit layout and then some bits are swapped, to be able to compare the result as long.
   * By this the precision is not reduced, but the value can easily used as a long.
   * The sort order (including {@link Double#NaN}) is defined by
   * {@link Double#compareTo}; {@code NaN} is greater than positive infinity.
   * @see #sortableLongToDouble
   */
  public static long doubleToSortableLong(double val) {
    long f = Double.doubleToLongBits(val);
    if (f<0) f ^= 0x7fffffffffffffffL;
    return f;
  }

  /**
   * Converts a sortable <code>long</code> back to a <code>double</code>.
   * @see #doubleToSortableLong
   */
  public static double sortableLongToDouble(long val) {
    if (val<0) val ^= 0x7fffffffffffffffL;
    return Double.longBitsToDouble(val);
  }

  /**
   * Converts a <code>float</code> value to a sortable signed <code>int</code>.
   * The value is converted by getting their IEEE 754 floating-point &quot;float format&quot;
   * bit layout and then some bits are swapped, to be able to compare the result as int.
   * By this the precision is not reduced, but the value can easily used as an int.
   * The sort order (including {@link Float#NaN}) is defined by
   * {@link Float#compareTo}; {@code NaN} is greater than positive infinity.
   * @see #sortableIntToFloat
   */
  public static int floatToSortableInt(float val) {
    int f = Float.floatToIntBits(val);
    if (f<0) f ^= 0x7fffffff;
    return f;
  }

  /**
   * Converts a sortable <code>int</code> back to a <code>float</code>.
   * @see #floatToSortableInt
   */
  public static float sortableIntToFloat(int val) {
    if (val<0) val ^= 0x7fffffff;
    return Float.intBitsToFloat(val);
  }

  /**
   * Splits a long range recursively.
   * You may implement a builder that adds clauses to a
   * {@link BooleanQuery} for each call to its
   * {@link LongRangeBuilder#addRange(BytesRef,BytesRef)}
   * method.
   * <p>This method is used by {@link NumericRangeQuery}.
   */
  public static void splitLongRange(final LongRangeBuilder builder,
    final int precisionStep,  final long minBound, final long maxBound
  ) {
    splitRange(builder, 64, precisionStep, minBound, maxBound);
  }
  
  /**
   * Splits an int range recursively.
   * You may implement a builder that adds clauses to a
   * {@link BooleanQuery} for each call to its
   * {@link IntRangeBuilder#addRange(BytesRef,BytesRef)}
   * method.
   * <p>This method is used by {@link NumericRangeQuery}.
   */
  public static void splitIntRange(final IntRangeBuilder builder,
    final int precisionStep,  final int minBound, final int maxBound
  ) {
    splitRange(builder, 32, precisionStep, minBound, maxBound);
  }
  
  /** This helper does the splitting for both 32 and 64 bit. */
  private static void splitRange(
    final Object builder, final int valSize,
    final int precisionStep, long minBound, long maxBound
  ) {
    if (precisionStep < 1)
      throw new IllegalArgumentException("precisionStep must be >=1");
    if (minBound > maxBound) return;
    for (int shift=0; ; shift += precisionStep) {
      // calculate new bounds for inner precision
      final long diff = 1L << (shift+precisionStep),
        mask = ((1L<<precisionStep) - 1L) << shift;
      final boolean
        hasLower = (minBound & mask) != 0L,
        hasUpper = (maxBound & mask) != mask;
      final long
        nextMinBound = (hasLower ? (minBound + diff) : minBound) & ~mask,
        nextMaxBound = (hasUpper ? (maxBound - diff) : maxBound) & ~mask;
      final boolean
        lowerWrapped = nextMinBound < minBound,
        upperWrapped = nextMaxBound > maxBound;
      
      if (shift+precisionStep>=valSize || nextMinBound>nextMaxBound || lowerWrapped || upperWrapped) {
        // We are in the lowest precision or the next precision is not available.
        addRange(builder, valSize, minBound, maxBound, shift);
        // exit the split recursion loop
        break;
      }
      
      if (hasLower)
        addRange(builder, valSize, minBound, minBound | mask, shift);
      if (hasUpper)
        addRange(builder, valSize, maxBound & ~mask, maxBound, shift);
      
      // recurse to next precision
      minBound = nextMinBound;
      maxBound = nextMaxBound;
    }
  }
  
  /** Helper that delegates to correct range builder */
  private static void addRange(
    final Object builder, final int valSize,
    long minBound, long maxBound,
    final int shift
  ) {
    // for the max bound set all lower bits (that were shifted away):
    // this is important for testing or other usages of the splitted range
    // (e.g. to reconstruct the full range). The prefixEncoding will remove
    // the bits anyway, so they do not hurt!
    maxBound |= (1L << shift) - 1L;
    // delegate to correct range builder
    switch(valSize) {
      case 64:
        ((LongRangeBuilder)builder).addRange(minBound, maxBound, shift);
        break;
      case 32:
        ((IntRangeBuilder)builder).addRange((int)minBound, (int)maxBound, shift);
        break;
      default:
        // Should not happen!
        throw new IllegalArgumentException("valSize must be 32 or 64.");
    }
  }

  /**
   * Callback for {@link #splitLongRange}.
   * You need to overwrite only one of the methods.
   */
  public static abstract class LongRangeBuilder {
    
    /**
     * Overwrite this method, if you like to receive the already prefix encoded range bounds.
     * You can directly build classical (inclusive) range queries from them.
     */
    public void addRange(BytesRef minPrefixCoded, BytesRef maxPrefixCoded) {
      throw new UnsupportedOperationException();
    }
    
    /**
     * Overwrite this method, if you like to receive the raw long range bounds.
     * You can use this for e.g. debugging purposes (print out range bounds).
     */
    public void addRange(final long min, final long max, final int shift) {
      final BytesRef minBytes = new BytesRef(BUF_SIZE_LONG), maxBytes = new BytesRef(BUF_SIZE_LONG);
      longToPrefixCoded(min, shift, minBytes);
      longToPrefixCoded(max, shift, maxBytes);
      addRange(minBytes, maxBytes);
    }
  
  }
  
  /**
   * Callback for {@link #splitIntRange}.
   * You need to overwrite only one of the methods.
   */
  public static abstract class IntRangeBuilder {
    
    /**
     * Overwrite this method, if you like to receive the already prefix encoded range bounds.
     * You can directly build classical range (inclusive) queries from them.
     */
    public void addRange(BytesRef minPrefixCoded, BytesRef maxPrefixCoded) {
      throw new UnsupportedOperationException();
    }
    
    /**
     * Overwrite this method, if you like to receive the raw int range bounds.
     * You can use this for e.g. debugging purposes (print out range bounds).
     */
    public void addRange(final int min, final int max, final int shift) {
      final BytesRef minBytes = new BytesRef(BUF_SIZE_INT), maxBytes = new BytesRef(BUF_SIZE_INT);
      intToPrefixCoded(min, shift, minBytes);
      intToPrefixCoded(max, shift, maxBytes);
      addRange(minBytes, maxBytes);
    }
  
  }
  
}
