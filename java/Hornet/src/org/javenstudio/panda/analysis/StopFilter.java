package org.javenstudio.panda.analysis;

import java.util.Arrays;
import java.util.List;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.util.CharTerm;
import org.javenstudio.panda.util.CharArraySet;

/**
 * Removes stop words from a token stream.
 * 
 * <a name="version"/>
 * <p>You must specify the required {@link Version}
 * compatibility when creating StopFilter:
 * <ul>
 *   <li> As of 3.1, StopFilter correctly handles Unicode 4.0
 *         supplementary characters in stopwords and position
 *         increments are preserved
 * </ul>
 */
public final class StopFilter extends FilteringTokenFilter {

	private final CharArraySet mStopWords;
  
	/**
	 * Constructs a filter which removes words from the input TokenStream that are
	 * named in the Set.
	 * 
	 * @param matchVersion
	 *          Lucene version to enable correct Unicode 4.0 behavior in the stop
	 *          set if Version > 3.0.  See <a href="#version">above</a> for details.
	 * @param in
	 *          Input stream
	 * @param stopWords
	 *          A {@link CharArraySet} representing the stopwords.
	 * @see #makeStopSet(Version, java.lang.String...)
	 */
	public StopFilter(ITokenStream in, CharArraySet stopWords) {
		super(true, in);
		mStopWords = stopWords;
	}

	/**
	 * Builds a Set from an array of stop words,
	 * appropriate for passing into the StopFilter constructor.
	 * This permits this stopWords construction to be cached once when
	 * an Analyzer is constructed.
	 * 
	 * @param matchVersion Lucene version to enable correct Unicode 4.0 
	 * behavior in the returned set if Version > 3.0
	 * @param stopWords An array of stopwords
	 * @see #makeStopSet(Version, java.lang.String[], boolean) passing false to ignoreCase
	 */
	public static CharArraySet makeStopSet(String... stopWords) {
		return makeStopSet(stopWords, false);
	}
  
	/**
	 * Builds a Set from an array of stop words,
	 * appropriate for passing into the StopFilter constructor.
	 * This permits this stopWords construction to be cached once when
	 * an Analyzer is constructed.
	 * 
	 * @param matchVersion Lucene version to enable correct Unicode 4.0 
	 * behavior in the returned set if Version > 3.0
	 * @param stopWords A List of Strings or char[] or any other 
	 * toString()-able list representing the stopwords
	 * @return A Set ({@link CharArraySet}) containing the words
	 * @see #makeStopSet(Version, java.lang.String[], boolean) passing false to ignoreCase
	 */
	public static CharArraySet makeStopSet(List<?> stopWords) {
		return makeStopSet(stopWords, false);
	}
    
	/**
	 * Creates a stopword set from the given stopword array.
	 * 
	 * @param matchVersion Lucene version to enable correct Unicode 4.0 
	 * behavior in the returned set if Version > 3.0
	 * @param stopWords An array of stopwords
	 * @param ignoreCase If true, all words are lower cased first.  
	 * @return a Set containing the words
	 */    
	public static CharArraySet makeStopSet(String[] stopWords, boolean ignoreCase) {
		CharArraySet stopSet = new CharArraySet(stopWords.length, ignoreCase);
		stopSet.addAll(Arrays.asList(stopWords));
		return stopSet;
	}
  
	/**
	 * Creates a stopword set from the given stopword list.
	 * @param matchVersion Lucene version to enable correct Unicode 4.0 
	 * behavior in the returned set if Version > 3.0
	 * @param stopWords A List of Strings or char[] or any other 
	 * toString()-able list representing the stopwords
	 * @param ignoreCase if true, all words are lower cased first
	 * @return A Set ({@link CharArraySet}) containing the words
	 */
	public static CharArraySet makeStopSet(List<?> stopWords, boolean ignoreCase){
		CharArraySet stopSet = new CharArraySet(stopWords.size(), ignoreCase);
		stopSet.addAll(stopWords);
		return stopSet;
	}
  
	/**
	 * Returns the next input Token whose term() is not a stop word.
	 */
	@Override
	protected boolean accept(IToken token) {
		if (token instanceof CharToken) {
			CharTerm term = ((CharToken)token).getTerm();
			if (mStopWords != null)
				return !mStopWords.contains(term.buffer(), 0, term.length());
			else
				return true;
		}
		
		return false;
	}

}
