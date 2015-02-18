package org.javenstudio.hornet.query;

import java.io.IOException;

import org.javenstudio.common.indexdb.IBooleanClause;
import org.javenstudio.common.indexdb.IIndexReader;
import org.javenstudio.common.indexdb.ISearcher;
import org.javenstudio.common.indexdb.IWeight;
import org.javenstudio.common.indexdb.search.Query;
import org.javenstudio.hornet.search.query.BooleanQuery;

/**
 * The BoostingQuery class can be used to effectively demote results that match a given query. 
 * Unlike the "NOT" clause, this still selects documents that contain undesirable terms, 
 * but reduces their overall score:
 *
 *  Query balancedQuery = new BoostingQuery(positiveQuery, negativeQuery, 0.01f);
 *  
 * In this scenario the positiveQuery contains the mandatory, desirable criteria which is used to 
 * select all matching documents, and the negativeQuery contains the undesirable elements which 
 * are simply used to lessen the scores. Documents that match the negativeQuery have their score 
 * multiplied by the supplied "boost" parameter, so this should be less than 1 to achieve a 
 * demoting effect
 * 
 * This code was originally made available here: 
 * [WWW] http://marc.theaimsgroup.com/?l=lucene-user&m=108058407130459&w=2
 * and is documented here: http://wiki.apache.org/lucene-java/CommunityContributions
 */
public class BoostingQuery extends Query {
	
    private final float mBoost;  		// the amount to boost by
    private final Query mMatch;    		// query to match
    private final Query mContext;    	// boost when matches too

    public BoostingQuery(Query match, Query context, float boost) {
    	mMatch = match;
    	mContext = context.clone();  	// clone before boost
    	mBoost = boost;
    	mContext.setBoost(0.0f);      	// ignore context-only matches
    }

    @Override
    public Query rewrite(IIndexReader reader) throws IOException {
    	BooleanQuery result = new BooleanQuery() {
    		@Override
    		public IWeight createWeight(ISearcher searcher) throws IOException {
    			return new BooleanQuery.BooleanWeight(searcher, false) {
    				@Override
    				public float coord(int overlap, int max) {
    					switch (overlap) {
    					case 1:                               // matched only one clause
    						return 1.0f;                        // use the score as-is
    					case 2:                               // matched both clauses
    						return mBoost;                       // multiply by boost
    					default:
    						return 0.0f;
    					}
    				}
    			};
    		}
    	};

    	result.add(mMatch, IBooleanClause.Occur.MUST);
    	result.add(mContext, IBooleanClause.Occur.SHOULD);

    	return result;
    }

    @Override
    public int hashCode() {
    	final int prime = 31;
    	int result = 1;
    	
    	result = prime * result + Float.floatToIntBits(mBoost);
    	result = prime * result + ((mContext == null) ? 0 : mContext.hashCode());
    	result = prime * result + ((mMatch == null) ? 0 : mMatch.hashCode());
    	
    	return result;
    }

    @Override
    public boolean equals(Object obj) {
    	if (this == obj) return true;
    	if (obj == null || getClass() != obj.getClass()) 
    		return false;

    	BoostingQuery other = (BoostingQuery) obj;
    	if (Float.floatToIntBits(mBoost) != Float.floatToIntBits(other.mBoost)) 
    		return false;
      
    	if (mContext == null) {
    		if (other.mContext != null) 
    			return false;
    		
    	} else if (!mContext.equals(other.mContext)) {
    		return false;
    	}
      
    	if (mMatch == null) {
    		if (other.mMatch != null) 
    			return false;
    		
    	} else if (!mMatch.equals(other.mMatch)) {
    		return false;
    	}
    	
    	return true;
    }

    @Override
    public String toString(String field) {
    	return mMatch.toString(field) + "/" + mContext.toString(field);
    }
    
}
