package org.javenstudio.falcon.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.util.StringUtils;

public class IdentityUtils {

	public static String newKey(String name, int len) throws ErrorException { 
		return IdentityUtils.toIdentity(IdentityUtils.toKeyString(name), len);
	}
	
	public static String newChecksumKey(String name, int len) throws ErrorException { 
		return IdentityUtils.toChecksumIdentity(name, IdentityUtils.toKeyString(name), len);
	}
	
	private static final char[] CHECKSUM_CHARS = new char[] { 
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
			'a', 'b', 'c', 'd', 'e', 'f', 'g'
		};
	
	public static String toChecksumIdentity(String name, String val, int len) { 
		String id = toIdentity(val, len-1);
		char checksum = '0';
		if (name != null && name.length() > 0) { 
			long key = makeLong(name, 0);
			if (key < 0) key *= -1;
			
			int num = (int)(key % CHECKSUM_CHARS.length);
			if (num < 0) num *= -1;
			if (num < 0) num = 0;
			if (num > CHECKSUM_CHARS.length-1) 
				num = CHECKSUM_CHARS.length-1;
			
			checksum = CHECKSUM_CHARS[num];
		}
		return checksum + id;
	}
	
	public static String toIdentity(String val, int len) { 
		if (len <= 0) return "";
		
		if (val == null) val = "";
		if (val.length() == len) return val;
		
		if (val.length() < len) { 
			StringBuilder sbuf = new StringBuilder();
			sbuf.append(val);
			while (sbuf.length() < len) { 
				sbuf.append('0');
			}
			val = sbuf.toString();
		} else { 
			val = val.substring(0, len);
		}
		
		return val;
	}
	
	public static long makeLong(String path, int type) { 
        byte[] key = makeKey(path, type);
        long cacheKey = Utils.crc64Long(key);
        return cacheKey;
	}
	
	public static byte[] makeKey(String path, int type) {
        return getBytes(path + "+" + type);
    }

    public static byte[] getBytes(String in) {
        byte[] result = new byte[in.length() * 2];
        int output = 0;
        for (char ch : in.toCharArray()) {
            result[output++] = (byte) (ch & 0xFF);
            result[output++] = (byte) (ch >> 8);
        }
        return result;
    }
	
    /**
     * All possible chars for representing a number as a String
     */
    final static char[] sDigits = {
        '0' , '1' , '2' , '3' , '4' , '5' ,
        '6' , '7' , '8' , '9' , 'a' , 'b' ,
        'c' , 'd' , 'e' , 'f' , 'g' , 'h' ,
        'i' , 'j' , 'k' , 'l' , 'm' , 'n' ,
        'o' , 'p' , 'q' , 'r' , 's' , 't' ,
        'u' , 'v' , 'w' , 'x' , 'y' , 'z'
    };
    
    /**
     * Convert the integer to an unsigned number.
     */
    public static String toUnsignedString(int i, int shift) {
        char[] buf = new char[32];
        int charPos = 32;
        int radix = 1 << shift;
        int mask = radix - 1;
        do {
            buf[--charPos] = sDigits[i & mask];
            i >>>= shift;
        } while (i != 0);

        return new String(buf, charPos, (32 - charPos));
    }
    
    // 16 chars
    public static String toKeyString(String val) throws ErrorException {
		try {
			if (val != null) {
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.update(val.getBytes());
				byte[] bytes = md5.digest();
				if (bytes != null) {
					StringBuilder sbuf = new StringBuilder();
					for (int i=0; i < bytes.length; i++) { 
					    int bb = bytes[i];
					    if (bb < 0) bb += 256;
					    int num = bb % 36;
					    if (num < 0) num *= -1;
					    if (num > 35) num = 35;
					    sbuf.append(sDigits[num]);
					}
					return sbuf.toString();
				}
			}
		} catch (Throwable e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		return "0000000000000000";
	}
    
    public static String toHexString(int i) {
        return toUnsignedString(i, 4);
    }
    
    public static String toOctalString(int i) {
        return toUnsignedString(i, 3);
    }
    
    public static String toBinaryString(int i) {
        return toUnsignedString(i, 1);
    }
    
	public static String toMD5(String val) throws ErrorException {
		if (val == null) return null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(val.getBytes());
			byte[] m = md5.digest();
			if (m == null) 
				return null;
			
			return StringUtils.byteToHexString(m);
		} catch (NoSuchAlgorithmException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public static String toSHA256(String val) throws ErrorException {
		if (val == null) return null;
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(val.getBytes());
			byte[] m = sha.digest();
			if (m == null) 
				return null;
			
			return StringUtils.byteToHexString(m);
		} catch (NoSuchAlgorithmException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
}
