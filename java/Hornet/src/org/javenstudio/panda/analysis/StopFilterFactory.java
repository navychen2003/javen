package org.javenstudio.panda.analysis;

import java.util.Map;
import java.io.IOException;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.panda.util.CharArraySet;
import org.javenstudio.panda.util.ResourceLoader;
import org.javenstudio.panda.util.ResourceLoaderAware;

/**
 * Factory for {@link StopFilter}.
 * <pre class="prettyprint" >
 * &lt;fieldType name="text_stop" class="lightning.TextFieldType" positionIncrementGap="100" 
 * 		autoGeneratePhraseQueries="true"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="lightning.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="lightning.StopFilterFactory" ignoreCase="true"
 *             words="stopwords.txt" enablePositionIncrements="true"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 *
 */
public class StopFilterFactory extends TokenFilterFactory 
		implements ResourceLoaderAware {

	private CharArraySet mStopWords;
	private boolean mIgnoreCase;
	private boolean mEnablePositionIncrements;
	
	@Override
	public void init(Map<String,String> args) {
		super.init(args);
	}

	@Override
	public void inform(ResourceLoader loader) throws IOException {
		String stopWordFiles = getArgs().get("words");
		
		mIgnoreCase = getBoolean("ignoreCase", false);
		mEnablePositionIncrements = getBoolean("enablePositionIncrements", false);

		if (stopWordFiles != null) {
			if ("snowball".equalsIgnoreCase(getArgs().get("format"))) 
				mStopWords = getSnowballWordSet(loader, stopWordFiles, mIgnoreCase);
			else 
				mStopWords = getWordSet(loader, stopWordFiles, mIgnoreCase);
			
		} else {
			mStopWords = new CharArraySet(StopAnalyzer.ENGLISH_STOP_WORDS_SET, mIgnoreCase);
		}
	}

	public boolean isEnablePositionIncrements() { return mEnablePositionIncrements; }
	public boolean isIgnoreCase() { return mIgnoreCase; }
	public CharArraySet getStopWords() { return mStopWords; }

	@Override
	public ITokenStream create(ITokenStream input) {
		StopFilter stopFilter = new StopFilter(input, mStopWords);
		stopFilter.setEnablePositionIncrements(mEnablePositionIncrements);
		return stopFilter;
	}
	
}
