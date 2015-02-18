package org.javenstudio.falcon.search.filter;

import org.javenstudio.common.indexdb.IFilter;
import org.javenstudio.falcon.search.hits.DelegatingCollector;
import org.javenstudio.falcon.search.hits.DocSet;

public class ProcessedFilter {

    private DocSet mAnswer;  // the answer, if non-null
    private IFilter mFilter;
    private DelegatingCollector mPostFilter;
	
    public ProcessedFilter() {}
    
    public DocSet getAnswer() { return mAnswer; }
    public void setAnswer(DocSet set) { mAnswer = set; }
    
    public IFilter getFilter() { return mFilter; }
    public void setFilter(IFilter filter) { mFilter = filter; }
    
    public DelegatingCollector getPostFilter() { return mPostFilter; }
    public void setPostFilter(DelegatingCollector f) { mPostFilter = f; }
    
}
