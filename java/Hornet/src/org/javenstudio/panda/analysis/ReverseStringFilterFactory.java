package org.javenstudio.panda.analysis;

import org.javenstudio.common.indexdb.ITokenStream;

/**
 * Factory for {@link ReverseStringFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_rvsstr" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.ReverseStringFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 * @since solr 1.4
 */
public class ReverseStringFilterFactory extends TokenFilterFactory {
	
	@Override
	public ReverseStringFilter create(ITokenStream in) {
		return new ReverseStringFilter(in);
	}
	
}

