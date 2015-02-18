package org.javenstudio.panda.analysis;

import java.io.IOException;
import java.util.LinkedList;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * Filter for DoubleMetaphone (supporting secondary codes)
 */
public final class DoubleMetaphoneFilter extends TokenFilter {

	@SuppressWarnings("unused")
	private static final String TOKEN_TYPE = "DoubleMetaphone";
  
	private final LinkedList<CharToken> mRemainingTokens = new LinkedList<CharToken>();
	private final DoubleMetaphone mEncoder = new DoubleMetaphone();
	private final boolean mInject;

	/** 
	 * Creates a DoubleMetaphoneFilter with the specified maximum code length, 
	 *  and either adding encoded forms as synonyms (<code>inject=true</code>) or
	 *  replacing them.
	 */
	public DoubleMetaphoneFilter(ITokenStream input, int maxCodeLength, boolean inject) {
		super(input);
		mEncoder.setMaxCodeLen(maxCodeLength);
		mInject = inject;
	}

	@Override
	public IToken nextToken() throws IOException {
		for(;;) {
			if (!mRemainingTokens.isEmpty()) {
				// clearAttributes();  // not currently necessary
				return mRemainingTokens.removeFirst();
			}

			CharToken token = (CharToken)super.nextToken();
			if (token == null) 
				return null;

			int len = token.getTerm().length();
			if (len == 0) 
				return token; // pass through zero length terms
      
			int firstAlternativeIncrement = mInject ? 0 : token.getPositionIncrement();

			String v = token.getTerm().toString();
			String primaryPhoneticValue = mEncoder.doubleMetaphone(v);
			String alternatePhoneticValue = mEncoder.doubleMetaphone(v, true);

			// a flag to lazily save state if needed... this avoids a save/restore when only
			// one token will be generated.
			boolean saveState = mInject;

			if (primaryPhoneticValue!=null && primaryPhoneticValue.length() > 0 && 
				!primaryPhoneticValue.equals(v)) {
				if (saveState) 
					mRemainingTokens.addLast(token);
				
				token.setPositionIncrement( firstAlternativeIncrement );
				firstAlternativeIncrement = 0;
				
				token.getTerm().setEmpty().append(primaryPhoneticValue);
				saveState = true;
			}

			if (alternatePhoneticValue!=null && alternatePhoneticValue.length() > 0 && 
				!alternatePhoneticValue.equals(primaryPhoneticValue) && 
				!primaryPhoneticValue.equals(v)) {
				if (saveState) {
					mRemainingTokens.addLast(token);
					saveState = false;
				}
				
				token.setPositionIncrement( firstAlternativeIncrement );
				token.getTerm().setEmpty().append(alternatePhoneticValue);
				saveState = true;
			}

			// Just one token to return, so no need to capture/restore
			// any state, simply return it.
			if (mRemainingTokens.isEmpty()) 
				return token;

			if (saveState) 
				mRemainingTokens.addLast(token);
		}
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		mRemainingTokens.clear();
	}
	
}
