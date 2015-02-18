package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * This class produces a special form of reversed tokens, suitable for
 * better handling of leading wildcards. Tokens from the input TokenStream
 * are reversed and prepended with a special "reversed" marker character.
 * If <code>withOriginal<code> argument is <code>true</code> then first the
 * original token is returned, and then the reversed token (with
 * <code>positionIncrement == 0</code>) is returned. Otherwise only reversed
 * tokens are returned.
 * <p>Note: this filter doubles the number of tokens in the input stream when
 * <code>withOriginal == true</code>, which proportionally increases the size
 * of postings and term dictionary in the index.
 */
public final class ReversedWildcardFilter extends TokenFilter {
  
	private CharToken mSave = null;
	private boolean mWithOriginal;
	private char mMarkerChar;
	
	protected ReversedWildcardFilter(ITokenStream input, 
			boolean withOriginal, char markerChar) {
		super(input);
		mWithOriginal = withOriginal;
		mMarkerChar = markerChar;
	}

	@Override
	public IToken nextToken() throws IOException {
		if (mSave != null) {
			// clearAttributes();  // not currently necessary
			CharToken save = mSave;
			mSave = null;
			return save;
		}

		CharToken token = (CharToken)super.nextToken();
		if (token == null) 
			return null;

		// pass through zero-length terms
		int oldLen = token.getTerm().length();
		if (oldLen ==0) 
			return token;
		
		int origOffset = token.getPositionIncrement();
		if (mWithOriginal == true){
			token.setPositionIncrement(0);
			mSave = token;
		}
		
		char[] buffer = token.getTerm().resizeBuffer(oldLen + 1);
		buffer[oldLen] = mMarkerChar;
		reverse(buffer, 0, oldLen + 1);

		token.setPositionIncrement(origOffset);
		token.getTerm().copyBuffer(buffer, 0, oldLen +1);
    
		return token;
	}
  
	/**
	 * Partially reverses the given input buffer in-place from the given offset
	 * up to the given length, keeping surrogate pairs in the correct (non-reversed) order.
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
