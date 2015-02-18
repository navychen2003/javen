package org.javenstudio.panda.analysis.standard;

import java.io.Reader;
import java.util.Map;

import org.javenstudio.panda.analysis.TokenizerFactory;

/**
 * Factory for {@link StandardTokenizer}. 
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_stndrd" class="lightning.TextFieldType" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.StandardTokenizerFactory" maxTokenLength="255"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class StandardTokenizerFactory extends TokenizerFactory {
  
	private int mMaxTokenLength;
  
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
		mMaxTokenLength = getInt("maxTokenLength", StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
	}

	@Override
	public StandardTokenizer create(Reader input) {
		StandardTokenizer tokenizer = new StandardTokenizer(input); 
		tokenizer.setMaxTokenLength(mMaxTokenLength);
		return tokenizer;
	}
	
}
