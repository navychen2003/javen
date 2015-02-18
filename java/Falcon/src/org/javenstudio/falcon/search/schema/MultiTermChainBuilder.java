package org.javenstudio.falcon.search.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.analysis.TokenizerChain;
import org.javenstudio.panda.analysis.AnalysisFactory;
import org.javenstudio.panda.analysis.CharFilterFactory;
import org.javenstudio.panda.analysis.KeywordTokenizerFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.analysis.TokenizerFactory;
import org.javenstudio.panda.util.MultiTermAwareComponent;

public class MultiTermChainBuilder {

	static final KeywordTokenizerFactory sKeyFactory;
	static {
		sKeyFactory = new KeywordTokenizerFactory();
		sKeyFactory.init(new HashMap<String,String>());
	}

	private List<CharFilterFactory> mCharFilters = null;
	private List<TokenFilterFactory> mFilters = new ArrayList<TokenFilterFactory>(2);
	private TokenizerFactory mTokenizer = sKeyFactory;

	public void add(Object current) throws ErrorException {
		if (!(current instanceof MultiTermAwareComponent)) 
			return;
		
		AnalysisFactory newComponent = ((MultiTermAwareComponent)current).getMultiTermComponent();
		if (newComponent instanceof TokenFilterFactory) {
			if (mFilters == null) 
				mFilters = new ArrayList<TokenFilterFactory>(2);
			
			mFilters.add((TokenFilterFactory)newComponent);
			
		} else if (newComponent instanceof TokenizerFactory) {
			mTokenizer = (TokenizerFactory)newComponent;
			
		} else if (newComponent instanceof CharFilterFactory) {
			if (mCharFilters == null) 
				mCharFilters = new ArrayList<CharFilterFactory>(1);
			
			mCharFilters.add((CharFilterFactory)newComponent);

		} else {
			throw new ErrorException(ErrorException.ErrorCode.SERVER_ERROR, 
					"Unknown analysis component from MultiTermAwareComponent: " + newComponent);
		}
	}

	public TokenizerChain build() {
		CharFilterFactory[] charFilterArr = (mCharFilters == null) ? null : 
			mCharFilters.toArray(new CharFilterFactory[mCharFilters.size()]);
		TokenFilterFactory[] filterArr = (mFilters == null) ? new TokenFilterFactory[0] : 
			mFilters.toArray(new TokenFilterFactory[mFilters.size()]);
		
		return new TokenizerChain(mTokenizer, filterArr, charFilterArr);
	}
	
}
