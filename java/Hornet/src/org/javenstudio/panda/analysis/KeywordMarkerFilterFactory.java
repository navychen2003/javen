package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.util.CharArraySet;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;

/**
 * Factory for {@link KeywordMarkerFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_keyword" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.KeywordMarkerFilterFactory" protected="protectedkeyword.txt" ignoreCase="false"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre> 
 *
 */
public class KeywordMarkerFilterFactory extends TokenFilterFactory 
		implements ResourceLoaderAware {
	
	public static final String PROTECTED_TOKENS = "protected";
	
	private CharArraySet mProtectedWords;
	private boolean mIgnoreCase;
  
	@Override
	public void inform(ResourceLoader loader) throws IOException {
		String wordFiles = getArgs().get(PROTECTED_TOKENS);
		mIgnoreCase = getBoolean("ignoreCase", false);
		if (wordFiles != null) 
			mProtectedWords = getWordSet(loader, wordFiles, mIgnoreCase);
	}
  
	public boolean isIgnoreCase() { return mIgnoreCase; }

	@SuppressWarnings("resource")
	@Override
	public ITokenStream create(ITokenStream input) {
		return mProtectedWords == null ? input : new KeywordMarkerFilter(input, mProtectedWords);
	}
	
}
