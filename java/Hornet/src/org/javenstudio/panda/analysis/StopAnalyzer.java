package org.javenstudio.panda.analysis;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

import org.javenstudio.common.indexdb.analysis.LowerCaseTokenizer;
import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.panda.util.CharArraySet;
import org.javenstudio.panda.util.WordlistLoader;

/** 
 * Filters {@link LetterTokenizer} with {@link LowerCaseFilter} and {@link StopFilter}.
 *
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating StopAnalyzer:
 * <ul>
 *    <li> As of 3.1, StopFilter correctly handles Unicode 4.0
 *         supplementary characters in stopwords
 *   <li> As of 2.9, position increments are preserved
 * </ul>
 */
public final class StopAnalyzer extends StopwordAnalyzerBase {
  
	/** 
	 * An unmodifiable set containing some common English words that are not usually useful
  	 * for searching. 
  	 */
	public static final CharArraySet ENGLISH_STOP_WORDS_SET;
  
	static {
		final List<String> stopWords = Arrays.asList(
				"a", "an", "and", "are", "as", "at", "be", "but", "by",
				"for", "if", "in", "into", "is", "it",
				"no", "not", "of", "on", "or", "such",
				"that", "the", "their", "then", "there", "these",
				"they", "this", "to", "was", "will", "with"
			);
		
		ENGLISH_STOP_WORDS_SET = CharArraySet.unmodifiableSet(
				new CharArraySet(stopWords, false)); 
	}
  
	/** 
	 * Builds an analyzer which removes words in
	 *  {@link #ENGLISH_STOP_WORDS_SET}.
	 * @param matchVersion See <a href="#version">above</a>
	 */
	public StopAnalyzer() {
		this(ENGLISH_STOP_WORDS_SET);
	}

	/** 
	 * Builds an analyzer with the stop words from the given set.
	 * @param matchVersion See <a href="#version">above</a>
	 * @param stopWords Set of stop words 
	 */
	public StopAnalyzer(CharArraySet stopWords) {
		super(stopWords);
	}

	/** 
	 * Builds an analyzer with the stop words from the given file.
	 * @see WordlistLoader#getWordSet(Reader, Version)
	 * @param matchVersion See <a href="#version">above</a>
	 * @param stopwordsFile File to load stop words from 
	 */
	public StopAnalyzer(File stopwordsFile) throws IOException {
		this(loadStopwordSet(stopwordsFile));
	}

	/** 
	 * Builds an analyzer with the stop words from the given reader.
	 * @see WordlistLoader#getWordSet(Reader, Version)
	 * @param matchVersion See <a href="#version">above</a>
	 * @param stopwords Reader to load stop words from 
	 */
	public StopAnalyzer(Reader stopwords) throws IOException {
		this(loadStopwordSet(stopwords));
	}

	/**
	 * Creates
	 * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 * used to tokenize all the text in the provided {@link Reader}.
	 * 
	 * @return {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 *         built from a {@link LowerCaseTokenizer} filtered with
	 *         {@link StopFilter}
	 */
	@Override
	public TokenComponents createComponents(String fieldName, Reader reader) {
		final Tokenizer source = new LowerCaseTokenizer(reader);
		return new TokenComponents(source, new StopFilter(source, mStopwords));
	}
	
}

