package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.regex.Pattern;

import org.javenstudio.common.indexdb.analysis.Tokenizer;

/**
 * Factory for {@link PatternTokenizer}.
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
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_ptn" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.PatternTokenizerFactory" pattern="\'([^\']+)\'" group="1"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 * 
 * @see PatternTokenizer
 * @since solr1.2
 *
 */
public class PatternTokenizerFactory extends TokenizerFactory {
	
	public static final String PATTERN = "pattern";
	public static final String GROUP = "group";
 
	protected Pattern mPattern;
	protected int mGroup;
  
	/**
	 * Require a configured pattern
	 */
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
		
		mPattern = getPattern(PATTERN);
		mGroup = -1; // use 'split'
		
		String g = args.get(GROUP);
		if (g != null) 
			mGroup = Integer.parseInt(g);
	}
  
	/**
	 * Split the input using configured pattern
	 */
	@Override
	public Tokenizer create(final Reader in) throws IOException {
		return new PatternTokenizer(in, mPattern, mGroup);
	}
	
}
