package org.javenstudio.panda.analysis;

import org.javenstudio.common.indexdb.ITokenStream;

/**
 * Factory for {@link RemoveDuplicatesTokenFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_rmdup" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.RemoveDuplicatesTokenFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 */
public class RemoveDuplicatesTokenFilterFactory extends TokenFilterFactory {
	
	@Override
	public RemoveDuplicatesTokenFilter create(ITokenStream input) {
		return new RemoveDuplicatesTokenFilter(input);
	}
	
}
