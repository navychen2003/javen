package org.javenstudio.panda.language.english;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.analysis.TokenFilterFactory;

/**
 * Factory for {@link PorterStemFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_porterstem" class="lightning.TextFieldType" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.StandardTokenizerFactory"/&gt;
 *     &lt;filter class="lightning.LowerCaseFilterFactory"/&gt;
 *     &lt;filter class="lightning.PorterStemFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 */
public class PorterStemFilterFactory extends TokenFilterFactory {
	
	@Override
	public PorterStemFilter create(ITokenStream input) {
		return new PorterStemFilter(input);
	}
	
}
