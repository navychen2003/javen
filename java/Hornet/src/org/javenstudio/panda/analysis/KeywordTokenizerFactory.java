package org.javenstudio.panda.analysis;

import java.io.Reader;

/**
 * Factory for {@link KeywordTokenizer}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_keyword" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.KeywordTokenizerFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class KeywordTokenizerFactory extends TokenizerFactory {
	
	@Override
	public KeywordTokenizer create(Reader input) {
		return new KeywordTokenizer(input);
	}
	
}
