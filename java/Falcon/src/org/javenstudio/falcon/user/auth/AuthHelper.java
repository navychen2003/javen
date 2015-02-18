package org.javenstudio.falcon.user.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.mime.DecoderUtil;

public final class AuthHelper {

	private static final char[] sHashChars = {
			'0','1','2','3','4','5','6','7','8','9',
			'a','b','c','d','e','f','g',
			'h','i','j','k','l','m','n',
			'o','p','q','r','s','t',
			'u','v','w','x','y','z'
		};
	
	static String getStoreDir(String name) { 
		if (name == null) name = "";
		
		int hash1 = createStoreHash(getHashName1(name));
		int hash2 = createStoreHash(getHashName2(name));
		int hash3 = createStoreHash(getHashName3(name));
		
		char chr1 = sHashChars[hash1 % sHashChars.length];
		char chr2 = sHashChars[hash2 % sHashChars.length];
		char chr3 = sHashChars[hash3 % sHashChars.length];
		
		return Character.toString(chr1) + '/' 
				+ Character.toString(chr2) + '/' 
				+ Character.toString(chr3);
	}
	
	static String getHashName1(String name) {
		if (name == null) name = "";
		return name;
	}
	
	static String getHashName2(String name) {
		if (name == null) name = "";
		if (name.length() > 1) name = name.substring(0, name.length()/2);
		return name;
	}
	
	static String getHashName3(String name) {
		if (name == null) name = "";
		if (name.length() > 1) name = name.substring(name.length()/2);
		return name;
	}
	
	static int createStoreHash(String name) {
		if (name == null) name = "";
		
		int hash = 0;
		for (int i=0; i < name.length(); i++) { 
			char chr = name.charAt(i);
			hash = 31 * hash + chr;
		}
		
		if (hash < 0) hash *= (-1);
		return hash;
	}
	
	static byte[] makeUserData(byte flag, byte type, 
			String username, String userkey, String password) 
			throws ErrorException { 
		byte[] md5 = encodePwd(password);
		byte[] buffer = new byte[md5 != null ? md5.length+1 : 1];
		buffer[0] = (byte)flag;
		if (md5 != null && md5.length > 0)
			System.arraycopy(md5, 0, buffer, 1, md5.length);
		return buffer;
	}
	
	static byte[] encodePwd(String val) throws ErrorException {
		if (val == null) return null;
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(val.getBytes());
			byte[] m = md5.digest();
			return m;
		} catch (NoSuchAlgorithmException e) { 
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
	}
	
	public static String decodeSecret(String val) throws ErrorException {
		if (val == null || val.length() == 0)
			return val;
		
		try {
			byte[] buffer = DecoderUtil.decodeBase64(val);
			if (buffer != null && buffer.length > 0) 
				return new String(buffer, "US-ASCII");
		} catch (Throwable e) {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, e);
		}
		
		return "";
	}
	
}
