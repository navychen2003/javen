package org.javenstudio.hornet.search.query;

import org.javenstudio.common.indexdb.ITerm;
import org.javenstudio.common.indexdb.ITermContext;

/**
 * A rewrite method that first translates each term into
 * {@link BooleanClause.Occur#SHOULD} clause in a BooleanQuery, and keeps the
 * scores as computed by the query.
 * 
 * <p>
 * This rewrite method only uses the top scoring terms so it will not overflow
 * the boolean max clause count. It is the default rewrite method for
 * {@link FuzzyQuery}.
 * 
 * @see #setRewriteMethod
 */
public class TopTermsScoringBooleanQueryRewrite extends TopTermsRewrite<BooleanQuery> {

	/** 
     * Create a TopTermsScoringBooleanQueryRewrite for 
     * at most <code>size</code> terms.
     * <p>
     * NOTE: if {@link BooleanQuery#getMaxClauseCount} is smaller than 
     * <code>size</code>, then it will be used instead. 
     */
    public TopTermsScoringBooleanQueryRewrite(int size) {
    	super(size);
    }
    
    @Override
    protected int getMaxSize() {
    	return BooleanQuery.getMaxClauseCount();
    }
    
    @Override
    protected BooleanQuery getTopLevelQuery() {
    	return new BooleanQuery(true);
    }
    
    @Override
    protected void addClause(BooleanQuery topLevel, ITerm term, 
    		int docCount, float boost, ITermContext states) {
    	final TermQuery tq = new TermQuery(term, states);
    	tq.setBoost(boost);
    	topLevel.add(tq, BooleanClause.Occur.SHOULD);
    }
	
}
