package org.javenstudio.panda.analysis;

import java.io.Reader;
import java.util.Map;

import org.javenstudio.common.indexdb.analysis.LowerCaseTokenizer;
import org.javenstudio.panda.util.MultiTermAwareComponent;

/**
 * Factory for {@link LowerCaseTokenizer}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_lwrcase" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.LowerCaseTokenizerFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class LowerCaseTokenizerFactory extends TokenizerFactory 
		implements MultiTermAwareComponent {
	
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
	}

	@Override
	public LowerCaseTokenizer create(Reader input) {
		return new LowerCaseTokenizer(input);
	}

	@Override
	public AnalysisFactory getMultiTermComponent() {
		LowerCaseFilterFactory filt = new LowerCaseFilterFactory();
		filt.init(getArgs());
		return filt;
	}
	
}
