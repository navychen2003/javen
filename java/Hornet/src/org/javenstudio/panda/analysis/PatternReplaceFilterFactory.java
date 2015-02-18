package org.javenstudio.panda.analysis;

import java.util.Map;
import java.util.regex.Pattern;

import org.javenstudio.common.indexdb.ITokenStream;

/**
 * Factory for {@link PatternReplaceFilter}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_ptnreplace" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.KeywordTokenizerFactory"/&gt;
 *     &lt;filter class="solr.PatternReplaceFilterFactory" pattern="([^a-z])" replacement=""
 *             replace="all"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 * @see PatternReplaceFilter
 */
public class PatternReplaceFilterFactory extends TokenFilterFactory {
	
	private Pattern mPattern;
	private String mReplacement;
	private boolean mAll = true;
  
	@Override
	public void init(Map<String, String> args) {
		super.init(args);
		
		mPattern = getPattern("pattern");
		mReplacement = args.get("replacement");
    
		String r = args.get("replace");
		if (r != null) {
			if (r.equals("all")) {
				mAll = true;
				
			} else {
				if (r.equals("first")) {
					mAll = false;
				} else {
					throw new IllegalArgumentException("Configuration Error: 'replace' must be 'first' or 'all' in "
							+ getClass().getName());
				}
			}
		}
	}
	
	@Override
	public PatternReplaceFilter create(ITokenStream input) {
		return new PatternReplaceFilter(input, mPattern, mReplacement, mAll);
	}
	
}
