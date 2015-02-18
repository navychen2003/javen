package org.javenstudio.falcon.search.query;

import java.io.IOException;
import java.util.Set;

import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Query;

/** A simple query that wraps another query and implements ExtendedQuery. */
public class WrappedQuery extends ExtendedQueryBase {
	
	private IQuery mQuery;

	public WrappedQuery(IQuery q) {
		mQuery = q;
	}

	public IQuery getWrappedQuery() {
		return mQuery;
	}

	public void setWrappedQuery(IQuery q) {
		mQuery = q;
	}

	@Override
	public void setBoost(float b) {
		mQuery.setBoost(b);
	}

	@Override
	public float getBoost() {
		return mQuery.getBoost();
	}

	@Override
	public IWeight createWeight(ISearcher searcher) throws IOException {
		return mQuery.createWeight(searcher);
	}

	@Override
	public IQuery rewrite(IIndexReader reader) throws IOException {
		// currently no need to continue wrapping at this point.
		return mQuery.rewrite(reader);
	}

	@Override
	public void extractTerms(Set<ITerm> terms) {
		mQuery.extractTerms(terms);
	}

	@Override
	public WrappedQuery clone() {
		WrappedQuery newQ = (WrappedQuery)super.clone();
		newQ.mQuery = ((Query) mQuery).clone();
		return newQ;
	}

	@Override
	public int hashCode() {
		return mQuery.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WrappedQuery) 
			return mQuery.equals(((WrappedQuery)obj).mQuery);
		
		return mQuery.equals(obj);
	}

	@Override
	public String toString(String field) {
		return getOptions() + mQuery.toString();
	}
	
}
