package org.javenstudio.common.indexdb.util;

import java.util.Comparator;
import java.util.StringTokenizer;

import org.javenstudio.util.StringUtils;

/**
 * Methods for manipulating strings.
 *
 */
public abstract class StringHelper {
  private StringHelper() {}
	
  /**
   * Compares two {@link BytesRef}, element by element, and returns the
   * number of elements common to both arrays.
   *
   * @param left The first {@link BytesRef} to compare
   * @param right The second {@link BytesRef} to compare
   * @return The number of common elements.
   */
  public static int bytesDifference(BytesRef left, BytesRef right) {
    int len = left.getLength() < right.getLength() ? left.getLength() : right.getLength();
    final byte[] bytesLeft = left.getBytes();
    final int offLeft = left.getOffset();
    byte[] bytesRight = right.getBytes();
    final int offRight = right.getOffset();
    for (int i = 0; i < len; i++)
      if (bytesLeft[i+offLeft] != bytesRight[i+offRight])
        return i;
    return len;
  }

  /**
   * @return a Comparator over versioned strings such as X.YY.Z
   */
  public static Comparator<String> getVersionComparator() {
    return versionComparator;
  }
  
  private static Comparator<String> versionComparator = new Comparator<String>() {
    public int compare(String a, String b) {
      StringTokenizer aTokens = new StringTokenizer(a, ".");
      StringTokenizer bTokens = new StringTokenizer(b, ".");
      
      while (aTokens.hasMoreTokens()) {
        int aToken = Integer.parseInt(aTokens.nextToken());
        if (bTokens.hasMoreTokens()) {
          int bToken = Integer.parseInt(bTokens.nextToken());
          if (aToken != bToken) {
            return aToken < bToken ? -1 : 1;
          }
        } else {
          // a has some extra trailing tokens. if these are all zeroes, thats ok.
          if (aToken != 0) {
            return 1; 
          }
        }
      }
      
      // b has some extra trailing tokens. if these are all zeroes, thats ok.
      while (bTokens.hasMoreTokens()) {
        if (Integer.parseInt(bTokens.nextToken()) != 0)
          return -1;
      }
      
      return 0;
    }
  };

  public static boolean equals(String s1, String s2) {
    if (s1 == null) {
      return s2 == null;
    } else {
      return s1.equals(s2);
    }
  }

  /**
   * Returns <code>true</code> iff the ref starts with the given prefix.
   * Otherwise <code>false</code>.
   * 
   * @param ref
   *          the {@link BytesRef} to test
   * @param prefix
   *          the expected prefix
   * @return Returns <code>true</code> iff the ref starts with the given prefix.
   *         Otherwise <code>false</code>.
   */
  public static boolean startsWith(BytesRef ref, BytesRef prefix) {
    return sliceEquals(ref, prefix, 0);
  }

  /**
   * Returns <code>true</code> iff the ref ends with the given suffix. Otherwise
   * <code>false</code>.
   * 
   * @param ref
   *          the {@link BytesRef} to test
   * @param suffix
   *          the expected suffix
   * @return Returns <code>true</code> iff the ref ends with the given suffix.
   *         Otherwise <code>false</code>.
   */
  public static boolean endsWith(BytesRef ref, BytesRef suffix) {
    return sliceEquals(ref, suffix, ref.getLength() - suffix.getLength());
  }
  
  private static boolean sliceEquals(BytesRef sliceToTest, BytesRef other, int pos) {
    if (pos < 0 || sliceToTest.getLength() - pos < other.getLength()) {
      return false;
    }
    int i = sliceToTest.getOffset() + pos;
    int j = other.getOffset();
    final int k = other.getOffset() + other.getLength();
    
    while (j < k) {
      if (sliceToTest.getByteAt(i++) != other.getByteAt(j++)) {
        return false;
      }
    }
    
    return true;
  }
  
  public static String toHumanReadableUnits(long size) { 
	  return StringUtils.byteDesc(size);
  }
  
  /**
   * for printing boost only if not 1.0
   */
  public static String toBoostString(float boost) {
    if (boost != 1.0f) {
      return "^" + Float.toString(boost);
    } else return "";
  }

  public static void toByteArray(StringBuilder buffer, byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      buffer.append("b[").append(i).append("]=").append(bytes[i]);
      if (i < bytes.length - 1) {
        buffer.append(',');
      }

    }
  }
  
}
