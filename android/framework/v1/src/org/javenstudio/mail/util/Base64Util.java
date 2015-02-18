package org.javenstudio.mail.util;

import java.io.InputStream;

public class Base64Util {

	public static InputStream createBase64InputStream(InputStream is) { 
		return new Base64InputStream(is, Base64.DEFAULT);
	}
	
}
