package org.javenstudio.mail.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class Base64Util {
	//private static final Logger LOG = Logger.getLogger(Base64Util.class);

	public static InputStream createBase64InputStream(InputStream is) { 
		return new Base64InputStream(is, Base64.DEFAULT);
	}
	
	public static OutputStream createBase64OutputStream(OutputStream out) {
		return new Base64OutputStream(out, Base64.DEFAULT);
	}
	
	public static byte[] decodeBase64(String s) throws IOException {
		if (s == null) return null;
		
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            byte[] bytes = s.getBytes("US-ASCII");
            InputStream is = createBase64InputStream(new ByteArrayInputStream(bytes));
            
            int b = 0;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
        } catch (IOException e) {
            //if (LOG.isWarnEnabled())
            //	LOG.warn("decodeBase64: error: " + e, e);
        	
        	throw e;
        }
        
        return baos.toByteArray();
    }
	
	public static String encodeBase64(String str) throws IOException {
		if (str == null) return null;
		
		try {
			byte[] bytes = str.getBytes("US-ASCII");
			return encodeBase64(bytes);
			
		} catch (Throwable e) {
        	//if (LOG.isWarnEnabled())
            //	LOG.warn("encodeBase64: error: " + e, e);
			
			throw new IOException(e.toString(), e);
        }
	}
	
	public static String encodeBase64(byte[] bytes) throws IOException {
		if (bytes == null) throw new NullPointerException();
		
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            InputStream is = new ByteArrayInputStream(bytes);
            OutputStream out = createBase64OutputStream(baos);
            
            int b = 0;
            while ((b = is.read()) != -1) {
                out.write(b);
            }
            
            out.flush();
        } catch (IOException e) {
            //if (LOG.isWarnEnabled())
            //	LOG.warn("encodeBase64: error: " + e, e);
        	
        	throw e;
        }
        
        try {
        	baos.flush();
	        byte[] result = baos.toByteArray();
	        
	        if (result != null)
	        	return new String(result, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
        	//if (LOG.isWarnEnabled())
            //	LOG.warn("encodeBase64: error: " + e, e);
        	
        	throw new IOException(e.toString(), e);
        }
        
        throw new IOException("Encoded result is empty");
    }
	
	public static String encodeSecret(String str) throws IOException {
		return encodeSecret(str, "UTF-8");
	}
	
	public static String encodeSecret(String str, String charset) throws IOException {
		if (str == null) return null;
		
		try {
			byte[] bytes = str.getBytes(charset);
			byte[] result = Base64.encode(bytes, Base64.DEFAULT);
			return new String(result, charset);
			
		} catch (Throwable e) {
        	//if (LOG.isWarnEnabled())
            //	LOG.warn("encodeBase64: error: " + e, e);
			
			throw new IOException(e.toString(), e);
        }
	}
	
	public static String decodeSecret(String str) throws IOException {
		return decodeSecret(str, "UTF-8");
	}
	
	public static String decodeSecret(String str, String charset) throws IOException {
		if (str == null) return null;
		
		try {
			byte[] result = Base64.decode(str, Base64.DEFAULT);
			return new String(result, charset);
			
		} catch (Throwable e) {
        	//if (LOG.isWarnEnabled())
            //	LOG.warn("encodeBase64: error: " + e, e);
			
			throw new IOException(e.toString(), e);
        }
	}
	
}
