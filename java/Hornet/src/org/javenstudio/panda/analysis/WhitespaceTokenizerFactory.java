package org.javenstudio.panda.analysis;

import java.io.Reader;
import java.util.Map;

/**
 * Factory for {@link WhitespaceTokenizer}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_ws" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class WhitespaceTokenizerFactory extends TokenizerFactory {
	
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
	}

	@Override
	public WhitespaceTokenizer create(Reader input) {
		return new WhitespaceTokenizer(input);
	}
	
}
