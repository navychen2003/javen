package org.javenstudio.falcon.datum.table.store;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for MD5
 * MD5 hash produces a 128-bit digest.
 */
public class MD5Hash {
  //private static final Logger LOG = Logger.getLogger(MD5Hash.class);

  /**
   * Given a byte array, returns in MD5 hash as a hex string.
   * @param key
   * @return SHA1 hash as a 32 character hex string.
   */
  public static String getMD5AsHex(byte[] key) {
    return getMD5AsHex(key, 0, key.length);
  }
  
  /**
   * Given a byte array, returns its MD5 hash as a hex string.
   * Only "length" number of bytes starting at "offset" within the
   * byte array are used.
   *
   * @param key the key to hash (variable length byte array)
   * @param offset
   * @param length 
   * @return MD5 hash as a 32 character hex string.
   */
  public static String getMD5AsHex(byte[] key, int offset, int length) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(key, offset, length);
      byte[] digest = md.digest();
      return new String(Hex.encodeHex(digest));
    } catch (NoSuchAlgorithmException e) {
      // this should never happen unless the JDK is messed up.
      throw new RuntimeException("Error computing MD5 hash", e);
    }
  }
}
