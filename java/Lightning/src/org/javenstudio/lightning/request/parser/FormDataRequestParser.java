package org.javenstudio.lightning.request.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.List;

import org.javenstudio.common.indexdb.util.IOUtils;
import org.javenstudio.common.util.Logger;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.util.ContentStream;
import org.javenstudio.falcon.util.ContentStreamBase;
import org.javenstudio.falcon.util.FastInputStream;
import org.javenstudio.falcon.util.MultiMapParams;
import org.javenstudio.falcon.util.Params;
import org.javenstudio.lightning.request.HttpHelper;
import org.javenstudio.lightning.request.RequestInput;
import org.javenstudio.lightning.request.RequestParser;

/**
 * Extract application/x-www-form-urlencoded form data for POST requests
 */
public class FormDataRequestParser implements RequestParser {
	private static final Logger LOG = Logger.getLogger(FormDataRequestParser.class);

	public static boolean isFormData(RequestInput req) {
		String contentType = req.getContentType();
		if (contentType != null) {
			int idx = contentType.indexOf(';');
			if (idx > 0) { // remove the charset definition "; charset=utf-8"
				contentType = contentType.substring( 0, idx );
			}
			contentType = contentType.trim();
			if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) 
				return true;
		}
		return false;
	}
	
	private final long mUploadLimitKB;
	
	public FormDataRequestParser(long limit) { 
		mUploadLimitKB = limit;
	}
	
	@Override
	public Params parseParamsAndFillStreams(RequestInput input,
			List<ContentStream> streams) throws ErrorException {
		if (LOG.isDebugEnabled())
			LOG.debug("parseParamsAndFillStreams: input=" + input);
		
		if (!isFormData(input)) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"Not application/x-www-form-urlencoded content: " + input.getContentType());
		}
	    
		// also add possible URL parameters and include into the map (parsed using UTF-8):
		MultiMapParams params = HttpHelper.parseQueryString(input.getQueryString());
		
	    // may be -1, so we check again later. But if its already greater we can stop processing!
	    final long totalLength = input.getContentLength();
	    final long maxLength = ((long) mUploadLimitKB) * 1024L;
	    if (totalLength > maxLength) {
	    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    			"application/x-www-form-urlencoded content length (" +
	    			totalLength + " bytes) exceeds upload limit of " + mUploadLimitKB + " KB");
	    }
		
	    // get query String from request body, using the charset given in content-type:
	    final String cs = ContentStreamBase.getCharsetFromContentType(input.getContentType());
	    final Charset charset = (cs == null) ? IOUtils.CHARSET_UTF_8 : Charset.forName(cs);
	    InputStream in = null;
	    try {
	    	in = input.getInputStream();
	    	final long bytesRead = parseFormDataContent(
	    			FastInputStream.wrap(in), maxLength, charset, params);
	    	if (bytesRead == 0L && totalLength > 0L) 
	    		throw newParameterIncompatibilityException();
	    } catch (IOException ioe) {
	    	throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, ioe);
	    } catch (IllegalStateException ise) {
	    	throw (ErrorException) newParameterIncompatibilityException().initCause(ise);
	    } finally {
	    	IOUtils.closeWhileHandlingException(in);
	    }
	    
	    return params;
	}
	
	private ErrorException newParameterIncompatibilityException() {
	    return new ErrorException(ErrorException.ErrorCode.SERVER_ERROR,
	      "Requires that request parameters sent using application/x-www-form-urlencoded " +
	      "content-type can be read through the request input stream. Unfortunately, the " +
	      "stream was empty / not available. This may be caused by another servlet filter calling " +
	      "ServletRequest.getParameter*() before DispatchFilter, please remove it."
	    );
	}
	
	/** Makes the buffer of ByteArrayOutputStream available without copy. */
	static final class ByteArrayOutputStream2 extends ByteArrayOutputStream {
		byte[] buffer() {
			return this.buf;
		}
	}
	
	private static String decodeChars(ByteArrayOutputStream2 stream, 
			long position, CharsetDecoder charsetDecoder) throws ErrorException {
		try {
			return charsetDecoder.decode(ByteBuffer.wrap(stream.buffer(), 0, stream.size())).toString();
		} catch (CharacterCodingException cce) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST,
					"URLDecoder: Invalid character encoding detected after position " + position +
					" of query string / form data (while parsing as " + charsetDecoder.charset().name() + ")"
					);
		}
	}
	
	/**
	 * Given a url-encoded form from POST content (as InputStream), map it into the given map.
	 * The given InputStream should be buffered!
	 * @param postContent to be parsed
	 * @param charset to be used to decode resulting bytes after %-decoding
	 * @param map place all parameters in this map
	 */
	@SuppressWarnings("fallthrough")
	static long parseFormDataContent(final InputStream postContent, 
			final long maxLen, final Charset charset, 
			final MultiMapParams map) throws IOException, ErrorException {
		final CharsetDecoder charsetDecoder = charset.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		
	    long len = 0L, keyPos = 0L, valuePos = 0L;
	    
	    final ByteArrayOutputStream2 keyStream = new ByteArrayOutputStream2();
	    final ByteArrayOutputStream2 valueStream = new ByteArrayOutputStream2();
	    
	    ByteArrayOutputStream2 currentStream = keyStream;
	    for (;;) {
	    	int b = postContent.read();
	    	switch (b) {
	        	case -1: // end of stream
	        	case '&': // separator
	        		if (keyStream.size() > 0) {
	        			final String key = decodeChars(keyStream, keyPos, charsetDecoder);
	        			final String value = decodeChars(valueStream, valuePos, charsetDecoder);
	        			map.addParam(key, value);
	        		} else if (valueStream.size() > 0) {
	        			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	        					"application/x-www-form-urlencoded invalid: missing key");
	        		}
	        		keyStream.reset();
	        		valueStream.reset();
	        		keyPos = valuePos = len + 1;
	        		currentStream = keyStream;
	        		break;
	        	case '+': // space replacement
	        		currentStream.write(' ');
	        		break;
	        	case '%': // escape
	        		final int upper = digit16(b = postContent.read());
	        		len++;
	        		final int lower = digit16(b = postContent.read());
	        		len++;
	        		currentStream.write(((upper << 4) + lower));
	        		break;
	        	case '=': // kv separator
	        		if (currentStream == keyStream) {
	        			valuePos = len + 1;
	        			currentStream = valueStream;
	        			break;
	        		}
	        		// fall-through
	        	default:
	        		currentStream.write(b);
	    	}
	    	if (b == -1) {
	    		break;
	    	}
	    	len++;
	    	if (len > maxLen) {
	    		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
	    				"application/x-www-form-urlencoded content exceeds upload limit of " + 
	    						(maxLen/1024L) + " KB");
	    	}
	    }
    	return len;
	}
	
	private static int digit16(int b) throws ErrorException {
		if (b == -1) {
			throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
					"URLDecoder: Incomplete trailing escape (%) pattern");
		}
		if (b >= '0' && b <= '9') {
			return b - '0';
		}
		if (b >= 'A' && b <= 'F') {
			return b - ('A' - 10);
		}
		if (b >= 'a' && b <= 'f') {
			return b - ('a' - 10);
		}
		throw new ErrorException(ErrorException.ErrorCode.BAD_REQUEST, 
				"URLDecoder: Invalid digit (" + ((char) b) + ") in escape (%) pattern");
	}
	
}
