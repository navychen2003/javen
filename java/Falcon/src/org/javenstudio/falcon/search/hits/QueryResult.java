package org.javenstudio.falcon.search.hits;


/**
 * The result of a search.
 */
public class QueryResult {

    private boolean mPartialResults;
    private DocListAndSet mDocListAndSet;

    private Object mGroupedResults;   // TODO: currently for testing
    
    public DocList getDocList() { return mDocListAndSet.getDocList(); }
    
	public void setDocList(DocList list) {
    	if (mDocListAndSet == null) 
    		mDocListAndSet = new DocListAndSet();
    	
    	mDocListAndSet.setDocList(list);
    }

    public DocSet getDocSet() { return mDocListAndSet.getDocSet(); }
    
    public void setDocSet(DocSet set) {
    	if (mDocListAndSet == null) 
    		mDocListAndSet = new DocListAndSet();
      
    	mDocListAndSet.setDocSet(set);
    }

    public boolean isPartialResults() { return mPartialResults; }
    public void setPartialResults(boolean partialResults) { mPartialResults = partialResults; }

    public void setDocListAndSet(DocListAndSet listSet) { mDocListAndSet = listSet; }
    public DocListAndSet getDocListAndSet() { return mDocListAndSet; }
	
    public Object getGroupedResults() { return mGroupedResults; }
    public void setGroupedResults(Object results) { mGroupedResults = results; }
    
}
