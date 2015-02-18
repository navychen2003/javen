package org.javenstudio.panda.language.english;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.analysis.TokenFilterFactory;

/** 
 * Factory for {@link EnglishMinimalStemFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_enminstem" class="lightning.TextFieldType" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.StandardTokenizerFactory"/&gt;
 *     &lt;filter class="lightning.LowerCaseFilterFactory"/&gt;
 *     &lt;filter class="lightning.EnglishMinimalStemFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 */
public class EnglishMinimalStemFilterFactory extends TokenFilterFactory {
	
	@Override
	public ITokenStream create(ITokenStream input) {
		return new EnglishMinimalStemFilter(input);
	}
	
}
