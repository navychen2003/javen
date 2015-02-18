package org.javenstudio.falcon.search.transformer;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.search.Searcher;
import org.javenstudio.falcon.search.ISearchRequest;
import org.javenstudio.falcon.search.hits.DocIterator;

/**
 * Environment variables for the transformed documents
 *
 * @since 4.0
 */
public class TransformContext {
	
	private IQuery mQuery;
	private boolean mWantsScores = false;
	private DocIterator mIterator;
	private Searcher mSearcher;
	private ISearchRequest mRequest;
	
	public IQuery getQuery() { return mQuery; }
	public void setQuery(IQuery query) { mQuery = query; }
	
	public Searcher getSearcher() { return mSearcher; }
	public void setSearcher(Searcher searcher) { mSearcher = searcher; }
	
	public ISearchRequest getRequest() { return mRequest; }
	public void setRequest(ISearchRequest req) { mRequest = req; }
	
	public DocIterator getDocIterator() { return mIterator; }
	public void setDocIterator(DocIterator iter) { mIterator = iter; }
	
	public boolean wantsScores() { return mWantsScores; }
	public void setWantsScores(boolean wants) { mWantsScores = wants; }
	
}
