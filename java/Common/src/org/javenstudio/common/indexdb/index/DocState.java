package org.javenstudio.common.indexdb.index;

import org.javenstudio.common.indexdb.IAnalyzer;
import org.javenstudio.common.indexdb.IDocument;

public class DocState {

	private IDocument mDoc = null;
	private IAnalyzer mAnalyzer = null;
	private int mDocID = 0;
	private String mMaxTermPrefix = null;
	
	public DocState() {}
	
	public IDocument getDocument() { return mDoc; }
	public IAnalyzer getAnalyzer() { return mAnalyzer; }
	public int getDocID() { return mDocID; }
	
	public String getMaxTermPrefix() { return mMaxTermPrefix; }
	public void setMaxTermPrefix(String val) { mMaxTermPrefix = val; }
	
	public void setAnalyzer(IAnalyzer analyzer) { 
		mAnalyzer = analyzer;
	}
	
	public void setDocument(int docId, IDocument doc) { 
		mDoc = doc;
		mDocID = docId;
	}
	
	public void clear() {
    	// don't hold onto doc nor analyzer, in case it is
        // largish:
        mDoc = null;
        mAnalyzer = null;
	}
	
}
