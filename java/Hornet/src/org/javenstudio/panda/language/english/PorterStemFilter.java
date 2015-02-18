package org.javenstudio.panda.language.english;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/** 
 * Transforms the token stream as per the Porter stemming algorithm.
 * Note: the input to the stemming filter must already be in lower case,
 *  so you will need to use LowerCaseFilter or LowerCaseTokenizer farther
 *  down the Tokenizer chain in order for this to work properly!
 *  <P>
 *  To use this filter with other analyzers, you'll want to write an
 *  Analyzer class that sets up the TokenStream chain as you want it.
 *  To use this with LowerCaseTokenizer, for example, you'd write an
 *  analyzer like this:
 *  <P>
 *  <PRE class="prettyprint">
 *  class MyAnalyzer extends Analyzer {
 *    {@literal @Override}
 *    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
 *      Tokenizer source = new LowerCaseTokenizer(version, reader);
 *      return new TokenStreamComponents(source, new PorterStemFilter(source));
 *    }
 *  }
 *  </PRE>
 *  <p>
 *  Note: This filter is aware of the {@link KeywordAttribute}. To prevent
 *  certain terms from being passed to the stemmer
 *  {@link KeywordAttribute#isKeyword()} should be set to <code>true</code>
 *  in a previous {@link TokenStream}.
 *  </p>
 */
public final class PorterStemFilter extends TokenFilter {
	
	private final PorterStemmer mStemmer = new PorterStemmer();

	public PorterStemFilter(ITokenStream in) {
		super(in);
	}

	@Override
	public final IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token != null) {
			if ((!token.isKeyword()) && mStemmer.stem(token.getTerm().buffer(), 0, 
					token.getTerm().length())) {
				token.getTerm().copyBuffer(mStemmer.getResultBuffer(), 0, 
						mStemmer.getResultLength());
			}
		}
		
		return token;
	}
	
}
