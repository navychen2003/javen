package org.javenstudio.panda.analysis.snowball;

import java.io.Reader;

import org.javenstudio.common.indexdb.analysis.Analyzer;
import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.common.indexdb.analysis.TokenStream;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.panda.analysis.LowerCaseFilter;
import org.javenstudio.panda.analysis.StopFilter;
import org.javenstudio.panda.analysis.standard.StandardFilter;
import org.javenstudio.panda.analysis.standard.StandardTokenizer;
import org.javenstudio.panda.language.english.EnglishPossessiveFilter;
import org.javenstudio.panda.util.CharArraySet;

/** 
 * Filters {@link StandardTokenizer} with {@link StandardFilter}, {@link
 * LowerCaseFilter}, {@link StopFilter} and {@link SnowballFilter}.
 *
 * Available stemmers are listed in org.tartarus.snowball.ext.  The name of a
 * stemmer is the part of the class name before "Stemmer", e.g., the stemmer in
 * {@link org.javenstudio.panda.snowball.ext.EnglishStemmer} is named "English".
 *
 * <p><b>NOTE</b>: This class uses the same {@link Version}
 * dependent settings as {@link StandardAnalyzer}, with the following addition:
 * <ul>
 *   <li> As of 3.1, uses {@link TurkishLowerCaseFilter} for Turkish language.
 * </ul>
 * </p>
 */
public final class SnowballAnalyzer extends Analyzer {
	
	private String mName;
	private CharArraySet mStopSet;

	public SnowballAnalyzer() { 
		this("English");
	}
	
	/** Builds the named analyzer with no stop words. */
	public SnowballAnalyzer(String name) {
		mName = name;
	}

	/** Builds the named analyzer with the given stop words. */
	public SnowballAnalyzer(String name, CharArraySet stopWords) {
		this(name);
		mStopSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stopWords));
	}

	/** 
	 * Constructs a {@link StandardTokenizer} filtered by a {@link
     * StandardFilter}, a {@link LowerCaseFilter}, a {@link StopFilter},
     * and a {@link SnowballFilter} 
     */
	@Override
	public TokenComponents createComponents(String fieldName, Reader reader) {
		Tokenizer tokenizer = new StandardTokenizer(reader);
		TokenStream result = new StandardFilter(tokenizer);
		
		// remove the possessive 's for english stemmers
		if (mName.equals("English") || mName.equals("Porter") || mName.equals("Lovins"))
			result = new EnglishPossessiveFilter(result);
		
		// Use a special lowercase filter for turkish, the stemmer expects it.
		result = new LowerCaseFilter(result);
		
		if (mStopSet != null)
			result = new StopFilter(result, mStopSet);
		
		result = new SnowballFilter(result, mName);
		
		return new TokenComponents(tokenizer, result);
	}
	
}
