package org.apache.james.mime4j.util;

import java.io.InputStream;

import org.javenstudio.mime.DecoderUtil;
import org.javenstudio.mime.QuotedPrintableInputStream;
import org.javenstudio.mime.Base64;
import org.javenstudio.mime.Base64InputStream;

public class DecodeUtil {

	public static InputStream createBase64InputStream(InputStream is) { 
		return new Base64InputStream(is, Base64.DEFAULT);
	}
	
	public static InputStream createQuotedPrintableInputStream(InputStream is) { 
		return new QuotedPrintableInputStream(is);
	}
	
	public static InputStream createLoggingInputStream(InputStream is) { 
		return is; //new LoggingInputStream(is, "MIME", true);
	}
	
	public static String decodeEncodedWords(String text) {
		return DecoderUtil.decodeEncodedWords(text);
	}
	
}
