package org.javenstudio.panda.analysis.standard;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.common.indexdb.analysis.TokenStream;
import org.javenstudio.panda.analysis.LowerCaseFilter;
import org.javenstudio.panda.analysis.StopAnalyzer;
import org.javenstudio.panda.analysis.StopFilter;
import org.javenstudio.panda.analysis.StopwordAnalyzerBase;
import org.javenstudio.panda.util.CharArraySet;

/**
 * Filters {@link StandardTokenizer} with {@link StandardFilter}, {@link
 * LowerCaseFilter} and {@link StopFilter}, using a list of
 * English stop words.
 *
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating StandardAnalyzer:
 * <ul>
 *   <li> As of 3.4, Hiragana and Han characters are no longer wrongly split
 *        from their combining characters. If you use a previous version number,
 *        you get the exact broken behavior for backwards compatibility.
 *   <li> As of 3.1, StandardTokenizer implements Unicode text segmentation,
 *        and StopFilter correctly handles Unicode 4.0 supplementary characters
 *        in stopwords.  {@link ClassicTokenizer} and {@link ClassicAnalyzer} 
 *        are the pre-3.1 implementations of StandardTokenizer and
 *        StandardAnalyzer.
 *   <li> As of 2.9, StopFilter preserves position increments
 *   <li> As of 2.4, Tokens incorrectly identified as acronyms
 *        are corrected (see <a href="https://issues.apache.org/jira/browse/LUCENE-1068">LUCENE-1068</a>)
 * </ul>
 */
public final class StandardAnalyzer extends StopwordAnalyzerBase {

	/** Default maximum allowed token length */
	public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

	/** 
	 * An unmodifiable set containing some common English words that are usually not
  	 * useful for searching. 
  	 */
	public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET; 

	private int mMaxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;
	
	/** 
	 * Builds an analyzer with the given stop words.
	 * @param matchVersion Lucene version to match See {@link
	 * <a href="#version">above</a>}
	 * @param stopWords stop words 
	 */
	public StandardAnalyzer(CharArraySet stopWords) {
		super(stopWords);
	}

	/** 
	 * Builds an analyzer with the default stop words ({@link
	 * #STOP_WORDS_SET}).
	 * @param matchVersion Lucene version to match See {@link
	 * <a href="#version">above</a>}
	 */
	public StandardAnalyzer() {
		this(STOP_WORDS_SET);
	}

	/** 
	 * Builds an analyzer with the stop words from the given reader.
	 * @see WordlistLoader#getWordSet(Reader, Version)
	 * @param matchVersion Lucene version to match See {@link
	 * <a href="#version">above</a>}
	 * @param stopwords Reader to read stop words from 
	 */
	public StandardAnalyzer(Reader stopwords) throws IOException {
		this(loadStopwordSet(stopwords));
	}

	/**
	 * Set maximum allowed token length.  If a token is seen
	 * that exceeds this length then it is discarded.  This
	 * setting only takes effect the next time tokenStream or
	 * tokenStream is called.
	 */
	public void setMaxTokenLength(int length) {
		mMaxTokenLength = length;
	}
    
	/**
	 * @see #setMaxTokenLength
	 */
	public int getMaxTokenLength() {
		return mMaxTokenLength;
	}

	@Override
	public TokenComponents createComponents(final String fieldName, final Reader reader) {
		final StandardTokenizer src = new StandardTokenizer(reader);
		src.setMaxTokenLength(mMaxTokenLength);
		
		TokenStream tok = new StandardFilter(src);
		tok = new LowerCaseFilter(tok);
		tok = new StopFilter(tok, mStopwords);
		
		return new TokenComponents(src, tok) {
				@Override
				protected boolean reset(final Reader r) throws IOException {
					src.setMaxTokenLength(StandardAnalyzer.this.mMaxTokenLength);
					return super.reset(r);
				}
			};
	}
	
}
