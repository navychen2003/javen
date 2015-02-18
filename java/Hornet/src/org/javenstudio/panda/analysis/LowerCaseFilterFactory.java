package org.javenstudio.panda.analysis;

import java.util.Map;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.util.MultiTermAwareComponent;

/**
 * Factory for {@link LowerCaseFilter}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_lwrcase" class="lightning.TextFieldType" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="lightning.LowerCaseFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class LowerCaseFilterFactory extends TokenFilterFactory 
		implements MultiTermAwareComponent {
	
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
	}

	@Override
	public LowerCaseFilter create(ITokenStream input) {
		return new LowerCaseFilter(input);
	}

	@Override
	public AnalysisFactory getMultiTermComponent() {
		return this;
	}
	
}
