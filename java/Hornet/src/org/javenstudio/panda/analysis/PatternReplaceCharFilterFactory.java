package org.javenstudio.panda.analysis;

import java.io.Reader;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Factory for {@link PatternReplaceCharFilter}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_ptnreplace" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;charFilter class="solr.PatternReplaceCharFilterFactory" 
 *                    pattern="([^a-z])" replacement=""/&gt;
 *     &lt;tokenizer class="solr.KeywordTokenizerFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 * 
 * @since Solr 3.1
 */
@SuppressWarnings("deprecation")
public class PatternReplaceCharFilterFactory extends CharFilterFactory {
  
	private String mReplacement;
	private int mMaxBlockChars;
	private String mBlockDelimiters;
	private Pattern mPattern;

	@Override
	public void init(Map<String, String> args) {
		super.init(args);
		
		mPattern = getPattern("pattern");
		mReplacement = args.get("replacement");
		if (mReplacement == null)
			mReplacement = "";
		
		// TODO: warn if you set maxBlockChars or blockDelimiters ?
		mMaxBlockChars = getInt("maxBlockChars", PatternReplaceCharFilter.DEFAULT_MAX_BLOCK_CHARS);
		mBlockDelimiters = args.get("blockDelimiters");
	}

	@Override
	public CharFilter create(Reader input) {
		return new PatternReplaceCharFilter(mPattern, mReplacement, mMaxBlockChars, mBlockDelimiters, input);
	}
	
}
