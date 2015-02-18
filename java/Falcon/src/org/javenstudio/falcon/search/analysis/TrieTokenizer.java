package org.javenstudio.falcon.search.analysis;

import java.io.IOException;
import java.io.Reader;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.NumericToken;
import org.javenstudio.common.indexdb.analysis.NumericTokenStream;
import org.javenstudio.common.indexdb.analysis.Tokenizer;
import org.javenstudio.falcon.ErrorException;
import org.javenstudio.falcon.search.schema.TrieTypes;
import org.javenstudio.falcon.search.schema.type.DateFieldType;

public class TrieTokenizer extends Tokenizer {
	
	protected static final DateFieldType sDateField = new DateFieldType();
	
  	protected final int mPrecisionStep;
  	protected final TrieTypes mType;
  	protected final NumericTokenStream mTokenStream;
  
  	protected int mStartOffset, mEndOffset;

  	static NumericTokenStream getNumericTokenStream(int precisionStep) {
  		return new NumericTokenStream(precisionStep);
  	}

  	public TrieTokenizer(Reader input, TrieTypes type, int precisionStep, 
  			NumericTokenStream ts) {
  		// must share the attribute source with the NumericTokenStream we delegate to
  		super(input);
  		mType = type;
  		mPrecisionStep = precisionStep;
  		mTokenStream = ts;
  	}

  	@Override
  	public void reset() throws IOException {
		char[] buf = new char[32];
		int len = getInput().read(buf);
		
		mStartOffset = correctOffset(0);
		mEndOffset = correctOffset(len);
		
		String v = new String(buf, 0, len);
		try {
			switch (mType) {
			case INTEGER:
				mTokenStream.setIntValue(Integer.parseInt(v));
				break;
			case FLOAT:
				mTokenStream.setFloatValue(Float.parseFloat(v));
				break;
			case LONG:
				mTokenStream.setLongValue(Long.parseLong(v));
				break;
			case DOUBLE:
				mTokenStream.setDoubleValue(Double.parseDouble(v));
				break;
			case DATE:
				mTokenStream.setLongValue(sDateField.parseMath(null, v).getTime());
				break;
			default:
				throw new IOException("Unknown type for trie field");
			}
		} catch (ErrorException ex) { 
			throw new IOException(ex.toString(), ex);
			
		} catch (NumberFormatException nfe) {
			throw new IOException("Invalid Number: " + v, nfe);
		}
		
  		mTokenStream.reset();
  	}

  	@Override
  	public void close() throws IOException {
  		super.close();
  		mTokenStream.close();
  	}

  	@Override
  	public IToken nextToken() {
  		IToken token = mTokenStream.nextToken();
  		if (token != null && token instanceof NumericToken) {
  			NumericToken t = (NumericToken)token;
  			t.setOffset(mStartOffset, mEndOffset);
  			return t;
  		}
  		return token;
  	}

  	@Override
  	public int end() throws IOException {
  		mTokenStream.end();
  		return mEndOffset;
  	}
  	
}
