package org.javenstudio.falcon.search.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.panda.analysis.CharFilterFactory;
import org.javenstudio.panda.analysis.TokenFilterFactory;
import org.javenstudio.panda.analysis.TokenizerFactory;

/**
 * An analyzer that uses a tokenizer and a list of token filters to
 * create a TokenStream.
 */
public final class TokenizerChain extends BaseAnalyzer {
	
	private final CharFilterFactory[] mCharFilters;
	private final TokenizerFactory mTokenizer;
	private final TokenFilterFactory[] mFilters;

	public TokenizerChain(TokenizerFactory tokenizer, TokenFilterFactory[] filters) {
		this(tokenizer, filters, null);
	}

	public TokenizerChain(TokenizerFactory tokenizer, TokenFilterFactory[] filters, 
			CharFilterFactory[] charFilters) {
		mCharFilters = charFilters;
		mTokenizer = tokenizer;
		mFilters = filters;
	}

	public CharFilterFactory[] getCharFilterFactories() { return mCharFilters; }
	public TokenizerFactory getTokenizerFactory() { return mTokenizer; }
	public TokenFilterFactory[] getTokenFilterFactories() { return mFilters; }

	@Override
	public Reader initReader(String fieldName, Reader reader) throws IOException {
		if (mCharFilters != null && mCharFilters.length > 0) {
			Reader cs = reader;
			
			for (CharFilterFactory charFilter : mCharFilters) {
				cs = charFilter.create(cs);
			}
			
			reader = cs;
		}
		
		return reader;
	}

	@Override
	public TokenComponents createComponents(String fieldName, 
			Reader aReader) throws IOException {
		Tokenizer tk = mTokenizer.create(aReader);
		ITokenStream ts = tk;
		
		for (TokenFilterFactory filter : mFilters) {
			ts = filter.create(ts);
		}
		
		return new TokenComponents(tk, ts);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("TokenizerChain(");
		
		for (CharFilterFactory filter: mCharFilters) {
			sb.append(filter);
			sb.append(", ");
		}
		
		sb.append(mTokenizer);
		
		for (TokenFilterFactory filter: mFilters) {
			sb.append(", ");
			sb.append(filter);
		}
		
		sb.append(')');
		
		return sb.toString();
	}

}
