package org.apache.james.mime4j.util;

import java.io.InputStream;

import org.javenstudio.mail.decoder.DecoderUtil;
import org.javenstudio.mail.decoder.QuotedPrintableInputStream;
import org.javenstudio.mail.transport.LoggingInputStream;
import org.javenstudio.mail.util.Base64;
import org.javenstudio.mail.util.Base64InputStream;

public class DecodeUtil {

	public static InputStream createBase64InputStream(InputStream is) { 
		return new Base64InputStream(is, Base64.DEFAULT);
	}
	
	public static InputStream createQuotedPrintableInputStream(InputStream is) { 
		return new QuotedPrintableInputStream(is);
	}
	
	public static InputStream createLoggingInputStream(InputStream is) { 
		return new LoggingInputStream(is, "MIME", true);
	}
	
	public static String decodeEncodedWords(String text) {
		return DecoderUtil.decodeEncodedWords(text);
	}
	
}
