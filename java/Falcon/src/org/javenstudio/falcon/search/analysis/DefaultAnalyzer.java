package org.javenstudio.falcon.search.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenComponents;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.SchemaFieldType;

/**
 * Default analyzer for types that only produce 1 verbatim token...
 * A maximum size of chars to be read must be specified
 */
public class DefaultAnalyzer extends BaseAnalyzer {
	
	private final SchemaFieldType mFieldType;
    private final int mMaxChars;

    public DefaultAnalyzer(SchemaFieldType fieldType, int maxChars) {
    	mFieldType = fieldType;
    	mMaxChars = maxChars;
    }
    
    public final int getMaxChars() { return mMaxChars; }
    
    @Override
    public TokenComponents createComponents(String fieldName, Reader reader) {
    	Tokenizer ts = new Tokenizer(reader) {
    			private final char[] mBuf = new char[mMaxChars];
    			private final CharToken mToken = new CharToken();
    			
				@Override
				public IToken nextToken() throws IOException {
					mToken.clear();
					int n = getInput().read(mBuf, 0, mMaxChars);
					if (n <= 0) 
						return null;
					
					try {
						String s = mFieldType.toInternal(new String(mBuf, 0, n));
						
						mToken.getTerm().setEmpty().append(s);
						mToken.setOffset(0, n);
						
						return mToken;
					} catch (ErrorException ex) { 
						throw new IOException(ex);
					}
				}
	    	};

    	return new TokenComponents(ts);
    }
    
}
