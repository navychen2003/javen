package org.javenstudio.falcon.search.hits;

import org.javenstudio.common.indexdb.IDocsEnum;
import org.javenstudio.common.indexdb.ITermsEnum;
import org.javenstudio.common.indexdb.util.Bits;

public class DocsEnumState {
	
	// currently interned for as long as indexdb requires it
    private final String mFieldName; 
    
    private ITermsEnum mTermsEnum;
    private IDocsEnum mDocsEnum;
    private Bits mLiveDocs;

    private int mMinSetSizeCached;
    private int[] mScratches;
    
    public DocsEnumState(String fieldName) { 
    	mFieldName = fieldName;
    }
    
    public String getFieldName() { return mFieldName; }
    
    public void setTermsEnum(ITermsEnum e) { mTermsEnum = e; }
    public ITermsEnum getTermsEnum() { return mTermsEnum; }
    
    public void setDocsEnum(IDocsEnum e) { mDocsEnum = e; }
    public IDocsEnum getDocsEnum() { return mDocsEnum; }
    
    public void setLiveDocs(Bits docs) { mLiveDocs = docs; }
    public Bits getLiveDocs() { return mLiveDocs; }
    
    public int getMinSetSizeCached() { return mMinSetSizeCached; }
    public void setMinSetSizeCached(int size) { mMinSetSizeCached = size; }
    
    public int[] getScratches() { return mScratches; }
    public void setScratches(int[] val) { mScratches = val; }
    
}
