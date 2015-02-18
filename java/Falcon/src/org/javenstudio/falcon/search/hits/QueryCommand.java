package org.javenstudio.falcon.search.hits;

import java.util.ArrayList;
import java.util.List;

import org.javenstudio.common.indexdb.IQuery;
import org.javenstudio.common.indexdb.IScoreDoc;
import org.javenstudio.common.indexdb.ISort;
import org.javenstudio.falcon.search.Searcher;

/**
 * A query request command to avoid having to change the method signatures
 * if we want to pass additional information to the searcher.
 */
public class QueryCommand {

    private IQuery mQuery;
    private List<IQuery> mFilterList;
    private DocSet mFilter;
    private ISort mSort;
    
    private int mOffset;
    private int mLen;
    private int mSupersetMaxDoc;
    private int mFlags;
    private long mTimeAllowed = -1;
    
    private IScoreDoc mScoreDoc;
    
    // public List<Grouping.Command> groupCommands;
    
    public IScoreDoc getScoreDoc() { return mScoreDoc; }
    public void setScoreDoc(IScoreDoc scoreDoc) { mScoreDoc = scoreDoc; }

    public IQuery getQuery() { return mQuery; }
	public QueryCommand setQuery(IQuery query) {
    	mQuery = query;
    	return this;
    }
    
    public List<IQuery> getFilterList() { return mFilterList; }
    
    /**
     * @throws IllegalArgumentException if filter is not null.
     */
    public QueryCommand setFilterList(List<IQuery> filterList) {
    	if (mFilter != null) {
    		throw new IllegalArgumentException("Either filter or filterList " 
    				+ "may be set in the QueryCommand, but not both." );
    	}
    	
    	mFilterList = filterList;
    	return this;
    }
    
    /**
     * A simple setter to build a filterList from a query
     * @throws IllegalArgumentException if filter is not null.
     */
    public QueryCommand setFilterList(IQuery f) {
    	if (mFilter != null) {
    		throw new IllegalArgumentException("Either filter or filterList " 
    				+ "may be set in the QueryCommand, but not both." );
    	}
    	
    	mFilterList = null;
    	if (f != null) {
    		mFilterList = new ArrayList<IQuery>(2);
    		mFilterList.add(f);
    	}
    	
    	return this;
    }
    
    public DocSet getFilter() { return mFilter; }
    
    /**
     * @throws IllegalArgumentException if filterList is not null.
     */
    public QueryCommand setFilter(DocSet filter) {
    	if (mFilterList != null) {
    		throw new IllegalArgumentException("Either filter or filterList " 
    				+ "may be set in the QueryCommand, but not both." );
    	}
    	
      	mFilter = filter;
      	return this;
    }

    public ISort getSort() { return mSort; }
    
    public QueryCommand setSort(ISort sort) {
      	mSort = sort;
      	return this;
    }
    
    public int getOffset() { return mOffset; }
    
    public QueryCommand setOffset(int offset) {
    	mOffset = offset;
    	return this;
    }
    
    public int getLength() { return mLen; }
    
    public QueryCommand setLength(int len) {
    	mLen = len;
    	return this;
    }
    
    public int getSupersetMaxDoc() { return mSupersetMaxDoc; }
    
    public QueryCommand setSupersetMaxDoc(int supersetMaxDoc) {
    	mSupersetMaxDoc = supersetMaxDoc;
    	return this;
    }

    public int getFlags() { return mFlags; }

    public QueryCommand replaceFlags(int flags) {
      	mFlags = flags;
      	return this;
    }

    public QueryCommand setFlags(int flags) {
    	mFlags |= flags;
    	return this;
    }

    public QueryCommand clearFlags(int flags) {
    	mFlags &= ~flags;
    	return this;
    }

    public long getTimeAllowed() { return mTimeAllowed; }
    
    public QueryCommand setTimeAllowed(long timeAllowed) {
    	mTimeAllowed = timeAllowed;
    	return this;
    }
    
    public boolean isNeedDocSet() { 
    	return (mFlags & Searcher.GET_DOCSET) != 0; 
    }
    
    public QueryCommand setNeedDocSet(boolean needDocSet) {
    	return needDocSet ? setFlags(Searcher.GET_DOCSET) :
    		clearFlags(Searcher.GET_DOCSET);
    }
	
}
