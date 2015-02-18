package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * Reverse token string, for example "country" => "yrtnuoc".
 * <p>
 * If <code>marker</code> is supplied, then tokens will be also prepended by
 * that character. For example, with a marker of &#x5C;u0001, "country" =>
 * "&#x5C;u0001yrtnuoc". This is useful when implementing efficient leading
 * wildcards search.
 * </p>
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating ReverseStringFilter, or when using any of
 * its static methods:
 * <ul>
 *   <li> As of 3.1, supplementary characters are handled correctly
 * </ul>
 */
public final class ReverseStringFilter extends TokenFilter {

	/**
	 * Example marker character: U+0001 (START OF HEADING) 
	 */
	public static final char START_OF_HEADING_MARKER = '\u0001';
  
	/**
	 * Example marker character: U+001F (INFORMATION SEPARATOR ONE)
	 */
	public static final char INFORMATION_SEPARATOR_MARKER = '\u001F';
  
	/**
	 * Example marker character: U+EC00 (PRIVATE USE AREA: EC00) 
	 */
	public static final char PUA_EC00_MARKER = '\uEC00';
  
	/**
	 * Example marker character: U+200F (RIGHT-TO-LEFT MARK)
	 */
	public static final char RTL_DIRECTION_MARKER = '\u200F';
	
	public static final char NOMARKER = '\uFFFF';
	
	private final char mMarker;
  
	/**
	 * Create a new ReverseStringFilter that reverses all tokens in the 
	 * supplied {@link TokenStream}.
	 * <p>
	 * The reversed tokens will not be marked. 
	 * </p>
	 * 
	 * @param matchVersion See <a href="#version">above</a>
	 * @param in {@link TokenStream} to filter
	 */
	public ReverseStringFilter(ITokenStream in) {
		this(in, NOMARKER);
	}

	/**
	 * Create a new ReverseStringFilter that reverses and marks all tokens in the
	 * supplied {@link TokenStream}.
	 * <p>
	 * The reversed tokens will be prepended (marked) by the <code>marker</code>
	 * character.
	 * </p>
	 * 
	 * @param matchVersion See <a href="#version">above</a>
	 * @param in {@link TokenStream} to filter
	 * @param marker A character used to mark reversed tokens
	 */
	public ReverseStringFilter(ITokenStream in, char marker) {
		super(in);
		mMarker = marker;
	}

	@Override
	public IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token != null) {
			int len = token.getTerm().length();
			if (mMarker != NOMARKER) {
				len ++;
				token.getTerm().resizeBuffer(len);
				token.getTerm().buffer()[len - 1] = mMarker;
			}
			
			reverse(token.getTerm().buffer(), 0, len);
			token.getTerm().setLength(len);
			
			return token;
		}
		
		return null;
	}

	/**
	 * Reverses the given input string
	 * 
	 * @param matchVersion See <a href="#version">above</a>
	 * @param input the string to reverse
	 * @return the given input string in reversed order
	 */
	public static String reverse(final String input ){
		final char[] charInput = input.toCharArray();
		reverse(charInput, 0, charInput.length );
		return new String( charInput );
	}
  
	/**
	 * Reverses the given input buffer in-place
	 * @param matchVersion See <a href="#version">above</a>
	 * @param buffer the input char array to reverse
	 */
	public static void reverse(final char[] buffer) {
		reverse(buffer, 0, buffer.length);
	}
  
	/**
	 * Partially reverses the given input buffer in-place from offset 0
	 * up to the given length.
	 * @param matchVersion See <a href="#version">above</a>
	 * @param buffer the input char array to reverse
	 * @param len the length in the buffer up to where the
	 *        buffer should be reversed
	 */
	public static void reverse(final char[] buffer, final int len) {
		reverse(buffer, 0, len );
	}
  
	/**
	 * @deprecated (3.1) Remove this when support for 3.0 indexes is no longer needed.
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private static void reverseUnicode3(char[] buffer, int start, int len) {
		if (len <= 1) return;
		
		int num = len>>1;
		for (int i = start; i < (start + num); i++) {
			char c = buffer[i];
			buffer[i] = buffer[start * 2 + len - i - 1];
			buffer[start * 2 + len - i - 1] = c;
		}
	}
  
	/**
	 * Partially reverses the given input buffer in-place from the given offset
	 * up to the given length.
	 * @param matchVersion See <a href="#version">above</a>
	 * @param buffer the input char array to reverse
	 * @param start the offset from where to reverse the buffer
	 * @param len the length in the buffer up to where the
	 *        buffer should be reversed
	 */
	public static void reverse(final char[] buffer, final int start, final int len) {
		/** modified version of Apache Harmony AbstractStringBuilder reverse0() */
		if (len < 2)
			return;
		
		int end = (start + len) - 1;
		
		char frontHigh = buffer[start];
		char endLow = buffer[end];
		
		boolean allowFrontSur = true, allowEndSur = true;
		final int mid = start + (len >> 1);
		
		for (int i = start; i < mid; ++i, --end) {
			final char frontLow = buffer[i + 1];
			final char endHigh = buffer[end - 1];
			final boolean surAtFront = allowFrontSur && 
					Character.isSurrogatePair(frontHigh, frontLow);
			
			if (surAtFront && (len < 3)) {
				// nothing to do since surAtFront is allowed and 1 char left
				return;
			}
			
			final boolean surAtEnd = allowEndSur && 
					Character.isSurrogatePair(endHigh, endLow);
			
			allowFrontSur = allowEndSur = true;
			if (surAtFront == surAtEnd) {
				if (surAtFront) {
					// both surrogates
					buffer[end] = frontLow;
					buffer[--end] = frontHigh;
					buffer[i] = endHigh;
					buffer[++i] = endLow;
					frontHigh = buffer[i + 1];
					endLow = buffer[end - 1];
					
				} else {
					// neither surrogates
					buffer[end] = frontHigh;
					buffer[i] = endLow;
					frontHigh = frontLow;
					endLow = endHigh;
				}
			} else {
				if (surAtFront) {
					// surrogate only at the front
					buffer[end] = frontLow;
					buffer[i] = endLow;
					endLow = endHigh;
					allowFrontSur = false;
					
				} else {
					// surrogate only at the end
					buffer[end] = frontHigh;
					buffer[i] = endHigh;
					frontHigh = frontLow;
					allowEndSur = false;
				}
			}
		}
		
		if ((len & 0x01) == 1 && !(allowFrontSur && allowEndSur)) {
			// only if odd length
			buffer[end] = allowFrontSur ? endLow : frontHigh;
		}
	}
	
}
