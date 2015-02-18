package org.javenstudio.falcon.search.analysis;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.analysis.TokenStream;

/**
 * TokenStream that iterates over a list of pre-existing Tokens
 * 
 */
public class ListBasedTokenStream extends TokenStream {
	
	private final List<IToken> mTokens;
    private Iterator<IToken> mTokenIterator;

    /**
     * Creates a new ListBasedTokenStream which uses the given tokens as its token source.
     *
     * @param tokens Source of tokens to be used
     */
    public ListBasedTokenStream(List<IToken> tokens) {
    	mTokens = tokens;
    	mTokenIterator = tokens.iterator();
    }

    @Override
    public IToken nextToken() {
    	if (mTokenIterator.hasNext()) {
    		IToken next = mTokenIterator.next();
    		
    		//Iterator<Class<? extends Attribute>> atts = next.getAttributeClassesIterator();
    		//while (atts.hasNext()) { // make sure all att impls in the token exist here
    		//	addAttribute(atts.next());
    		//}
    		//next.copyTo(this);
    		
    		return next;
    	}
    	
    	return null;
    }

    @Override
    public void reset() throws IOException {
    	super.reset();
    	mTokenIterator = mTokens.iterator();
    }
    
}
