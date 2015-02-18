package org.javenstudio.panda.language.english;

import java.util.Map;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.analysis.TokenFilterFactory;

/**
 * Factory for {@link EnglishPossessiveFilter}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_enpossessive" class="lightning.TextFieldType" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.StandardTokenizerFactory"/&gt;
 *     &lt;filter class="lightning.LowerCaseFilterFactory"/&gt;
 *     &lt;filter class="lightning.EnglishPossessiveFilterFactory"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class EnglishPossessiveFilterFactory extends TokenFilterFactory {
  
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
	}
  
	@Override
	public ITokenStream create(ITokenStream input) {
		return new EnglishPossessiveFilter(input);
	}
	
}
