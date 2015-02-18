package org.javenstudio.falcon.search.component;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.falcon.search.hits.DocList;

public class QueryData {

	private final IQuery mQuery;
	private final DocList mDocs;
	
	public QueryData(DocList docs) { 
		this(docs, null);
	}
	
	public QueryData(DocList docs, IQuery query) { 
		mQuery = query;
		mDocs = docs;
	}
	
	public final IQuery getQuery() { return mQuery; }
	public final DocList getDocList() { return mDocs; }
	
}
