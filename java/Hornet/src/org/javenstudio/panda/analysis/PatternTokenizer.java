package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.common.indexdb.util.CharTerm;

/**
 * This tokenizer uses regex pattern matching to construct distinct tokens
 * for the input stream.  It takes two arguments:  "pattern" and "group".
 * <p/>
 * <ul>
 * <li>"pattern" is the regular expression.</li>
 * <li>"group" says which group to extract into tokens.</li>
 *  </ul>
 * <p>
 * group=-1 (the default) is equivalent to "split".  In this case, the tokens will
 * be equivalent to the output from (without empty tokens):
 * {@link String#split(java.lang.String)}
 * </p>
 * <p>
 * Using group >= 0 selects the matching group as the token.  For example, if you have:<br/>
 * <pre>
 *  pattern = \'([^\']+)\'
 *  group = 0
 *  input = aaa 'bbb' 'ccc'
 *</pre>
 * the output will be two tokens: 'bbb' and 'ccc' (including the ' marks).  With the same input
 * but using group=1, the output would be: bbb and ccc (no ' marks)
 * </p>
 * <p>NOTE: This Tokenizer does not output tokens that are of zero length.</p>
 *
 * @see Pattern
 */
public final class PatternTokenizer extends Tokenizer {

	// TODO: we should see if we can make this tokenizer work without reading
	// the entire document into RAM, perhaps with Matcher.hitEnd/requireEnd ?
	private final char[] mBuffer = new char[8192];
	
	private final CharTerm mTerm = new CharTerm();
	private final CharToken mToken = new CharToken(mTerm);
	
	private final StringBuilder mText = new StringBuilder();
	private int mIndex;
  
	@SuppressWarnings("unused")
	private final Pattern mPattern;
	private final Matcher mMatcher;
	private final int mGroup;

	/** creates a new PatternTokenizer returning tokens from group (-1 for split functionality) */
	public PatternTokenizer(Reader input, Pattern pattern, int group) throws IOException {
		super(input);
		
		mPattern = pattern;
		mGroup = group;

		// Use "" instead of str so don't consume chars
		// (fillBuffer) from the input on throwing IAE below:
		mMatcher = pattern.matcher("");

		// confusingly group count depends ENTIRELY on the pattern but is only accessible via matcher
		if (group >= 0 && group > mMatcher.groupCount()) {
			throw new IllegalArgumentException("invalid group specified: pattern only has: " 
					+ mMatcher.groupCount() + " capturing groups");
		}
	}

	@Override
	public IToken nextToken() {
		if (mIndex >= mText.length()) 
			return null;
		
		mToken.clear();
		if (mGroup >= 0) {
			// match a specific group
			while (mMatcher.find()) {
				mIndex = mMatcher.start(mGroup);
				
				final int endIndex = mMatcher.end(mGroup);
				if (mIndex == endIndex) 
					continue;
				
				mTerm.setEmpty().append(mText, mIndex, endIndex);
				mToken.setOffset(correctOffset(mIndex), correctOffset(endIndex));
				
				return mToken;
			}
      
			mIndex = Integer.MAX_VALUE; // mark exhausted
			return null;
      
		} else {
			// String.split() functionality
			while (mMatcher.find()) {
				if (mMatcher.start() - mIndex > 0) {
					// found a non-zero-length token
					mTerm.setEmpty().append(mText, mIndex, mMatcher.start());
					mToken.setOffset(correctOffset(mIndex), correctOffset(mMatcher.start()));
					
					mIndex = mMatcher.end();
					
					return mToken;
				}
        
				mIndex = mMatcher.end();
			}
      
			if (mText.length() - mIndex == 0) {
				mIndex = Integer.MAX_VALUE; // mark exhausted
				return null;
			}
      
			mTerm.setEmpty().append(mText, mIndex, mText.length());
			mToken.setOffset(correctOffset(mIndex), correctOffset(mText.length()));
			
			mIndex = Integer.MAX_VALUE; // mark exhausted
			
			return mToken;
		}
	}

	@Override
	public int end() {
		final int ofs = correctOffset(mText.length());
		return ofs;
	}

	@Override
	public void reset() throws IOException {
		fillBuffer(mText, getInput());
		mMatcher.reset(mText);
		mIndex = 0;
	}
  
	private void fillBuffer(StringBuilder sb, Reader input) throws IOException {
		int len;
		sb.setLength(0);
		while ((len = input.read(mBuffer)) > 0) {
			sb.append(mBuffer, 0, len);
		}
	}
	
}
