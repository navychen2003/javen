package org.javenstudio.hornet.search.query;

import java.util.Arrays;

import org.javenstudio.common.indexdb.IDocsAndPositionsEnum;
import org.javenstudio.common.indexdb.ITerm;

public class PostingsAndFreq implements Comparable<PostingsAndFreq> {
	
    private final IDocsAndPositionsEnum mPostings;
    private final int mDocFreq;
    private final int mPosition;
    private final ITerm[] mTerms;
    private final int mNumTerms; // for faster comparisons

    public PostingsAndFreq(IDocsAndPositionsEnum postings, 
    		int docFreq, int position, ITerm... terms) {
    	mPostings = postings;
    	mDocFreq = docFreq;
    	mPosition = position;
    	mNumTerms = terms==null ? 0 : terms.length;
    	
    	if (mNumTerms > 0) {
    		if (terms.length == 1) {
    			mTerms = terms;
    			
    		} else {
    			ITerm[] terms2 = new ITerm[terms.length];
    			System.arraycopy(terms, 0, terms2, 0, terms.length);
    			Arrays.sort(terms2);
    			mTerms = terms2;
    		}
    		
    	} else {
    		mTerms = null;
    	}
	}

    public IDocsAndPositionsEnum getPostings() { return mPostings; }
    public ITerm[] getTerms() { return mTerms; }
    public int getDocFreq() { return mDocFreq; }
    public int getPosition() { return mPosition; }
    
    @Override
    public int compareTo(PostingsAndFreq other) {
    	if (mDocFreq != other.mDocFreq) 
    		return mDocFreq - other.mDocFreq;
    	
    	if (mPosition != other.mPosition) 
    		return mPosition - other.mPosition;
      
    	if (mNumTerms != other.mNumTerms) 
    		return mNumTerms - other.mNumTerms;
      
    	if (mNumTerms == 0) 
    		return 0;
      
    	for (int i=0; i < mTerms.length; i++) {
    		int res = mTerms[i].compareTo(other.mTerms[i]);
    		if (res!=0) 
    			return res;
    	}
    	
    	return 0;
    }

    @Override
    public int hashCode() {
    	final int prime = 31;
    	
    	int result = 1;
    	result = prime * result + mDocFreq;
    	result = prime * result + mPosition;
    	
    	for (int i=0; i < mNumTerms; i++) {
    		result = prime * result + mTerms[i].hashCode(); 
    	}
    	
    	return result;
    }

    @Override
    public boolean equals(Object obj) {
    	if (this == obj) return true;
    	if (obj == null) return false;
    	if (getClass() != obj.getClass()) 
    		return false;
    	
    	PostingsAndFreq other = (PostingsAndFreq) obj;
    	
    	if (mDocFreq != other.mDocFreq) 
    		return false;
    	if (mPosition != other.mPosition) 
    		return false;
    	if (mTerms == null) 
    		return other.mTerms == null;
    	
    	return Arrays.equals(mTerms, other.mTerms);
    }
    
}
