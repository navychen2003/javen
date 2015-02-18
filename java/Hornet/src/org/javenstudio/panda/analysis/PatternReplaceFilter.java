package org.javenstudio.panda.analysis;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * A TokenFilter which applies a Pattern to each token in the stream,
 * replacing match occurances with the specified replacement string.
 *
 * <p>
 * <b>Note:</b> Depending on the input and the pattern used and the input
 * TokenStream, this TokenFilter may produce Tokens whose text is the empty
 * string.
 * </p>
 * 
 * @see Pattern
 */
public final class PatternReplaceFilter extends TokenFilter {
	
	private final Pattern mPattern;
	private final String mReplacement;
	private final boolean mAll;

	/**
	 * Constructs an instance to replace either the first, or all occurances
	 *
	 * @param in the TokenStream to process
	 * @param p the patterm to apply to each Token
	 * @param replacement the "replacement string" to substitute, if null a
	 *        blank string will be used. Note that this is not the literal
	 *        string that will be used, '$' and '\' have special meaning.
	 * @param all if true, all matches will be replaced otherwise just the first match.
	 * @see Matcher#quoteReplacement
	 */
	public PatternReplaceFilter(ITokenStream in, 
			Pattern p, String replacement, boolean all) {
		super(in);
		mPattern = p;
		mReplacement = (replacement == null) ? "" : replacement;
		mAll = all;
	}

	@Override
	public IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token == null) 
			return null;
    
		Matcher m = mPattern.matcher(token.getTerm());
		if (m.find()) {
			// replaceAll/replaceFirst will reset() this previous find.
			String transformed = mAll ? m.replaceAll(mReplacement) : m.replaceFirst(mReplacement);
			token.getTerm().setEmpty().append(transformed);
		}

		return token;
	}

}
