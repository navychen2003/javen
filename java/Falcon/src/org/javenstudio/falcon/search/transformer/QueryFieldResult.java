package org.javenstudio.falcon.search.transformer;

import org.javenstudio.common.indexdb.ITopDocs;

/**
 * Encapsulates {@link TopDocs} and the number of matches.
 */
public class QueryFieldResult {
	
	private final ITopDocs mTopDocs;
	private final int mMatches;

	public QueryFieldResult(ITopDocs topDocs, int matches) {
		mTopDocs = topDocs;
		mMatches = matches;
	}

	public ITopDocs getTopDocs() {
		return mTopDocs;
	}

	public int getMatches() {
		return mMatches;
	}
	
}
