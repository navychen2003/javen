package org.javenstudio.common.indexdb.analysis;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.javenstudio.common.indexdb.IToken;
import org.javenstudio.common.indexdb.ITokenStream;

/**
 * This class can be used if the token attributes of a TokenStream
 * are intended to be consumed more than once. It caches
 * all token attribute states locally in a List.
 * 
 * <P>CachingTokenFilter implements the optional method
 * {@link TokenStream#reset()}, which repositions the
 * stream to the first Token. 
 */
public final class CachingTokenFilter extends TokenFilter {
	
	private List<IToken> mCache = null;
	private Iterator<IToken> mIterator = null; 
	private int mFinalOffset = 0;
  
	/**
	 * Create a new CachingTokenFilter around <code>input</code>,
	 * caching its token attributes, which can be replayed again
	 * after a call to {@link #reset()}.
	 */
	public CachingTokenFilter(ITokenStream input) {
		super(input);
	}
  
	@Override
	public final IToken nextToken() throws IOException {
		if (mCache == null) {
			// fill cache lazily
			mCache = new LinkedList<IToken>();
			fillCache();
			mIterator = mCache.iterator();
		}
    
		if (!mIterator.hasNext()) {
			// the cache is exhausted, return false
			return null;
		}
		
		// Since the TokenFilter can be reset, the tokens need to be preserved as immutable.
		return mIterator.next();
	}
  
	@Override
	public final int end() {
		return mFinalOffset;
	}

	/**
	 * Rewinds the iterator to the beginning of the cached list.
	 * <p>
	 * Note that this does not call reset() on the wrapped tokenstream ever, even
	 * the first time. You should reset() the inner tokenstream before wrapping
	 * it with CachingTokenFilter.
	 */
	@Override
	public void reset() {
		if (mCache != null) 
			mIterator = mCache.iterator();
	}
  
	private void fillCache() throws IOException {
		IToken token = null;
		while ((token = mInput.nextToken()) != null) {
			mCache.add((IToken)token.clone()); // must clone
		}
		
		// capture final state
		mFinalOffset = mInput.end();
	}

}
