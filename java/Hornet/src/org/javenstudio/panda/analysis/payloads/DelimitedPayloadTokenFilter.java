package org.javenstudio.panda.analysis.payloads;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * Characters before the delimiter are the "token", those after are the payload.
 * <p/>
 * For example, if the delimiter is '|', then for the string "foo|bar", foo is the token
 * and "bar" is a payload.
 * <p/>
 * Note, you can also include a {@link PayloadEncoder} to convert the payload 
 * in an appropriate way (from characters to bytes).
 * <p/>
 * Note make sure your Tokenizer doesn't split on the delimiter, or this won't work
 *
 * @see PayloadEncoder
 */
public final class DelimitedPayloadTokenFilter extends TokenFilter {
	
	public static final char DEFAULT_DELIMITER = '|';
	
	private final char mDelimiter;
	private final PayloadEncoder mEncoder;

	public DelimitedPayloadTokenFilter(ITokenStream input, 
			char delimiter, PayloadEncoder encoder) {
		super(input);
		mDelimiter = delimiter;
		mEncoder = encoder;
		
		if (encoder == null) 
			throw new NullPointerException("PayloadEncoder is null");
	}

	@Override
	public IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token != null) {
			final char[] buffer = token.getTerm().buffer();
			final int length = token.getTerm().length();
			
			for (int i = 0; i < length; i++) {
				if (buffer[i] == mDelimiter) {
					token.setPayload(mEncoder.encode(buffer, i + 1, (length - (i + 1))));
					token.getTerm().setLength(i); // simply set a new length
					return token;
				}
			}
			
			// we have not seen the delimiter
			token.setPayload(null);
			
			return token;
		}
		
		return null;
	}
	
}
