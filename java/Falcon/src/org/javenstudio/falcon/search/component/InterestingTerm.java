package org.javenstudio.falcon.search.component;

import java.util.Comparator;

import org.javenstudio.common.indexdb.ITerm;

public class InterestingTerm {
	
    private final ITerm mTerm;
    private final float mBoost;
        
    public InterestingTerm(ITerm term, float boost) { 
    	mTerm = term; 
    	mBoost = boost;
    }
    
    public ITerm getTerm() { return mTerm; }
    public float getBoost() { return mBoost; }
    
    public static Comparator<InterestingTerm> BOOST_ORDER = 
    		new Comparator<InterestingTerm>() {
    			@Override
		    	public int compare(InterestingTerm t1, InterestingTerm t2) {
		    		float d = t1.mBoost - t2.mBoost;
		    		if (d == 0) 
		    			return 0;
		    		
		    		return (d>0) ? 1:-1;
		    	}
		    };
    
}
