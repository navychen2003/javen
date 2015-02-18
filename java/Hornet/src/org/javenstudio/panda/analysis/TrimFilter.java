package org.javenstudio.panda.analysis;

import java.io.IOException;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;
import org.javenstudio.common.indexdb.analysis.CharToken;
import org.javenstudio.common.indexdb.analysis.TokenFilter;

/**
 * Trims leading and trailing whitespace from Tokens in the stream.
 */
public final class TrimFilter extends TokenFilter {

	private final boolean mUpdateOffsets;

	public TrimFilter(ITokenStream in, boolean updateOffsets) {
		super(in);
		mUpdateOffsets = updateOffsets;
	}

	@Override
	public IToken nextToken() throws IOException {
		CharToken token = (CharToken)super.nextToken();
		if (token == null) 
			return null;

		char[] termBuffer = token.getTerm().buffer();
		int len = token.getTerm().length();
		
		//TODO: Is this the right behavior or should we return false? 
		// Currently, "  ", returns true, so I think this should
		//also return true
		if (len == 0) 
			return token;
		
		int start = 0;
		int end = 0;
		int endOff = 0;

		// eat the first characters
		//QUESTION: Should we use Character.isWhitespace() instead?
		for (start = 0; start < len && termBuffer[start] <= ' '; start++) {
		}
		
		// eat the end characters
		for (end = len; end >= start && termBuffer[end - 1] <= ' '; end--) {
			endOff ++;
		}
		
		if (start > 0 || end < len) {
			if (start < end) 
				token.getTerm().copyBuffer(termBuffer, start, (end - start));
			else 
				token.getTerm().setEmpty();
			
			if (mUpdateOffsets && len == token.getEndOffset() - token.getStartOffset()) {
				int newStart = token.getStartOffset()+start;
				int newEnd = token.getEndOffset() - (start<end ? endOff:0);
				
				token.setOffset(newStart, newEnd);
			}
		}

		return token;
	}
	
}
